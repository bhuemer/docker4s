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
package org.docker4s.api

import fs2.Stream
import org.docker4s.util.Compression
import org.docker4s.syntax._
import org.scalatest.Matchers

class ImagesBuildIntegrationTest extends ClientSpec with Matchers {

  "The client" should "support building images from in-memory Dockerfiles" given { client =>
    val image = Stream
      .emits(Seq(
        Compression.TarEntry(
          "Dockerfile",
          """
            |FROM busybox:latest
            |COPY hello.txt .
            |CMD ["cat", "hello.txt"]
          """.stripMargin.getBytes
        ),
        Compression.TarEntry(
          "hello.txt",
          "Hello World from a container built with docker4s".getBytes
        )
      ))
      .through(Compression.tar())
      .through(Compression.gzip())

    for {
      build <- client.images.build(image, name = Some("docker4s-in-memory-test")).result
      _ = build.imageId.isDefined should be(true)

      // Run a container with the image we just created
      container <- client.containers.create(build.imageId.get.value)
      _ <- client.containers.start(container.id)
      _ <- client.containers.stop(container.id)
      _ <- client.containers.await(container.id)

      logs <- client.containers.logs(container.id, Containers.LogParameter.stdout).compile.toList
      _ = logs should be(
        List(Containers.Log(Containers.Stream.StdOut, "Hello World from a container built with docker4s")))
    } yield ()
  }

}
