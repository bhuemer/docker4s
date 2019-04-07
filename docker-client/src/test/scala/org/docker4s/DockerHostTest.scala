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

import java.nio.file.Paths

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class DockerHostTest extends FlatSpec with Matchers {

  "The default endpoints" should "be OS dependent" in {
    DockerHost.fromEnvironment(
      Environment.from("os.name" -> "Mac OS X")
    ) should be(DockerHost.Unix(socketPath = Paths.get("/var/run/docker.sock"), sslContext = None))

    DockerHost.fromEnvironment(
      Environment.from("os.name" -> "Linux")
    ) should be(DockerHost.Unix(socketPath = Paths.get("/var/run/docker.sock"), sslContext = None))

    // TODO: Implement npipe support
    DockerHost.fromEnvironment(
      Environment.from("os.name" -> "Windows 10")
    ) should be(DockerHost.Tcp(host = "localhost", port = 2375, sslContext = None))
  }

  "The DOCKER_HOST environment variable" should "allow for TCP hosts" in {
    DockerHost.fromEnvironment(
      Environment.from("DOCKER_HOST" -> "tcp://34.73.66.175:2376")
    ) should be(DockerHost.Tcp(host = "34.73.66.175", port = 2376, sslContext = None))
  }

  "The DOCKER_HOST environment variable" should "allow for TCP hosts without ports specified" in {
    DockerHost.fromEnvironment(
      Environment.from("DOCKER_HOST" -> "tcp://34.73.66.175")
    ) should be(DockerHost.Tcp(host = "34.73.66.175", port = 2375, sslContext = None))
  }

  "The DOCKER_HOST environment variable" should "allow for UNIX domain sockets" in {
    DockerHost.fromEnvironment(
      Environment.from("DOCKER_HOST" -> "unix:///var/run/docker.sock")
    ) should be(DockerHost.Unix(socketPath = Paths.get("/var/run/docker.sock"), sslContext = None))

    DockerHost.fromEnvironment(
      Environment.from("DOCKER_HOST" -> "unix:///opt/run/docker.sock")
    ) should be(DockerHost.Unix(socketPath = Paths.get("/opt/run/docker.sock"), sslContext = None))
  }

  "Empty strings for DOCKER_CERT_PATH" should "be treated as unset" in {
    DockerHost.fromEnvironment(
      Environment.from(
        "DOCKER_HOST" -> "tcp://34.73.66.175:2376",
        "DOCKER_CERT_PATH" -> ""
      )) should be(DockerHost.Tcp(host = "34.73.66.175", port = 2376, sslContext = None))
  }

  "The DOCKER_CERT_PATH environment variable" should "be ignored if it points to a non-existing file" in {
    DockerHost.fromEnvironment(
      Environment.from(
        "DOCKER_HOST" -> "tcp://34.73.66.175:2376",
        "DOCKER_CERT_PATH" -> "/var/run/docker/non-existing/certs/directory"
      )) should be(DockerHost.Tcp(host = "34.73.66.175", port = 2376, sslContext = None))
  }

}
