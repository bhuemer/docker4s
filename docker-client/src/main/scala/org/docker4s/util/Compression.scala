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

import java.io.ByteArrayOutputStream

import fs2.{Chunk, Pipe, Pure, RaiseThrowable, Stream, compress}
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}

import scala.language.higherKinds

object Compression {

  // 100kb by default
  val DEFAULT_BUFFER_SIZE: Int = 1024 * 100

  case class TarEntry[F[_]](name: String, size: Long, mode: Option[Int], contents: Stream[F, Byte])

  object TarEntry {

    def apply[F[x] >: Pure[x]](name: String, size: Long, contents: Stream[F, Byte]): TarEntry[F] =
      new TarEntry[F](name, size, None, contents)

    def apply[F[x] >: Pure[x]](name: String, bytes: Array[Byte]): TarEntry[F] =
      new TarEntry[F](name, bytes.length, None, Stream.emits(bytes))

  }

  /**
    * Returns a `Pipe` that turns a stream of file contents into a TAR archive.
    */
  def tar[F[_]](bufferSize: Int = DEFAULT_BUFFER_SIZE): Pipe[F, TarEntry[F], Byte] = { in =>
    Stream.suspend({
      val bos: ByteArrayOutputStream = new ByteArrayOutputStream(bufferSize)
      val tos: TarArchiveOutputStream = new TarArchiveOutputStream(bos)

      def consume: Stream[F, Byte] = {
        val back = bos.toByteArray
        bos.reset()
        Stream.chunk(Chunk.bytes(back))
      }

      def processChunk(c: Chunk[Byte]): Unit = c match {
        case Chunk.Bytes(values, off, len) =>
          tos.write(values, off, len)
        case Chunk.ByteVectorChunk(bv) =>
          bv.copyToStream(tos)
        case chunk =>
          val len = chunk.size
          val buf = new Array[Byte](len)
          chunk.copyToArray(buf, 0)
          tos.write(buf)
      }

      val closeArchiveEntry: Stream[F, Byte] = Stream.suspend({
        tos.closeArchiveEntry()
        consume
      })

      val closeArchive: Stream[F, Byte] = Stream.suspend({
        tos.close()
        consume
      })

      val body: Stream[F, Byte] = in.flatMap({ entry =>
        val archiveEntry = new TarArchiveEntry(entry.name)
        archiveEntry.setSize(entry.size)
        entry.mode.foreach(archiveEntry.setMode)
        tos.putArchiveEntry(archiveEntry)

        entry.contents.chunks.flatMap({ chunk =>
          processChunk(chunk)
          tos.flush()
          consume
        }) ++ closeArchiveEntry
      })

      body ++ closeArchive
    })
  }

  def gzip[F[_]](bufferSize: Int = DEFAULT_BUFFER_SIZE): Pipe[F, Byte, Byte] = compress.gzip(bufferSize)

  def gunzip[F[_]: RaiseThrowable](bufferSize: Int = DEFAULT_BUFFER_SIZE): Pipe[F, Byte, Byte] =
    compress.gunzip(bufferSize)

}
