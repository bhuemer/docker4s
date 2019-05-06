/*
 * Copyright (c) 2019 Bernhard Huemer (bernhard.huemer@gmail.com)
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
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import cats.effect.ConcurrentEffect
import org.docker4s.transport.Client

import scala.language.higherKinds

class AkkaClient[F[_]](uri: Uri, transport: ClientTransport)(
    implicit F: ConcurrentEffect[F],
    system: ActorSystem,
    materializer: ActorMaterializer)
    extends Client[F] {

  override def get(path: String): Client.RequestBuilder[F] = builderFor(HttpMethods.GET, path)
  override def head(path: String): Client.RequestBuilder[F] = builderFor(HttpMethods.HEAD, path)
  override def post(path: String): Client.RequestBuilder[F] = builderFor(HttpMethods.POST, path)
  override def put(path: String): Client.RequestBuilder[F] = builderFor(HttpMethods.PUT, path)
  override def delete(path: String): Client.RequestBuilder[F] = builderFor(HttpMethods.DELETE, path)

  private def builderFor(method: HttpMethod, path: String): Client.RequestBuilder[F] = {
    new AkkaRequestBuilder(HttpRequest()
                             .withMethod(method)
                             .withUri(Uri.parseAndResolve(path, uri)),
                           transport,
                           Seq.empty)
  }

}
