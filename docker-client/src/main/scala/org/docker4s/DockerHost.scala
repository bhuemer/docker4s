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

import java.nio.file.{Files, Path, Paths}

import com.typesafe.scalalogging.LazyLogging
import javax.net.ssl.SSLContext

sealed trait DockerHost

object DockerHost extends LazyLogging {

  private val DEFAULT_UNIX_PATH = "/var/run/docker.sock"
  private val DEFAULT_ADDRESS = "localhost"
  private val DEFAULT_PORT = 2375

  /**
    * Connects via UNIX domain sockets to the given docker host.
    */
  case class Unix(socketPath: Path, sslContext: Option[SSLContext]) extends DockerHost

  // TODO: Npipe/Windows channels still need to be implemented
  case class Npipe(socketPath: Path, sslContext: Option[SSLContext]) extends DockerHost

  /**
    * Connects via TCP to the given docker host.
    */
  case class Tcp(host: String, port: Int, sslContext: Option[SSLContext]) extends DockerHost

  def fromEnvironment(environment: Environment): DockerHost = {
    def sslContext: Option[SSLContext] = {
      environment.getProperty("DOCKER_CERT_PATH").filter(_.nonEmpty).map(Paths.get(_)) match {
        case Some(path) if Files.exists(path) =>
          logger.info(s"Loading certificates from $path (via DOCKER_CERT_PATH).")
          Some(DockerCertificates(path).sslContext)

        case Some(path) =>
          logger.info(
            s"Not loading certificates from $path, because at least " +
              s"one of 'ca.pem', 'cert.pem', or 'key.pem' doesn't exist.")
          None

        case _ => None
      }
    }

    environment.getProperty("DOCKER_HOST") match {
      case None if environment.isOsX || environment.isLinux =>
        DockerHost.Unix(Paths.get(DEFAULT_UNIX_PATH), sslContext)

      case None =>
        DockerHost.Tcp(DEFAULT_ADDRESS, DEFAULT_PORT, sslContext)

      case Some(dockerHost) if dockerHost.startsWith("tcp://") =>
        dockerHost.substring("tcp://".length).split(":") match {
          case Array(host)       => DockerHost.Tcp(host, DEFAULT_PORT, sslContext)
          case Array(host, port) => DockerHost.Tcp(host, port.toInt, sslContext)
          case _ =>
            throw new IllegalArgumentException(
              s"Cannot determine the host and port for Docker host (DOCKER_HOST: $dockerHost)")
        }

      case Some(dockerHost) if dockerHost.startsWith("npipe://") =>
        throw new IllegalArgumentException(s"Npipe endpoints are not yet supported (DOCKER_HOST: $dockerHost).")

      case Some(dockerHost) if dockerHost.startsWith("unix://") =>
        DockerHost.Unix(Paths.get(dockerHost.substring("unix://".length)), sslContext)

      case Some(dockerHost) =>
        throw new IllegalArgumentException(s"Unsupported docker host scheme: $dockerHost")

    }
  }

}
