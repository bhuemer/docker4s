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

import java.net.SocketAddress
import java.util.concurrent.CancellationException

import io.netty.bootstrap.Bootstrap
import io.netty.channel.epoll.{EpollDomainSocketChannel, EpollEventLoopGroup}
import io.netty.channel.kqueue.{KQueueDomainSocketChannel, KQueueEventLoopGroup}
import io.netty.channel.unix.DomainSocketChannel
import io.netty.channel.{Channel, ChannelInitializer}

import scala.concurrent.{Future, Promise}

/**
  *
  */
trait EventLoop {

  /**
    * Creates a new connection / channel to the Docker UNIX domain socket.
    * @return a future that will complete once the connection has been established
    */
  def connect: Future[Channel]

  /**
    *
    */
  def close: Future[Unit]

}

object EventLoop {

  def apply(initializer: ChannelInitializer[DomainSocketChannel], remoteAddress: SocketAddress): EventLoop = {
    val bootstrap = new Bootstrap()

    if (isEpollAvailable) {
      bootstrap
        .group(new EpollEventLoopGroup())
        .channel(classOf[EpollDomainSocketChannel])
        .handler(initializer)
    } else if (isKQueueAvailable) {
      bootstrap
        .group(new KQueueEventLoopGroup())
        .channel(classOf[KQueueDomainSocketChannel])
        .handler(initializer)
    } else {
      throw new IllegalStateException(
        "Cannot construct an event loop, because neither epoll nor kqueue is available."
      )
    }

    new EventLoop {
      override def connect: Future[Channel] = {
        val promise = Promise[Channel]()

        val future = bootstrap.connect(remoteAddress)
        future.addListener((_: io.netty.util.concurrent.Future[_ >: Void]) => {
          if (future.isSuccess) {
            promise.success(future.channel())
          } else if (future.isCancelled) {
            promise.failure(new CancellationException())
          } else {
            promise.failure(future.cause())
          }
        })

        promise.future
      }

      override def close: Future[Unit] = {
        val promise = Promise[Unit]()

        val future = bootstrap.config().group().shutdownGracefully()
        future.addListener((_: io.netty.util.concurrent.Future[Void]) => {
          if (future.isSuccess) {
            promise.success(())
          } else if (future.isCancelled) {
            promise.failure(new CancellationException())
          } else {
            promise.failure(future.cause())
          }
        })

        promise.future
      }
    }
  }

  /**
    *
    */
  private lazy val isKQueueAvailable: Boolean = {
    try {
      Class.forName("io.netty.channel.kqueue.KQueue")
      io.netty.channel.kqueue.KQueue.isAvailable
    } catch {
      case _: ClassNotFoundException =>
        false
    }
  }

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
