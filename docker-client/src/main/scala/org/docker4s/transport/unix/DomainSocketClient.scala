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

import cats.effect.Effect.ops.toAllEffectOps
import cats.effect.{ConcurrentEffect, IO, Resource}
import cats.syntax.all._
import com.typesafe.scalalogging.LazyLogging
import fs2.Stream
import io.netty.buffer.Unpooled
import io.netty.channel.unix.DomainSocketChannel
import io.netty.handler.codec.http._
import org.http4s.client.Client
import org.http4s.{Request, Response}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

private[unix] final class DomainSocketClient[F[_]](private val eventLoop: DomainSocketEventLoop)(
    implicit F: ConcurrentEffect[F],
    ec: ExecutionContext)
    extends LazyLogging {

  /**
    * Executes the given http4s request using domain sockets, under the effect type `F`.
    */
  def run(request: Request[F]): F[Response[F]] = {
    for {
      channel <- async({ eventLoop.connect() })

      responseAndStream <- async({
        val future = ResponseHandler.initialiseChannel(channel)

        channel.writeAndFlush(asNettyRequest(request))

        // Start enqueuing the write operations for all the various chunks of the request body.
        asNettyRequestStream(request)
          .evalTap({ content =>
            channel.writeAndFlush(content)
            F.unit
          })
          .compile
          .drain
          .runAsync(_ => IO.unit)
          .unsafeRunSync()

        future
      })

      response <- asHttp4sResponse(responseAndStream._1)

      // Note that the channel will be closed by the `ResponseHandler` - we don't need to worry about it here.
    } yield {
      response.withBodyStream(asHttp4sResponseStream(responseAndStream._2))
    }
  }

  /**
    * Converts the information that is immediately available in the given http4s request into a Netty one, i.e.
    * it converts everything apart from a potential request body. That body needs to be processed separately.
    */
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
    import io.netty.handler.codec.http.DefaultHttpContent
    import io.netty.handler.codec.http.LastHttpContent

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
      .repeatEval(async({ stream.nextChunk() }))
      .takeWhile(_.isDefined)
      .flatMap({
        case Some(chunk) =>
          // Depending on the backing of the byte buffer: extract the underlying bytes. The response handler
          // has already created a defensive copy of the byte buffer, so we don't need to worry about Netty
          // re-using it and the bytes possibly having been corrupted at this stage.
          val bytes = if (chunk.hasArray) {
            chunk.array()
          } else {
            val temp = Array.ofDim[Byte](chunk.readableBytes())
            chunk.readBytes(temp)
            temp
          }

          Stream.emits(bytes)

        case None => Stream.empty
      })

  private def async[T](f: => Future[T]): F[T] =
    F.async({ cb =>
      f.onComplete({
        case Success(value) => cb(Right(value))
        case Failure(error) => cb(Left(error))
      })
    })

}

object DomainSocketClient {

  def apply[F[_]: ConcurrentEffect]()(implicit ec: ExecutionContext): Client[F] = {
    val client = new DomainSocketClient[F](
      DomainSocketEventLoop(
        (channel: DomainSocketChannel) => {
          val p = channel.pipeline
          p.addLast(new HttpClientCodec)
          p.addLast(new HttpContentDecompressor)
          p.addLast(new ResponseHandler)
        },
      ).get)

    Client { request =>
      Resource.liftF(client.run(request))
    }
  }

//  def main(args: Array[String]): Unit = {
//    import cats.effect.{ContextShift, Timer}
//    import scala.concurrent.ExecutionContext.global
//
//    implicit val cs: ContextShift[IO] = IO.contextShift(global)
//    implicit val timer: Timer[IO] = IO.timer(global)
//    val cf: ConcurrentEffect[IO] = implicitly[ConcurrentEffect[IO]]
//
//    val client = DomainSocketClient()(cf, global)
//
//    val response = client
//      .fetchAs[String](
//        Request[IO]()
//          .withMethod(Method.GET)
//          .withHeaders(Header("Host", "localhost"))
//          .withUri(Uri.unsafeFromString("http://localhost/info")))
//      .unsafeRunSync()
//    println(response)
//  }

}
