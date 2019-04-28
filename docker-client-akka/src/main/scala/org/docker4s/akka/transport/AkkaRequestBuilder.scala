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
package org.docker4s.akka.transport

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.model.{ContentTypes, HttpRequest}
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{JsonFraming, Sink, Source}
import akka.util.ByteString
import cats.effect.ConcurrentEffect
import fs2.{Chunk, Stream}
import fs2.interop.reactivestreams.{StreamSubscriber, StreamUnicastPublisher}
import io.circe.{Decoder, Json}
import org.docker4s.errors.DockerApiException
import org.docker4s.transport.{Client, Parameter}

import scala.language.higherKinds
import scala.util.{Failure, Success}

class AkkaRequestBuilder[F[_]](request: HttpRequest, transport: ClientTransport, parameters: Seq[Parameter[_]])(
    implicit F: ConcurrentEffect[F],
    system: ActorSystem,
    materializer: ActorMaterializer)
    extends Client.RequestBuilder[F] {

  import system.dispatcher

  override def withBody(json: Json): Client.RequestBuilder[F] =
    new AkkaRequestBuilder(request.withEntity(ContentTypes.`application/json`, json.noSpaces), transport, parameters)

  override def withBody(entity: Stream[F, Byte]): Client.RequestBuilder[F] = {
    val source = Source.fromPublisher(
      StreamUnicastPublisher(
        entity.chunks.map(chunk => ByteString(chunk.toArray))
      )
    )

    new AkkaRequestBuilder(request.withEntity(Chunked.fromData(ContentTypes.NoContentType, source)),
                           transport,
                           parameters)
  }

  override def withParameters(newParameters: Seq[Parameter[_]]): Client.RequestBuilder[F] = {
    new AkkaRequestBuilder(request, transport, parameters ++ newParameters)
  }

  override def on(status: Int): Client.StatusHandler[F] = _ => this

  override def execute: F[Unit] = {
    F.async({ cb =>
      transport
        .run(compiled)
        .map(_ => ())
        .onComplete({ result =>
          cb(result.toEither)
        })
    })
  }

  override def expect[A](decoder: Decoder[A]): F[A] = {
    F.async({ cb =>
      transport
        .run(compiled)
        .flatMap({ response =>
          Unmarshal(response).to[String]
        })
        .onComplete({
          case Success(response) =>
            io.circe.jawn
              .decodeAccumulating[A](response)(decoder)
              .fold(errors => {
                cb(Left(new DockerApiException(s"Could not decode from $response: $errors")))
              }, decoded => cb(Right(decoded)))

          case Failure(error) => cb(Left(error))
        })
    })
  }

  override def stream: Stream[F, Byte] = {
    Stream
      .eval(StreamSubscriber[F, ByteString])
      .evalMap({ subscriber =>
        F.async[Stream[F, ByteString]]({ cb =>
          transport
            .run(compiled)
            .map({ response =>
              response.entity.dataBytes
                .runWith(Sink.fromSubscriber(subscriber))
              subscriber.stream
            })
            .onComplete(result => cb(result.toEither))
        })
      })
      .flatMap(Predef.identity)
      .flatMap({ bs =>
        Stream.chunk(Chunk.array(bs.toArray))
      })
  }

  override def stream[A](decoder: Decoder[A]): Stream[F, A] = {
    Stream
      .eval(StreamSubscriber[F, ByteString])
      .evalMap({ subscriber =>
        F.async[Stream[F, ByteString]]({ cb =>
          transport
            .run(compiled)
            .map({ response =>
              response.entity.dataBytes
                .via(JsonFraming.objectScanner(16 * 1024))
                .runWith(Sink.fromSubscriber(subscriber))
              subscriber.stream
            })
            .onComplete(result => cb(result.toEither))
        })
      })
      .flatMap(Predef.identity)
      .evalMap({ bs =>
        io.circe.jawn
          .decodeByteBufferAccumulating(bs.toByteBuffer)(decoder)
          .fold[F[A]](errors => {
            F.raiseError(new DockerApiException(s"Could not decode from ${new String(bs.toArray)}: $errors"))
          }, decoded => F.delay(decoded))
      })
  }

  override def header[A](name: String, decoder: String => Either[Throwable, A]): F[A] = {
    F.async({ cb =>
      transport
        .run(compiled)
        .onComplete({
          case Success(response) =>
            response.headers.find(_.is(name.toLowerCase)) match {
              case Some(header) => cb(decoder(header.value()))
              case None =>
                cb(Left(new DockerApiException(s"Cannot find the header '$name' in the response $response.")))
            }

          case Failure(error) => cb(Left(error))
        })
    })
  }

  private def compiled: HttpRequest = {
    val requestWithBody = Parameter
      .compileBody(parameters)
      .map({ json =>
        request.withEntity(ContentTypes.`application/json`, json.noSpaces)
      })
      .getOrElse(request)

    val query = Parameter.compileQuery(parameters)

    val builder = Query.newBuilder
    query.foreach({
      case (key, values) =>
        values.foreach({ value =>
          builder += key -> value
        })
    })

    requestWithBody.withUri(requestWithBody.uri.withQuery(builder.result()))
  }

}
