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
import io.netty.bootstrap.Bootstrap
import io.netty.channel.epoll.{EpollDomainSocketChannel, EpollEventLoopGroup}
import io.netty.channel.kqueue.{KQueueDomainSocketChannel, KQueueEventLoopGroup}
import io.netty.channel.unix.{DomainSocketAddress, DomainSocketChannel}
import io.netty.channel.{Channel, ChannelInitializer}

import scala.concurrent.{CancellationException, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * Light-weight wrapper around a Netty event loop that will always connect channels to the given UNIX socket.
  */
final private[unix] class DomainSocketEventLoop(
    private val bootstrap: Bootstrap,
    private val address: DomainSocketAddress) {

  /**
    * Creates a new connection / channel to the Docker UNIX domain socket.
    * @return a future that will complete once the connection has been established
    */
  def connect(): Future[Channel] = {
    val future = bootstrap.connect(address)
    onComplete(future, result = future.channel())
  }

  /**
    * Signals this event loop that the caller wants this executor to shut down.
    *
    * Returns a future that will be notified when the closing operation has completed.
    */
  def close(): Future[Unit] = {
    val future = bootstrap.config().group().shutdownGracefully()
    onComplete(future, result = ())
  }

  /**
    * Converts the given Netty future into a Scala one that will resolve to the given value.
    */
  private def onComplete[T](future: io.netty.util.concurrent.Future[_], result: => T): Future[T] = {
    val promise = Promise[T]()
    future.addListener((_: io.netty.util.concurrent.Future[_]) => {
      if (future.isSuccess) {
        promise.success(result)
      } else if (future.isCancelled) {
        promise.failure(new CancellationException)
      } else {
        promise.failure(future.cause())
      }
    })
    promise.future
  }

}

object DomainSocketEventLoop extends LazyLogging {

  // Netty will use its own default number (available processors * 2) if this value is passed in.
  private val DEFAULT_EVENT_LOOP_THREADS = 0

  /**
    * Creates a new domain-socket-based event loop that will connect to the given socket path for every request.
    *
    * This method will "guess" the correct implementation based on what is available (epoll or kqueue). If it doesn't
    * succeed in creating an event loop, a failure will be returned instead with more information.
    *
    * @param initializer initializes the pipeline of any new channel the event loop is creating
    * @param address the path to Docker's UNIX socket file, usually `/var/run/docker.sock`
    */
  def apply(
      initializer: ChannelInitializer[DomainSocketChannel],
      address: DomainSocketAddress): Try[DomainSocketEventLoop] = {
    val threadFactory = org.http4s.util.threads.threadFactory(name = { i =>
      s"docker4s-netty-client-worker-$i"
    })

    if (isEpollAvailable) {
      logger.debug(s"Creating a new UNIX domain socket event loop using `epoll` [address: $address].")

      // @formatter:off
      Success(new DomainSocketEventLoop(
        new Bootstrap()
          .group(new EpollEventLoopGroup(DEFAULT_EVENT_LOOP_THREADS, threadFactory))
          .channel(classOf[EpollDomainSocketChannel])
          .handler(initializer)
        , address))
      // @formatter:on
    } else if (isKQueueAvailable) {
      logger.debug(s"Creating a new UNIX domain socket event loop using `kqueue` [address: $address].")

      // @formatter:off
      Success(new DomainSocketEventLoop(
        new Bootstrap()
          .group(new KQueueEventLoopGroup(DEFAULT_EVENT_LOOP_THREADS, threadFactory))
          .channel(classOf[KQueueDomainSocketChannel])
          .handler(initializer)
        , address))
      // @formatter:on
    } else {
      Failure(
        new IllegalStateException(
          "Cannot construct an event loop. Neither `epoll` nor `kqueue` is available. Are you using an unsupported " +
            "operating system (e.g. Windows), or are you missing some `io.netty` dependencies on your classpath?"
        ))
    }
  }

  // won't throw, even if the JAR or JNI files are not available on the classpath
  private lazy val isKQueueAvailable: Boolean = {
    try {
      Class.forName("io.netty.channel.kqueue.KQueue")
      io.netty.channel.kqueue.KQueue.isAvailable
    } catch {
      case _: ClassNotFoundException =>
        false
    }
  }

  // also won't throw, regardless of what's on the classpath
  private lazy val isEpollAvailable: Boolean = {
    try {
      Class.forName("io.netty.channel.epoll.Epoll")
      io.netty.channel.epoll.Epoll.isAvailable
    } catch {
      case _: ClassNotFoundException =>
        false
    }
  }

}
