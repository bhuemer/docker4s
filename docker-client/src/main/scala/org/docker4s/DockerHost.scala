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
import org.docker4s.util.Certificates

sealed trait DockerHost

object DockerHost extends LazyLogging {

  private val DEFAULT_CA_CERT_NAME = "ca.pem"
  private val DEFAULT_CLIENT_CERT_NAME = "cert.pem"
  private val DEFAULT_CLIENT_KEY_NAME = "key.pem"

  private val DEFAULT_UNIX_PATH = "/var/run/docker.sock"
  private val DEFAULT_ADDRESS = "localhost"
  private val DEFAULT_PORT = 2375

  /**
    * Connects via UNIX domain sockets to the given docker host.
    */
  case class Unix(socketPath: Path, sslContext: Option[SSLContext]) extends DockerHost

  // TODO: Npipe/Windows channels still need to be implemented
  // case class Npipe(socketPath: Path, sslContext: Option[SSLContext]) extends DockerHost

  /**
    * Connects via TCP to the given docker host.
    */
  case class Tcp(host: String, port: Int, sslContext: Option[SSLContext]) extends DockerHost

  def fromEnvironment(environment: Environment): DockerHost = {
    def sslContext: Option[SSLContext] = {
      val noContext = environment.getProperty("DOCKER_TLS_VERIFY").contains("0")
      if (noContext) {
        None
      } else {
        // By default, if we are supposed to load certificates, use the environment variable ..
        val context = environment.getProperty("DOCKER_CERT_PATH").map(Paths.get(_)).flatMap(loadCertificates)

        // .. but fall back to .docker in the user's home directory if we haven't found anything.
        context.orElse(environment.getProperty("user.home").map(Paths.get(_, ".docker")).flatMap(loadCertificates))
      }
    }

    environment.getProperty("DOCKER_HOST") match {
      case Some(dockerHost) => from(dockerHost, sslContext)

      case None if environment.isOsX || environment.isLinux =>
        DockerHost.Unix(Paths.get(DEFAULT_UNIX_PATH), sslContext)

      case None =>
        DockerHost.Tcp(DEFAULT_ADDRESS, DEFAULT_PORT, sslContext)
    }
  }

  def from(dockerHost: String, sslContext: Option[SSLContext] = None): DockerHost = {
    dockerHost.toLowerCase match {
      case tcp if tcp.startsWith("tcp://") =>
        dockerHost.substring("tcp://".length).split(":") match {
          case Array(host)       => DockerHost.Tcp(host, DEFAULT_PORT, sslContext)
          case Array(host, port) => DockerHost.Tcp(host, port.toInt, sslContext)
          case _ =>
            throw new IllegalArgumentException(s"Cannot determine the host and port for Docker host: $dockerHost")
        }

      case unix if unix.startsWith("unix://") =>
        DockerHost.Unix(Paths.get(dockerHost.substring("unix://".length)), sslContext)

      case npipe if npipe.startsWith("npipe://") =>
        throw new IllegalArgumentException(s"Npipe endpoints are not yet supported: $dockerHost")

      case _ =>
        throw new IllegalArgumentException(s"Unsupported docker host scheme: $dockerHost")
    }
  }

  def loadCertificates(certificatesPath: Path): Option[SSLContext] = {
    loadCertificates(
      clientKey = certificatesPath.resolve(DEFAULT_CLIENT_KEY_NAME),
      clientCerts = certificatesPath.resolve(DEFAULT_CLIENT_CERT_NAME),
      caCerts = certificatesPath.resolve(DEFAULT_CA_CERT_NAME)
    )
  }

  def loadCertificates(clientKey: Path, clientCerts: Path, caCerts: Path): Option[SSLContext] = {
    val everythingExists = List(
      clientKey,
      clientCerts,
      caCerts
    ).forall({ path =>
      val exists = Files.exists(path)
      if (!exists) {
        logger.warn(s"Cannot load certificates from the given path: $path does not exist.")
      }
      exists
    })
    if (everythingExists) {
      Some(Certificates.createSslContext(clientKey, clientCerts = clientCerts, caCerts = caCerts))
    } else {
      None
    }
  }

}
