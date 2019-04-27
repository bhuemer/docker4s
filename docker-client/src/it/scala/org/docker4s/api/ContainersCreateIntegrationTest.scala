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
package org.docker4s.api

import java.net.URL

import com.typesafe.scalalogging.LazyLogging
import org.docker4s.DockerHost
import org.docker4s.api.Containers.CreateParameter._
import org.docker4s.api.Containers.LogParameter._
import org.docker4s.models.containers.PortBinding
import org.scalatest.Matchers

import scala.concurrent.duration._

class ContainersCreateIntegrationTest extends ClientSpec with Matchers with LazyLogging {

  /**
    * Makes sure that you can override the default specified in an image's `CMD`.
    */
  "The client" should "support specifying additional commands when creating a container" given { client =>
    for {
      _ <- client.images.pull("busybox").compile.drain

      created <- client.containers.create(withImage("busybox"), withCmd("echo", "Hello from docker4s"))
      _ <- client.containers.start(created.id)
      _ <- client.containers.stop(created.id)
      _ <- client.containers.await(created.id)

      logs <- client.containers.logs(created.id, stdout).compile.toList
      _ = logs should be(List(Containers.Log(Containers.Stream.StdOut, "Hello from docker4s")))
    } yield ()
  }

  /**
    * Makes sure that we can run the equivalent to the following Docker command:
    * {{{
    * docker run -p 1234:5678 hashicorp/http-echo -text="hello world"
    * }}}
    */
  "The client" should "support specifying port bindings when creating a container" given { client =>
    for {
      _ <- client.images.pull(name = "hashicorp/http-echo").compile.drain

      container <- client.containers.create(
        withImage("hashicorp/http-echo"),
        withArgs("-text=Hello from Docker4s"),
        withPortBinding(PortBinding(privatePort = 5678, publicPort = Some(1234)))
      )
      _ <- client.containers.start(container.id)

      _ = {
        if (!runningOnCircleCI) {
          val url = new URL(dockerHost match {
            case DockerHost.Tcp(host, _, _) => s"http://$host:1234"
            case DockerHost.Unix(_, _)      => s"http://localhost:1234"
          })
          logger.info(s"Trying to read the echo message from $url.")
          val content = scala.io.Source.fromURL(url).mkString
          content should be("Hello from Docker4s\n")
        }
      }

      _ <- client.containers.stop(container.id, timeout = 1.second)
    } yield ()
  }

  /**
    * Makes sure that we can run the equivalent to the following Docker command:
    * {{{
    * docker run -p 1234:8000 hashicorp/http-echo -listen=:8000 -text="hello world"
    * }}}
    */
  "The client" should "support specifying exposed ports when creating a container" given { client =>
    for {
      _ <- client.images.pull(name = "hashicorp/http-echo").compile.drain

      container <- client.containers.create(
        withImage("hashicorp/http-echo"),
        withArgs("-text=Hello from Docker4s", "-listen=:8000"),
        withExposedPort(port = 8000),
        withPortBinding(PortBinding(privatePort = 8000, publicPort = Some(1235)))
      )
      _ <- client.containers.start(container.id)

      _ = {
        // CircleCI doesn't allow exposed parts on docker machines and seeing that we'd like to
        if (!runningOnCircleCI) {
          val url = new URL(dockerHost match {
            case DockerHost.Tcp(host, _, _) => s"http://$host:1235"
            case DockerHost.Unix(_, _)      => s"http://localhost:1235"
          })
          logger.info(s"Trying to read the echo message from $url.")
          val content = scala.io.Source.fromURL(url).mkString
          content should be("Hello from Docker4s\n")
        }
      }

      _ <- client.containers.stop(container.id, timeout = 1.second)
    } yield ()
  }

}
