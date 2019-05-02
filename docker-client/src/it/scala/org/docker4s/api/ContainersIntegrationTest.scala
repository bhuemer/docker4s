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

import java.io.{ByteArrayInputStream, InputStream}

import cats.effect.IO
import fs2.Stream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.utils.IOUtils
import org.docker4s.api.Containers.ListParameter.showAll
import org.docker4s.api.Containers.LogParameter.stdout
import org.docker4s.models.containers.ContainerChange
import org.docker4s.util.Compression
import org.docker4s.syntax._
import org.scalatest.Matchers

import scala.io.Codec

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

      processes <- client.containers.top(created.id)
      // The titles aren't really uniform enough to be included in this test.
      // _ = processes.titles should be(List("PID", "USER", "TIME", "COMMAND"))
      _ = processes.processes should have size 2

      _ <- client.containers.kill(created.id)
    } yield ()
  }

  "The client" should "support the stat operation for files in containers" given { client =>
    for {
      build <- client.images.build(busyboxImageWithDummyFiles, name = Some("docker4s-stat-test")).result
      _ = build.imageId.isDefined should be(true)

      container <- client.containers.create(build.imageId.get)
      _ <- client.containers.start(container.id)

      stat <- client.containers.stat(container.id, path = "/home/hello.txt")
      _ = stat.name should be("hello.txt")
      _ = stat.mode.asString should be("-------------rw-r--r--")
      _ = stat.linkTarget should be(None)

      stat <- client.containers.stat(container.id, path = "/home")
      _ = stat.name should be("home")
      _ = stat.mode.asString should be("d------------rwxr-xr-x")
      _ = stat.linkTarget should be(None)

      stat <- client.containers.stat(container.id, path = "/home/link.txt")
      _ = stat.name should be("link.txt")
      _ = stat.mode.asString should be("----L--------rwxrwxrwx")
      _ = stat.linkTarget should be(Some("/home/hello.txt"))
    } yield ()
  }

  "The client" should "support copying files from the container to the client" given { client =>
    for {
      build <- client.images.build(busyboxImageWithDummyFiles, name = Some("docker4s-copy-test1")).result
      _ = build.imageId.isDefined should be(true)

      container <- client.containers.create(build.imageId.get)
      _ <- client.containers.start(container.id)

      bytes <- client.containers.export(container.id, path = Some("/home/hello.txt")).compile.toList.map(_.toArray)
      _ = {
        val ain = new TarArchiveInputStream(new ByteArrayInputStream(bytes))

        val entry = ain.getNextEntry

        entry.getName should be("hello.txt")
        readFully(ain) should be("Hello from Docker4s\n")

        ain.getNextEntry should be(null)
      }
    } yield ()
  }

  "The client" should "support copying files from the client to the container" given { client =>
    for {
      build <- client.images.build(busyboxImageWithDummyFiles, name = Some("docker4s-copy-test1")).result
      _ = build.imageId.isDefined should be(true)

      container <- client.containers.create(build.imageId.get)
      _ <- client.containers.start(container.id)

      _ <- client.containers.upload(
        container.id,
        path = "/home",
        Stream
          .emits(
            Seq(
              Compression.TarEntry("uploaded1.txt", "Uploaded1 from Docker4s".getBytes),
              Compression.TarEntry("uploaded2.txt", "Uploaded2 from Docker4s".getBytes)
            ))
          .through(Compression.tar())
          .through(Compression.gzip())
      )

      changes <- client.containers.diff(container.id)
      _ = changes should contain theSameElementsAs
        List(
          ContainerChange("/home", ContainerChange.Kind.Modified),
          ContainerChange("/home/uploaded1.txt", ContainerChange.Kind.Added),
          ContainerChange("/home/uploaded2.txt", ContainerChange.Kind.Added)
        )
    } yield ()
  }

  "The client" should "support executing commands inside containers" given { client =>
    for {
      _ <- client.images.pull("jupyter/minimal-notebook").compile.drain

      container <- client.containers.create("jupyter/minimal-notebook")
      _ <- client.containers.start(container.id)

      _ <- client.execs
        .run(container.id, "pip", "install", "tensorflow")
        .evalTap[IO](message => IO(println(message)))
        .compile
        .drain

      diff <- client.containers.diff(container.id)
      _ = diff should contain(ContainerChange("/opt/conda/bin/tensorboard", ContainerChange.Kind.Added))
      _ = diff should contain(
        ContainerChange("/opt/conda/lib/python3.7/site-packages/tensorflow", ContainerChange.Kind.Added))
    } yield ()
  }

  private val busyboxImageWithDummyFiles = Stream
    .emits(
      Seq(
        Compression.TarEntry(
          "Dockerfile",
          """
            |FROM busybox:latest
            |RUN echo "Hello from Docker4s" > /home/hello.txt
            |RUN ln -s /home/hello.txt /home/link.txt
          """.stripMargin.getBytes
        )
      ))
    .through(Compression.tar())
    .through(Compression.gzip())

  private def readFully(is: InputStream)(implicit codec: Codec): String = {
    new String(IOUtils.toByteArray(is), codec.charSet)
  }

}
