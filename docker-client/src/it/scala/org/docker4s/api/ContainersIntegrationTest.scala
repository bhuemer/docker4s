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

import org.docker4s.api.Containers.ListCriterion.showAll
import org.docker4s.api.Containers.LogCriterion.stdout
import org.docker4s.models.containers.Processes
import org.scalatest.Matchers

class ContainersIntegrationTest extends ClientSpec with Matchers {

  "Running a `hello-world` container" should "produce logs for it" given { client =>
    for {
      _ <- client.images.pull("hello-world").compile.drain

      created <- client.containers.create(image = "hello-world")

      before <- client.containers.logs(created.id, stdout).compile.toList

      _ <- client.containers.start(created.id)

      after <- client.containers.logs(created.id, stdout).compile.toList
    } yield {
      before should be(List.empty)

      after.map(_.message).mkString("\n") should be(
        """
          |Hello from Docker!
          |This message shows that your installation appears to be working correctly.
          |
          |To generate this message, Docker took the following steps:
          | 1. The Docker client contacted the Docker daemon.
          | 2. The Docker daemon pulled the "hello-world" image from the Docker Hub.
          |    (amd64)
          | 3. The Docker daemon created a new container from that image which runs the
          |    executable that produces the output you are currently reading.
          | 4. The Docker daemon streamed that output to the Docker client, which sent it
          |    to your terminal.
          |
          |To try something more ambitious, you can run an Ubuntu container with:
          | $ docker run -it ubuntu bash
          |
          |Share images, automate workflows, and more with a free Docker ID:
          | https://hub.docker.com/
          |
          |For more examples and ideas, visit:
          | https://docs.docker.com/get-started/
          |""".stripMargin)
    }
  }

  "The client" should "support pruning unused containers" given { client =>
    for {
      _ <- client.images.pull("hello-world").compile.drain

      // Create a temporary container to clean up
      created <- client.containers.create(image = "hello-world")

      // Given that this container is not running, it shouldn't appear in the list without additional parameters ..
      containers1 <- client.containers.list()
      _ = containers1.map(_.id) shouldNot contain(created.id)

      // .. but with the additional parameter `all=true` it should appear.
      containers2 <- client.containers.list(showAll)
      _ = containers2.map(_.id) should contain(created.id)

      pruned <- client.containers.prune()
      _ = pruned.containers should contain(created.id)

      containers3 <- client.containers.list(showAll)
      _ = containers3.map(_.id) shouldNot contain(created.id)
    } yield ()
  }

  "The client" should "support listing processes in a container" given { client =>
    for {
      _ <- client.images.pull("nginx").compile.drain

      // Create and start a container for it
      created <- client.containers.create(image = "nginx")
      _ <- client.containers.start(created.id)

      processes <- client.containers.top(created.id, psArgs = Some("-ef"))
      _ = processes.titles should be(List("PID", "USER", "TIME", "COMMAND"))
      _ = processes.processes should have size 2
      _ = processes.processes(0)(3) should include("nginx")
      _ = processes.processes(1)(3) should include("nginx")

      _ <- client.containers.kill(created.id)
    } yield ()
  }

}
