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
package org.docker4s.akka

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpsConnectionContext
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.ActorMaterializer
import cats.effect.{ConcurrentEffect, Resource}
import javax.net.ssl.SSLContext
import org.docker4s.akka.transport.{AkkaClient, RequestRunner}
import org.docker4s.akka.transport.unix.UnixTransport
import org.docker4s.transport.Client
import org.docker4s.{DefaultDockerClient, DockerClient, DockerHost, Environment}

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object AkkaDockerClient {

  /**
    * Returns a [[DockerClient]] configured from environment variables.
    *
    * The environment variables used are the same as those used by the Docker command-line client. They are:
    *  - '''DOCKER_HOST''' - the URL to the Docker host
    *  - '''DOCKER_TLS_VERIFY''' - verify the host against a CA certificate
    *  - '''DOCKER_CERT_PATH''' - path to a directory containing TLS certificates to use when connecting
    */
  def fromEnvironment[F[_]: ConcurrentEffect](implicit ec: ExecutionContext): Resource[F, DockerClient[F]] = {
    fromEnvironment(Environment.Live)
  }

  def fromEnvironment[F[_]: ConcurrentEffect](environment: Environment)(
      implicit ec: ExecutionContext): Resource[F, DockerClient[F]] = {
    fromHost(DockerHost.fromEnvironment(environment))
  }

  def fromHost[F[_]](
      dockerHost: DockerHost)(implicit F: ConcurrentEffect[F], ec: ExecutionContext): Resource[F, DockerClient[F]] = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    dockerHost match {
      case DockerHost.Unix(socketPath, sslContext) =>
        val client: Client[F] = new AkkaClient[F](
          uri = Uri(s"http://localhost"),
          RequestRunner.unixRequests(socketPath)
        )

        Resource.liftF(F.delay(new DefaultDockerClient[F](client)))

      case DockerHost.Tcp(host, port, sslContext) =>
        val scheme = if (sslContext.isDefined) "https" else "http"
        val client: Client[F] = new AkkaClient(
          uri = Uri(s"$scheme://$host:$port"),
          RequestRunner.singleRequests
        )

        Resource.liftF(F.delay(new DefaultDockerClient[F](client)))

    }
  }

}
