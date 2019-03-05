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

import com.typesafe.scalalogging.LazyLogging
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.{HttpContent, HttpObject, HttpResponse, LastHttpContent}
import io.netty.util.AttributeKey

import scala.concurrent.{Future, Promise}

/**
  *
  */
private[unix] final class ResponseHandler extends SimpleChannelInboundHandler[HttpObject] with LazyLogging {

  import ResponseHandler._

  // -------------------------------------------- SimpleChannelInboundHandler methods

  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject): Unit = {
    val state = ctx.channel().attr(stateKey)

    (state.get(), msg) match {
      case (Waiting(promise), response: HttpResponse) =>
        val stream = new ResponseStream
        promise.success((response, stream))
        state.set(Receiving(stream))

      case (Receiving(stream), content: HttpContent) =>
        stream.enqueue(scala.util.Success(content.content().copy()))

        // Close the stream if we've received the last chunk, otherwise continue receiving more bytes.
        if (content.isInstanceOf[LastHttpContent]) {
          stream.close()
          state.set(Done)
        }

      case (null, _) =>
        // This really shouldn't ever happen and would be a serious bug in this client somewhere ..
        throw new IllegalStateException(
          s"The state for this channel hasn't ben initialised correctly. See `ResponseHandler.initialiseChannel()`.")

      case _ =>
        throw new IllegalStateException(
          s"Received the message [$msg, class: ${msg.getClass.getName}] in a state [${state.get()}] where we didn't expect it.")
    }

    // Close the channel once the request/response has finished processing. It'd be more efficient to reuse the
    // channel instead of creating one for every request, but we'll leave that for the future, if it really
    // turns out to be a performance problem.
    if (state.get() == Done) {
      ctx.close()
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    val state = ctx.channel().attr(stateKey)
    state.get() match {
      case Waiting(promise) =>
        promise.failure(cause)

      case Receiving(stream) =>
        stream.enqueue(scala.util.Failure(cause))

      case _ =>
        logger.error(s"An error occurred after the response had been handled already: ${cause.getMessage}", cause)
    }

    state.set(Done)
    ctx.close()
  }

}

private[unix] object ResponseHandler {

  private val stateKey: AttributeKey[ResponseHandler.State] =
    AttributeKey.valueOf[ResponseHandler.State]("docker4s.http-handler-state")

  /**
    * Initialises the given channel for HTTP handlers, i.e. makes sure that the initial state is set correctly.
    */
  def initialiseChannel(channel: Channel): Future[(HttpResponse, ResponseStream)] = {
    val stateAttribute = channel.attr(stateKey)

    val promise = Promise[(HttpResponse, ResponseStream)]()

    var initialised = stateAttribute.compareAndSet(null, Waiting(promise))
    if (!initialised) {
      initialised = stateAttribute.compareAndSet(Done, Waiting(promise))
    }

    if (!initialised) {
      Future.failed(
        new IllegalStateException(
          s"Channel [$channel] has been initialised already and is not " +
            s"currently in a state that we can reset: ${stateAttribute.get()}"))
    } else {
      promise.future
    }
  }

  private sealed trait State

  /**
    * Indicates that we're still waiting for the response to come back with HTTP status, headers, etc. Once we've
    * received that information, the promise will be set with the response information and a stream that will return
    * body chunks once they're ready.
    */
  private case class Waiting(promise: Promise[(HttpResponse, ResponseStream)]) extends State

  /**
    * Indicates that we're still receiving the response body. All individual chunks will be enqueued in this stream.
    */
  private case class Receiving(stream: ResponseStream) extends State

  /**
    * Indicates that we've finished processing the response and can close the channel and all resources related to it.
    */
  private case object Done extends State

}
