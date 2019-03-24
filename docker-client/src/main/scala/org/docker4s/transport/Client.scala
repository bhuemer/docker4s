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
package org.docker4s.transport

import cats.effect.Effect
import cats.syntax.all._
import fs2.Stream
import io.circe.{Decoder, Json}
import org.docker4s.Criterion
import org.docker4s.errors.DockerApiException
import org.http4s.{EntityEncoder, Header, Method, QueryParamEncoder, QueryParamKeyLike, Request, Response, Status, Uri}
import org.http4s.circe.accumulatingJsonOf

import scala.language.higherKinds

trait Client[F[_]] {

  def get(path: String): Client.RequestBuilder[F]

  def post(path: String): Client.RequestBuilder[F]

  def delete(path: String): Client.RequestBuilder[F]

}

object Client {

  trait RequestBuilder[F[_]] {

    def body[T](entity: T)(implicit encoder: EntityEncoder[F, T]): Client.RequestBuilder[F]

    def queryParam[T: QueryParamEncoder, K: QueryParamKeyLike](key: K, value: T): Client.RequestBuilder[F]

    /**
      */
    def criteria(criteria: Seq[Criterion[_]]): Client.RequestBuilder[F]

    def handleStatusWith(pf: PartialFunction[Status, (String, String) => Throwable]): Client.RequestBuilder[F]

    def execute: F[Unit]

    def expect[A](decoder: Decoder[A]): F[A]

    def expectMany[A](decoder: Decoder[A]): F[List[A]] = expect(Decoder.decodeList(decoder))

    def stream: Stream[F, Byte]

    def stream[A](decoder: Decoder[A]): Stream[F, A]

  }

  /**
    *
    */
  def from[F[_]: Effect](client: org.http4s.client.Client[F], uri: Uri): Client[F] = new Http4sClient[F](client, uri)

  private class Http4sClient[F[_]](private val client: org.http4s.client.Client[F], private val uri: Uri)(
      implicit F: Effect[F])
      extends Client[F] {

    override def get(path: String): RequestBuilder[F] = builderFor(Method.GET, path)

    override def post(path: String): RequestBuilder[F] = builderFor(Method.POST, path)

    override def delete(path: String): RequestBuilder[F] = builderFor(Method.DELETE, path)

    private def builderFor(method: org.http4s.Method, path: String): RequestBuilder[F] = {
      val request = Request[F]()
        .withMethod(method)
        .withUri(uri.withPath(path))
        .withHeaders(
          Header("Host", uri.host.map(_.value).getOrElse("localhost"))
        )

      new Http4SClientRequestBuilder[F](
        client,
        request, {
          case status if !status.isSuccess =>
            (message, context) =>
              new DockerApiException(s"An error occurred while evaluating the request: $message [$context].")
        }
      )
    }

  }

  private class Http4SClientRequestBuilder[F[_]](
      private val client: org.http4s.client.Client[F],
      private val request: org.http4s.Request[F],
      private val statusHandler: PartialFunction[Status, (String, String) => Throwable])(implicit F: Effect[F])
      extends Client.RequestBuilder[F] {

    override def body[T](entity: T)(implicit encoder: EntityEncoder[F, T]): RequestBuilder[F] =
      new Http4SClientRequestBuilder(client, request.withEntity(entity), statusHandler)

    override def queryParam[T: QueryParamEncoder, K: QueryParamKeyLike](key: K, value: T): RequestBuilder[F] = {
      new Http4SClientRequestBuilder(
        client,
        request.withUri(uri = request.uri.withQueryParam(key, value).asInstanceOf[org.http4s.Uri]),
        statusHandler)
    }

    override def criteria(criteria: Seq[Criterion[_]]): RequestBuilder[F] = {
      val parameters = Criterion.compile(criteria)
      val newUri = parameters.foldLeft(request.uri)({
        case (uri, (parameterName, parameterValues)) =>
          uri.withQueryParam(parameterName, parameterValues).asInstanceOf[org.http4s.Uri]
      })

      new Http4SClientRequestBuilder(client, request.withUri(uri = newUri), statusHandler)
    }

    override def handleStatusWith(
        pf: PartialFunction[Status, (String, String) => Throwable]): Client.RequestBuilder[F] =
      new Http4SClientRequestBuilder(client, request, pf.orElse(statusHandler))

    override def execute: F[Unit] = {
      client.fetch[Unit](request)({ response =>
        if (statusHandler.isDefinedAt(response.status)) {
          raiseError(response)
        } else {
          response.body.compile.drain
        }
      })
    }

    override def expect[A](decoder: Decoder[A]): F[A] = {
      client.fetch[A](request)({ response =>
        if (statusHandler.isDefinedAt(response.status)) {
          raiseError(response)
        } else {
          response.as(F, accumulatingJsonOf(F, decoder))
        }
      })
    }

    override def stream: Stream[F, Byte] = {
      client
        .stream(request)
        .flatMap({ response =>
          if (statusHandler.isDefinedAt(response.status)) {
            Stream.eval(raiseError(response))
          } else {
            response.body
          }
        })
    }

    override def stream[A](decoder: Decoder[A]): Stream[F, A] = {
      stream.through({ body =>
        implicit val facade: org.typelevel.jawn.RawFacade[Json] = io.circe.jawn.CirceSupportParser.facade
        body.chunks
          .through(jawnfs2.parseJsonStream)
          .evalMap({ json =>
            json.as(decoder).fold(error => F.raiseError[A](error), value => F.delay[A](value))
          })
      })
    }

    /**
      * Extracts the error message from the given response and lifts it, with some additional information, into `F`.
      */
    private def raiseError[A](response: Response[F]): F[A] = {
      response
        .as(F, accumulatingJsonOf(F, Decoder.instance({ c =>
          c.downField("message").as[String]
        })))
        // If the response cannot be parsed as error message JSON, continue anyway as we would otherwise lose
        // the information about the request and the response that we have anyway (status codes, URIs, ..).
        .recoverWith({
          case ex => F.delay(s"Unknown error message. Cannot decode the error response due to ${ex.getMessage}: $ex")
        })
        .flatMap({ errorMessage =>
          val context = s"Request: $request, Response: $response"
          F.raiseError(statusHandler(response.status)(errorMessage, context))
        })
    }

  }

}
