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

import java.nio.file.{Path, Paths}

import javax.net.ssl.SSLContext

sealed trait DockerHost

object DockerHost {

  def fromEnvironment: DockerHost = {
    val dockerHost = System.getenv("DOCKER_HOST")
    if (dockerHost == null) {
      throw new IllegalStateException("DOCKER_HOST not available.")
    } else if (dockerHost.startsWith("tcp://")) {
      val Array(host, port) = dockerHost.substring("tcp://".length).split(":")

      val sslContext = Option(System.getenv("DOCKER_CERT_PATH")) match {
        case Some(path) => Some(DockerCertificates(Paths.get(path)).sslContext)
        case _          => None
      }

      DockerHost.Tcp(host, port.toInt, sslContext)
    } else {
      throw new IllegalArgumentException(s"Unknown docker host $dockerHost")
    }

    // TODO: Implement this properly
    // DockerHost.Unix(Paths.get("/var/run/docker.sock"), None)
  }

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

}
