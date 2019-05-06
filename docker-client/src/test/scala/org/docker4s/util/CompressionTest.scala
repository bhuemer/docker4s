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
package org.docker4s.util

import java.io.{BufferedInputStream, FileOutputStream, InputStream}
import java.nio.file.{Files, Path}

import cats.effect.{ContextShift, IO}
import fs2.{Pipe, Stream, io}
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext
import scala.io.Codec

class CompressionTest extends FlatSpec with Matchers {

  "Compression" should "allow you to create simple .tar.gz archives" in {
    val dockerfile = Seq(
      "FROM ubuntu:18.04",
      "COPY . /app",
      "RUN make /app",
      "CMD python /app/app.py"
    ).mkString("\n")

    val configuration = Seq(
      "mysql:",
      "  host: localhost",
      "  user: root"
    ).mkString("\n")

    val stream = Stream
      .emits(
        Seq(
          Compression.TarEntry("Dockerfile", dockerfile.getBytes),
          Compression.TarEntry("config.yaml", configuration.getBytes)
        ))
      .through(Compression.tar())
      .through(Compression.gzip())

    val path = Files.createTempFile("dockertest", ".tar.gz")
    try {
      stream
        .through(writeTo(path))
        .compile
        .drain
        .unsafeRunSync()

      // @formatter:off
      val ain =
        new TarArchiveInputStream(
          new GzipCompressorInputStream(
            new BufferedInputStream(Files.newInputStream(path))))
      // @formatter:on

      var entry = ain.getNextEntry
      entry.getName should be("Dockerfile")
      readFully(ain) should be(dockerfile)

      entry = ain.getNextEntry
      entry.getName should be("config.yaml")
      readFully(ain) should be(configuration)

      entry = ain.getNextEntry
      entry shouldBe null
    } finally {
      Files.delete(path)
    }
  }

  // -------------------------------------------- Utility methods

  private def readFully(is: InputStream)(implicit codec: Codec): String = {
    new String(IOUtils.toByteArray(is), codec.charSet)
  }

  private def writeTo(path: Path): Pipe[IO, Byte, Unit] = {
    implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    io.writeOutputStream[IO](IO.delay(new FileOutputStream(path.toFile)), ExecutionContext.global)
  }

}
