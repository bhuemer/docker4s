/*
 * Copyright (c) 2019 Bernhard Huemer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.docker4s.transport.unix

import java.util.concurrent.atomic.AtomicReference

import com.typesafe.scalalogging.LazyLogging
import io.netty.buffer.ByteBuf

import scala.annotation.tailrec
import scala.collection.immutable.Queue
import scala.concurrent.{Future, Promise}
import scala.util.{Success, Try}

/**
  * Implements an asynchronous unbounded queue of byte buffers based on Viktor Klang's asynchronous queue from:
  * https://groups.google.com/forum/#!topic/scala-user/lyoAdNs3E1o
  */
final private[unix] class ResponseStream extends LazyLogging {

  import ResponseStream._

  private val state = new AtomicReference[State](Waiting(Queue()))

  /**
    * Returns the next chunk of bytes that is available in this response stream. If an error has occurred in the
    * channel since the last call to this method, the `Future` returned will be a failed one with additional details
    * about the error. `None` will be returned, if the stream has been closed and all previously accumulated chunks
    * have been drained already.
    */
  @tailrec def nextChunk(): Future[Option[ByteBuf]] = {
    state.get() match {
      // No byte buffers are available yet -> register a promise.
      case current @ Waiting(queue) =>
        val promise = Promise[Option[ByteBuf]]()
        if (!state.compareAndSet(current, Waiting(queue.enqueue(promise)))) {
          nextChunk()
        } else {
          promise.future
        }

      // Byte buffers have been drained, we need to wait for another one -> register a promise.
      case current @ Available(queue) if queue.isEmpty =>
        val promise = Promise[Option[ByteBuf]]()
        if (!state.compareAndSet(current, Waiting(Queue(promise)))) {
          nextChunk()
        } else {
          promise.future
        }

      // A byte buffer is available, so we can immediately respond with that.
      case current @ Available(queue) =>
        val (result, remaining) = queue.dequeue
        if (!state.compareAndSet(current, Available(remaining))) {
          nextChunk()
        } else {
          Future.fromTry(result.map(Option(_)))
        }

      // The stream has been closed and we've completely drained it already as well.
      case Closed(queue) if queue.isEmpty =>
        Future.successful(Option.empty)

      // The stream has been closed, but we still need to drain it.
      case current @ Closed(queue) =>
        val (result, remaining) = queue.dequeue
        if (!state.compareAndSet(current, Closed(remaining))) {
          nextChunk()
        } else {
          Future.fromTry(result.map(Option(_)))
        }
    }
  }

  /**
    * Enqueues another chunk of bytes in this response stream.
    */
  @tailrec def enqueue(response: Try[ByteBuf]): Boolean = {
    state.get() match {
      // Nobody waiting yet, so we switch into the available state
      case current @ Waiting(queue) if queue.isEmpty =>
        if (!state.compareAndSet(current, Available(Queue(response)))) {
          enqueue(response)
        } else {
          logger.trace(s"Switching response stream to available state given the response [$response].")
          true
        }

      // Somebody was waiting already, so just complete the previous promise registered.
      case current @ Waiting(queue) =>
        val (previous, remaining) = queue.dequeue
        if (!state.compareAndSet(current, Waiting(remaining))) {
          enqueue(response)
        } else {
          response.fold(previous.failure, buffer => previous.success(Option(buffer)))
          logger.trace(
            s"Resolved previously registered promise with response [$response]. There are ${queue.size - 1} promises still waiting.")
          true
        }

      // Nobody waiting yet - we've actually accumulated responses already!
      case current @ Available(queue) =>
        if (!state.compareAndSet(current, Available(queue.enqueue(response)))) {
          enqueue(response)
        } else {
          logger.trace(s"Enqueued response [$response] to the queue [size: ${queue.size + 1}].")
          true
        }

      case Closed(_) =>
        logger.error(s"Received another chunk [$response] even though the stream has been closed already.")
        false
    }
  }

  /**
    * Drains and releases all available byte buffers.
    */
  @tailrec def release(): Unit = {
    def releaseAll(queue: Queue[Try[ByteBuf]]): Unit = {
      queue.foreach({
        case Success(buffer) => buffer.release()
        case _               =>
      })
    }

    state.get() match {
      case current @ Available(queue) =>
        if (!state.compareAndSet(current, Closed(Queue.empty))) {
          release()
        } else {
          releaseAll(queue)
          logger.trace(s"Closed the stream, and drained and released ${queue.size} messages.")
        }

      case current @ Closed(queue) =>
        if (!state.compareAndSet(current, Closed(Queue.empty))) {
          release()
        } else {
          releaseAll(queue)
          logger.trace(s"Drained and released ${queue.size} message in this already-closed stream.")
        }

      // No buffered messages to release in this case.
      case _ =>
    }
  }

  /**
    * Marks this response stream as closed.
    *
    * Closing this stream is necessary to indicate to downstream subscribers: you shouldn't expect any further chunks.
    */
  @tailrec def close(): Unit = {
    state.get() match {
      // Zero, one or more chunks have been requested. All will be notified about the end of the stream (Option.none).
      case current @ Waiting(queue) =>
        if (!state.compareAndSet(current, Closed(Queue()))) {
          close()
        } else {
          queue.foreach({ promise =>
            promise.success(None)
          })
        }

      // Some elements are available that haven't been processed yet -> make sure they can be drained later on.
      case current @ Available(queue) =>
        if (!state.compareAndSet(current, Closed(queue))) {
          close()
        }

      // If the stream is closed already, no need for further action.
      case _ =>
    }
  }

}

private[unix] object ResponseStream {

  sealed private trait State

  /**
    * Indicates that one or more calls to `nextChunk` has/have been made.
    */
  private case class Waiting(queue: Queue[Promise[Option[ByteBuf]]]) extends State

  /**
    * Indicates that we have responses available in the queue that haven't been processed yet.
    */
  private case class Available(queue: Queue[Try[ByteBuf]]) extends State

  /**
    * Indicates that the stream has finished, we've received all elements, but we might still
    * need to drain previously received ones.
    */
  private case class Closed(remaining: Queue[Try[ByteBuf]]) extends State

}
