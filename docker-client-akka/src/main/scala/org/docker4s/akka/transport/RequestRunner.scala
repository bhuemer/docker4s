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

import java.nio.file.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import org.docker4s.akka.transport.unix.UnixTransport
import org.docker4s.errors.DockerApiException

import scala.concurrent.Future
import scala.language.higherKinds

/**
  * Lightweight wrapper around triggering requests in the Akka HTTP DSL.
  *
  * This allows us to extract some of the transport layer logic (e.g. SSL, Unix domain sockets, etc.) from
  * the low-level Docker client implementation. Additionally, I haven't managed so far to make Unix domain
  * sockets work with Akka HTTP's pools - only with `outgoingConnection()` which is only suboptimal.
  */
trait RequestRunner {

  def run(request: HttpRequest)(implicit system: ActorSystem, materializer: ActorMaterializer): Future[HttpResponse]

}

object RequestRunner {

  def singleRequests: RequestRunner = new RequestRunner {
    override def run(request: HttpRequest)(implicit s: ActorSystem, m: ActorMaterializer): Future[HttpResponse] =
      Http().singleRequest(request)
  }

  def unixRequests(socketPath: Path): RequestRunner = new RequestRunner {
    override def run(request: HttpRequest)(implicit s: ActorSystem, m: ActorMaterializer): Future[HttpResponse] = {
      val settings = ClientConnectionSettings(s).withTransport(new UnixTransport(socketPath))
      Source
        .single(request)
        .viaMat(Http().outgoingConnection(request.uri.authority.host.address(), settings = settings))(Keep.right)
        .runWith(Sink.headOption)
        .map(_.getOrElse({
          throw new DockerApiException(s"End of stream reached before receiving response to '$request'.")
        }))(s.dispatcher)
    }
  }

}
