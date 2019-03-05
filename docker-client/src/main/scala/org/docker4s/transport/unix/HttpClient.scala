package org.docker4s.transport.unix

import cats.effect.IO
import io.netty.buffer.ByteBuf
import io.netty.channel.unix.DomainSocketChannel
import io.netty.handler.codec.http._
import org.http4s.Response

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

class HttpClient {

//  def execute(request: Request[IO])(implicit F: ConcurrentEffect[IO]): IO[Response[IO]] = {
//    val nettyRequest = new DefaultStreamedHttpRequest(
//      HttpVersion.HTTP_1_1,
//      request.method match {
//        case Method.GET  => HttpMethod.GET
//        case Method.PUT  => HttpMethod.PUT
//        case Method.POST => HttpMethod.POST
//      },
//      request.uri.renderString,
//      StreamUnicastPublisher(request.body.chunks.map({ chunk =>
//        new DefaultHttpContent(Unpooled.wrappedBuffer(chunk.toArray))
//      }))
//    )
//
//    request.headers.foreach({ header =>
//      nettyRequest.headers().set(header.name.value, header.value)
//    })
//
//    ???
//  }

//
//  def request(implicit ec: ExecutionContext): Future[String] = {
//    eventLoop.connect.flatMap({ channel =>
//      val request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost/info")
//      request.headers().set(HttpHeaderNames.HOST, "localhost")
//      request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
//      request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP)
//
//      val promise = Promise[String]()
//      HttpHandler.initialiseChannel(
//        channel,
//        HttpHandler.Waiting({
//          case Left(error) =>
//            promise.failure(error)
//            HttpHandler.Done
//
//          case Right(response) =>
//            System.err.println("STATUS: " + response.status)
//            System.err.println("VERSION: " + response.protocolVersion)
//            receiving(promise, new StringBuilder)
//        })
//      )
//
//      channel.writeAndFlush(request)
//      promise.future
//    })
//  }
//
//  def receiving(promise: Promise[String], buffer: StringBuilder): HttpHandler.Receiving =
//    HttpHandler.Receiving({
//      case Left(error) =>
//        promise.failure(error)
//        HttpHandler.Done
//
//      case Right(Some(bytes)) =>
//        buffer.append(bytes.toString(CharsetUtil.UTF_8))
//        receiving(promise, buffer)
//
//      case Right(None) =>
//        promise.success(buffer.toString())
//        HttpHandler.Done
//    })

}

object HttpClient {

//  def apply(eventLoop: EventLoop)(implicit ec: ExecutionContext): Client[IO] = Client[IO] { request =>
//    Resource.liftF(
//      eventLoop.connect
//        .flatMap({ channel =>
//          val promise = Promise[Response[IO]]()
//
//          promise.future
//        })
//        .asIO
//    )
//  }

  def main(args: Array[String]): Unit = {
    val eventLoop = DomainSocketEventLoop(
      (channel: DomainSocketChannel) => {
        val p = channel.pipeline
        p.addLast(new HttpClientCodec)
        p.addLast(new HttpContentDecompressor)
        p.addLast(new ResponseHandler)
      },
    ).get

    import scala.concurrent.ExecutionContext.Implicits.global

    val result = eventLoop.connect
      .flatMap({ channel =>
        val response = ResponseHandler.initialiseChannel(channel)

        val request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost/info")
        request.headers().set(HttpHeaderNames.HOST, "localhost")
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP)

        channel.writeAndFlush(request)

        response
      })
      .map({
        case (response, stream) =>
          Response[IO]()
            .withBodyStream(streaming(stream))
      })

    val response = Await.result(result, FiniteDuration(1, "min")).as[String].unsafeRunSync()

    val json = io.circe.parser.parse(response).fold(throw _, Predef.identity)
    System.out.println("Response: " + json.spaces2)

//    println("Response: " + Await.result(client.request, FiniteDuration(1, "min")))

    Await.result(eventLoop.close, FiniteDuration(1, "min"))
  }

  private def streaming(stream: ResponseStream)(implicit ec: ExecutionContext): fs2.Stream[IO, Byte] = {
    fs2.Stream
      .repeatEval(IO.async[Option[ByteBuf]]({ cb =>
        stream.nextChunk.onComplete({
          case Success(value) => cb(Right(value))
          case Failure(cause) => cb(Left(cause))
        })
      }))
      .takeWhile(_.isDefined)
      .flatMap({
        case Some(buffer) =>
          val bytes = Array.ofDim[Byte](buffer.readableBytes())
          buffer.readBytes(bytes)
          fs2.Stream.emits(bytes)

        case None => fs2.Stream.empty
      })
  }

}
