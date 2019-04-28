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
package org.docker4s.akka.transport.unix

import java.net.InetSocketAddress
import java.nio.file.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.Http.OutgoingConnection
import akka.http.scaladsl.{ClientTransport, Http}
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import jnr.unixsocket.UnixSocketAddress

import scala.concurrent.Future

class UnixTransport(socketPath: Path) extends ClientTransport with LazyLogging {

  override def connectTo(host: String, port: Int, settings: ClientConnectionSettings)(
      implicit system: ActorSystem): Flow[ByteString, ByteString, Future[Http.OutgoingConnection]] = {
    logger.debug(s"Connecting to $socketPath for requests to $host:$port.")
    UnixDomainSocket()
      .outgoingConnection(new UnixSocketAddress(socketPath.toFile))
      .mapMaterializedValue(_.map({ _ =>
        OutgoingConnection(settings.localAddress.orNull, InetSocketAddress.createUnresolved(host, port))
      })(system.dispatcher))
  }

}
