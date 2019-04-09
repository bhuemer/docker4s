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

import java.nio.file.Path

import cats.effect.{Effect, IO, Resource}
import cats.syntax.all._
import com.typesafe.scalalogging.LazyLogging
import fs2.Stream
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.unix.{DomainSocketAddress, DomainSocketChannel}
import io.netty.handler.codec.http.{HttpContent, HttpRequest, HttpResponse}
import org.http4s.client.Client
import org.http4s.{Request, Response}

import scala.concurrent.{CancellationException, ExecutionContext, Future}
import scala.language.higherKinds

final private[unix] class DomainSocketClient[F[_]](private val eventLoop: DomainSocketEventLoop)(
    implicit F: Effect[F],
    ec: ExecutionContext)
    extends LazyLogging {

  /**
    * Executes the given http4s request using domain sockets, under the effect type `F`.
    */
  def run(request: Request[F]): F[Response[F]] = {
    for {
      channel <- async { eventLoop.connect() }

      responseAndStream <- async {
        val future = ResponseHandler.initialiseChannel(channel)

        channel.writeAndFlush(asNettyRequest(request))

        // Start enqueuing the write operations for all the various chunks of the request body.
        F.runAsync(
            asNettyRequestStream(request)
              .evalTap({ content =>
                channel.writeAndFlush(content)
                F.unit
              })
              .compile
              .drain)(_ => IO.unit)
          .unsafeRunSync()

        future
      }

      // @formatter:off
      response <- asHttp4sResponse(responseAndStream._1).recoverWith({ case ex =>
        F.raiseError(new IllegalStateException(
          s"Error occurred while parsing the response [${responseAndStream._1}] to the request [$request].", ex))
      })
      // @formatter:on
    } yield {
      // Return the fully formed response, but don't wait for the response stream. That will keep
      // processing, but this also allows the caller to consume the streaming response already. If
      // the caller stops consuming the streaming response, the channel will be automatically closed.
      response.withBodyStream(bracket(channel).flatMap(_ => asHttp4sResponseStream(responseAndStream._2)))
    }
  }

  // -------------------------------------------- Utility methods

  private def asNettyRequest(request: Request[F]): HttpRequest = {
    import io.netty.handler.codec.http.{DefaultHttpRequest, HttpMethod, HttpVersion}

    // @formatter:off
    val result = new DefaultHttpRequest(
      HttpVersion.valueOf(request.httpVersion.renderString),
      HttpMethod.valueOf(request.method.name),
      request.uri.renderString)
    // @formatter:on

    request.headers.foreach({ header =>
      result.headers().set(header.name.value, header.value)
    })

    result
  }

  private def asNettyRequestStream(request: Request[F]): Stream[F, HttpContent] = {
    import io.netty.handler.codec.http.{DefaultHttpContent, LastHttpContent}

    request.body.chunks.map({ chunk =>
      new DefaultHttpContent(Unpooled.wrappedBuffer(chunk.toByteBuffer))
    }) ++ Stream.emit(LastHttpContent.EMPTY_LAST_CONTENT)
  }

  private def asHttp4sResponse(response: HttpResponse): F[Response[F]] = {
    import org.http4s.{Header, Headers, HttpVersion, Status}
    import scala.collection.JavaConverters._

    for {
      version <- F.fromEither(HttpVersion.fromString(response.protocolVersion.text()))

      status <- F.fromEither(Status.fromInt(response.status().code()))

      headers <- F.delay({
        Headers(
          response
            .headers()
            .asScala
            .map({ header =>
              Header(header.getKey, header.getValue)
            })
            .toList)
      })
    } yield {
      Response[F]()
        .withHttpVersion(version)
        .withStatus(status)
        .withHeaders(headers)
    }
  }

  private def asHttp4sResponseStream(stream: ResponseStream): Stream[F, Byte] =
    Stream
      .bracket(F.delay(stream))(stream => F.delay(stream.release()))
      .flatMap({ stream =>
        Stream
          .repeatEval(async { stream.nextChunk() })
          .takeWhile(_.isDefined)
          .flatMap({
            case Some(chunk) =>
              Stream.emits({
                val bytes = Array.ofDim[Byte](chunk.readableBytes())
                chunk.readBytes(bytes)
                chunk.release()
                bytes
              })

            case None => Stream.empty
          })
      })

  /**
    * Wraps the given channel in a stream such that when this stream is consumed/terminated, the channel will be
    * closed at the end of it. In the case of "normal" HTTP responses with just a single JSON object coming back,
    * this isn't necessary as the response handler will know when to close the channel. However, some responses
    * can be "infinite" (e.g. monitoring system events) where the channel needs to be closed once the subscriber
    * isn't processing anything any more.
    */
  private def bracket(channel: Channel): Stream[F, Channel] = {
    Stream.bracket(F.delay(channel))({ channel =>
      if (channel.isOpen) {
        logger.info(s"Closing the channel $channel, because the response body is not consumed any more.")
        F.async({ cb =>
          channel
            .close()
            .addListener({ future: io.netty.util.concurrent.Future[_ >: Void] =>
              if (future.isSuccess) {
                cb(Right(()))
              } else if (future.isCancelled) {
                cb(Left(new CancellationException()))
              } else {
                cb(Left(future.cause()))
              }
            })
        })
      } else {
        // Channel is already closed, no further action needed.
        F.unit
      }
    })
  }

  /**
    * Asynchronously processes/consumes the given future under the effect type `F`.
    */
  private def async[T](f: => Future[T]): F[T] =
    F.async({ cb =>
      f.onComplete(result => cb(result.toEither))
    })

}

object DomainSocketClient extends LazyLogging {

  /**
    * Creates a new http4s client that will use the given UNIX socket path for communication.
    *
    * Note that the execution context will never be used for blocking calls (the client's backend is implemented
    * in a non-blocking way), but it allows you to off-load response processing from Netty's event loop thread.
    */
  def apply[F[_]](socketPath: Path)(implicit F: Effect[F], ec: ExecutionContext): Resource[F, Client[F]] = {
    val eventLoopResource = Resource.make(
      F.fromTry(DomainSocketEventLoop(
        (channel: DomainSocketChannel) => {
          val p = channel.pipeline
          p.addLast(new io.netty.handler.codec.http.HttpClientCodec)
          p.addLast(new io.netty.handler.codec.http.HttpContentDecompressor)
          p.addLast(new ResponseHandler)
        },
        new DomainSocketAddress(socketPath.toFile.getAbsolutePath)
      )))({ eventLoop =>
      F.async({ cb =>
        logger.trace("Closing the domain socket event loop.")
        eventLoop
          .close()
          .onComplete({ result =>
            logger.debug("Closed the domain socket event loop.")
            cb(result.toEither)
          })
      })
    })

    eventLoopResource.map({ eventLoop =>
      val client = new DomainSocketClient[F](eventLoop)
      Client[F] { request =>
        Resource.liftF(client.run(request))
      }
    })
  }

}
