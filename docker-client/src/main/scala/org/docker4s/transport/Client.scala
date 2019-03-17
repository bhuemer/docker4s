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
import org.docker4s.errors.DockerApiException
import org.http4s.{EntityDecoder, Request, Response}
import org.http4s.Status.Successful

import scala.language.higherKinds

trait Client[F[_]] {

  def evaluate(request: Request[F]): F[Unit]

  def expect[A](request: Request[F])(implicit decoder: Decoder[A]): F[A]

  def stream[A](request: Request[F])(implicit decoder: Decoder[A]): Stream[F, A]

}

object Client {

  /**
    *
    */
  def from[F[_]: Effect](client: org.http4s.client.Client[F]): Client[F] = new Http4sClient[F](client)

  private class Http4sClient[F[_]](private val client: org.http4s.client.Client[F])(implicit F: Effect[F])
      extends Client[F] {

    override def evaluate(request: Request[F]): F[Unit] = {
      client.fetch(request)({
        case Successful(response) => F.unit
        case response             => handleError(request, response)
      })
    }

    override def expect[A](request: Request[F])(implicit decoder: Decoder[A]): F[A] =
      client.fetch[A](request)({
        case Successful(response) =>
          response.as(F, accumulatingJsonOf(decoder))

        case response => handleError(request, response)
      })

    /**
      *
      */
    override def stream[A](request: Request[F])(implicit decoder: Decoder[A]): Stream[F, A] = {
      client
        .stream(request)
        .flatMap({
          case Successful(response) =>
            implicit val facade: org.typelevel.jawn.RawFacade[Json] = io.circe.jawn.CirceSupportParser.facade
            response.body.chunks.through(jawnfs2.parseJsonStream)

          case response => Stream.eval(handleError(request, response))
        })
        .evalMap(json =>
          json
            .as(decoder)
            .fold(error => F.raiseError[A](error), value => F.delay[A](value)))
    }

    /**
      * Extracts the error message from the given response and lifts it, with some additional information, into `F`.
      */
    protected def handleError[A](request: Request[F], response: Response[F]): F[A] = {
      response
        .as(F, accumulatingJsonOf(Decoder.instance({ c =>
          for {
            message <- c.downField("message").as[String].right
          } yield message
        })))
        // If the response cannot be parsed as error message JSON, continue anyway as we would otherwise lose
        // the information about the request and the response that we have anyway (status codes, URIs, ..).
        .recoverWith({
          case ex => F.delay(s"Unknown error message. Cannot decode the error response due to ${ex.getMessage}: $ex")
        })
        .flatMap({ errorMessage =>
          F.raiseError(new DockerApiException(
            s"Error occurred while evaluating request $request. The response was $response with the error message '$errorMessage'."))
        })
    }

    // Partially applied version of `accumulatingJsonOf` that doesn't require evidence for the effect type any more.
    private def accumulatingJsonOf[T](decoder: Decoder[T]): EntityDecoder[F, T] =
      org.http4s.circe.accumulatingJsonOf(F, decoder)

  }

}
