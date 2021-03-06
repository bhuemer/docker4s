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
package org.docker4s.http4s

import cats.effect.{ConcurrentEffect, Resource}
import org.docker4s.http4s.transport.Http4sClient
import org.docker4s.http4s.transport.unix.DomainSocketClient
import org.docker4s.{DefaultDockerClient, DockerClient, DockerHost, Environment}
import org.http4s.Uri
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

object Http4sDockerClient {

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

  def fromHost[F[_]: ConcurrentEffect](dockerHost: DockerHost)(
      implicit ec: ExecutionContext): Resource[F, DockerClient[F]] = {
    dockerHost match {
      case DockerHost.Unix(socketPath, sslContext) =>
        DomainSocketClient(socketPath).map({ client =>
          new DefaultDockerClient[F](new Http4sClient(client, uri = Uri.unsafeFromString("http://localhost")))
        })

      case DockerHost.Tcp(host, port, sslContext) =>
        BlazeClientBuilder[F](ec)
        // TODO: Make this configurable
          .withRequestTimeout(FiniteDuration(60, "seconds"))
          .withResponseHeaderTimeout(FiniteDuration(60, "seconds"))
          .withSslContextOption(sslContext)
          .resource
          .map({ client =>
            val scheme = if (sslContext.isDefined) "https" else "http"
            new DefaultDockerClient[F](new Http4sClient(client, uri = Uri.unsafeFromString(s"$scheme://$host:$port")))
          })

    }
  }

}
