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

import java.net.InetSocketAddress
import java.nio.file.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.Http.OutgoingConnection
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.stream.ActorMaterializer
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import javax.net.ssl.SSLContext
import jnr.unixsocket.UnixSocketAddress
import org.docker4s.errors.DockerApiException

import scala.concurrent.Future
import scala.language.higherKinds

/**
  * Lightweight wrapper around triggering requests in the Akka HTTP DSL.
  *
  * This allows us to extract some of the transport layer logic (e.g. SSL, Unix domain sockets, etc.)
  * from the low-level Docker client implementation and make it more testable/mock-able.
  */
trait ClientTransport {

  def run(request: HttpRequest): Future[HttpResponse]

}

object ClientTransport {

  def singleRequests(sslContext: Option[SSLContext])(
      implicit system: ActorSystem,
      materializer: ActorMaterializer): ClientTransport = {
    val connectionContext = sslContext match {
      case Some(context) =>
        ConnectionContext.https(context)

      case None => Http().defaultClientHttpsContext
    }

    request: HttpRequest =>
      Http().singleRequest(request, connectionContext)
  }

  def unixRequests(socketPath: Path)(implicit system: ActorSystem, materializer: ActorMaterializer): ClientTransport = {
    val settings = ClientConnectionSettings(system).withTransport(new UnixTransport(socketPath))
    request: HttpRequest =>
      Source
        .single(request)
        .viaMat(Http().outgoingConnection(request.uri.authority.host.address(), settings = settings))(Keep.right)
        .runWith(Sink.headOption)
        .map(_.getOrElse({
          throw new DockerApiException(s"End of stream reached before receiving a response to '$request'.")
        }))(system.dispatcher)
  }

  private class UnixTransport(socketPath: Path) extends akka.http.scaladsl.ClientTransport with LazyLogging {

    override def connectTo(host: String, port: Int, settings: ClientConnectionSettings)(
        implicit system: ActorSystem): Flow[ByteString, ByteString, Future[Http.OutgoingConnection]] = {
      UnixDomainSocket()
        .outgoingConnection(new UnixSocketAddress(socketPath.toFile))
        .mapMaterializedValue(_.map({ _ =>
          OutgoingConnection(settings.localAddress.orNull, InetSocketAddress.createUnresolved(host, port))
        })(system.dispatcher))
    }

  }

}
