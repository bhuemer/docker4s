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
import com.typesafe.scalalogging.LazyLogging
import fs2.Stream
import io.circe.{Decoder, Json}
import org.docker4s.api.Parameter
import org.docker4s.errors.DockerApiException
import org.docker4s.transport.Client.RequestBuilder
import org.http4s.{EmptyBody, EntityEncoder, Header, Method, Request, Response, Status, Uri}
import org.http4s.circe.accumulatingJsonOf
import org.http4s.circe.jsonEncoder

import scala.language.higherKinds

class Http4sClient[F[_]](
    // @formatter:off
    private val client: org.http4s.client.Client[F],
    private val uri: Uri)(implicit F: Effect[F])
    // @formatter:on
    extends Client[F] {

  override def get(path: String): RequestBuilder[F] = builderFor(Method.GET, path)

  override def head(path: String): RequestBuilder[F] = builderFor(Method.HEAD, path)

  override def post(path: String): RequestBuilder[F] = builderFor(Method.POST, path)

  override def delete(path: String): RequestBuilder[F] = builderFor(Method.DELETE, path)

  private def builderFor(method: org.http4s.Method, path: String): RequestBuilder[F] = {
    val request = Request[F]()
      .withMethod(method)
      .withUri(uri.withPath(path))
      .withHeaders(
        Header("Host", uri.host.map(_.value).getOrElse("localhost"))
      )

    new Http4sClient.Http4sRequestBuilder[F](
      client,
      request,
      Seq.empty, {
        case status if !status.isSuccess =>
          (message, context) =>
            new DockerApiException(s"An error occurred while evaluating the request: $message [$context].")
      }
    )
  }

}

object Http4sClient {

  private type ErrorCreator = (String, String) => Throwable

  private class Http4sRequestBuilder[F[_]](
      private val client: org.http4s.client.Client[F],
      private val request: org.http4s.Request[F],
      private val parameters: Seq[Parameter[_]],
      private val statusHandler: PartialFunction[Status, ErrorCreator])(implicit F: Effect[F])
      extends RequestBuilder[F]
      with LazyLogging {

    override def withBody[T](entity: T)(implicit encoder: EntityEncoder[F, T]): RequestBuilder[F] = {
      new Http4sRequestBuilder(client, request.withEntity(entity), parameters, statusHandler)
    }

    override def withParameters(newParameters: Seq[Parameter[_]]): RequestBuilder[F] = {
      new Http4sRequestBuilder(client, request, parameters ++ newParameters, statusHandler)
    }

    override def on(status: Status): Client.StatusHandler[F] = (creator: ErrorCreator) => {
      val pf: PartialFunction[Status, ErrorCreator] = {
        case given if given == status => creator
      }

      new Http4sRequestBuilder(client, request, parameters, pf.orElse(statusHandler))
    }

    override def execute: F[Unit] = {
      client.fetch[Unit](compiled)({ response =>
        if (statusHandler.isDefinedAt(response.status)) {
          raiseError(response)
        } else {
          response.body.compile.drain
        }
      })
    }

    override def expect[A](decoder: Decoder[A]): F[A] = {
      client.fetch[A](compiled)({ response =>
        if (statusHandler.isDefinedAt(response.status)) {
          raiseError(response)
        } else {
          safeDecode(response, decoder)
        }
      })
    }

    override def stream: Stream[F, Byte] = {
      client
        .stream(compiled)
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

    private def compiled: Request[F] = {
      val requestWithBody = Parameter
        .compileBody(parameters)
        .map({ json =>
          if (request.body != EmptyBody) {
            logger.warn(s"Replacing previously configured body for request $request with $json.")
          }

          request.withEntity(json)
        })
        .getOrElse(request)

      val queryParameters = Parameter.compileQuery(parameters)
      val newUri = queryParameters.foldLeft(request.uri)({
        case (uri, (parameterName, parameterValues)) =>
          uri.withQueryParam(parameterName, parameterValues).asInstanceOf[org.http4s.Uri]
      })

      val result = requestWithBody.withUri(newUri)
      logger.debug(s"${result.method} ${result.pathInfo}?${result.queryString}")
      result
    }

    private def safeDecode[A](response: Response[F], decoder: Decoder[A]): F[A] = {
      response
        .as(F, accumulatingJsonOf(F, decoder))
        .recoverWith({
          case ex =>
            val context = s"Request: $request, Parameters: ${Parameter.toDebugString(parameters)}, Response: $response"
            F.raiseError(new DockerApiException(s"Error occurred while decoding the response. $context", ex))
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
          case ex =>
            response
              .as[String]
              .map({ body =>
                s"Unknown error message. Cannot decode the response '$body' (${response.status})."
              })
        })
        .flatMap({ errorMessage =>
          val context = s"Request: $request, Parameters: ${Parameter.toDebugString(parameters)}, Response: $response"
          F.raiseError(statusHandler(response.status)(errorMessage, context))
        })
    }

  }

}
