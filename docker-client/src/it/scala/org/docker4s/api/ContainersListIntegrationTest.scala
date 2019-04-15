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

import cats.effect.IO
import org.docker4s.DockerClient
import org.docker4s.api.Containers.ListParameter._
import org.docker4s.models.containers.{Container, ContainerCreated}
import org.scalatest.Matchers

class ContainersListIntegrationTest extends ClientSpec with Matchers {

  "The client" should "support including size information in listing containers" given { client =>
    for {
      nginx <- pullCreateAndRun(client, image = "nginx")

      containers <- client.containers.list(withSize)

      _ = containers.size should be >= 1
      _ = containers.foreach({ container =>
        assert(container.sizeRw.isDefined, s"$container does not have `sizeRw` defined.")
        assert(container.sizeRootFs.isDefined, s"$container does not have `sizeRootFs` defined.")
      })

      _ <- client.containers.stop(nginx.id)
    } yield ()
  }

  /**
    * Makes sure that filtering by status parameter works as expected.
    */
  "The client" should "support listing containers by status" given { client =>
    for {
      nginx <- pullCreateAndRun(client, image = "nginx")

      containers <- client.containers.list(withStatus(Container.Status.Running))
      _ = containers.map(_.id) should contain(nginx.id)

      containers <- client.containers.list(withStatus(Container.Status.Exited))
      _ = containers.map(_.id) shouldNot contain(nginx.id)

      _ <- client.containers.stop(nginx.id)

      containers <- client.containers.list(withStatus(Container.Status.Running))
      _ = containers.map(_.id) shouldNot contain(nginx.id)

      containers <- client.containers.list(withStatus(Container.Status.Exited))
      _ = containers.size should be >= 1
      _ = containers.map(_.id) should contain(nginx.id)
    } yield ()
  }

  /** Utility method to create a container from scratch (pull if necessary, etc.). */
  private def pullCreateAndRun(client: DockerClient[IO], image: String): IO[ContainerCreated] = {
    for {
      _ <- client.images.pull(image).compile.drain

      created <- client.containers.create(image)
      _ <- client.containers.start(created.id)
    } yield created
  }

}
