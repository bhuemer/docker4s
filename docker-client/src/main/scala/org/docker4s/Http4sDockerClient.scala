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
package org.docker4s

import cats.effect.ConcurrentEffect
import org.docker4s.models.Info
import org.docker4s.models.Decoders._
import org.http4s.{EntityDecoder, Header, Method, Request, Uri}
import org.http4s.client.Client

import scala.language.higherKinds

private[docker4s] class Http4sDockerClient[F[_]: ConcurrentEffect](client: Client[F], uri: Uri)
    extends DockerClient[F] {

  override def info: F[Info] = {
    implicit val infoEntityDecoder: EntityDecoder[F, Info] = org.http4s.circe.accumulatingJsonOf
    client.fetchAs[Info](
      Request[F]()
        .withMethod(Method.GET)
        .withHeaders(Header("Host", uri.host.map(_.value).getOrElse("localhost")))
        .withUri(uri.withPath("/info")))
  }

}
