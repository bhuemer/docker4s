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
package org.docker4s.util

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import fs2.{Chunk, Pipe, Pull, Stream, text}
import org.docker4s.api.Containers

import scala.language.higherKinds

/**
  * Docker daemons are prefixing log entries by a header, to determine if the subsequent entry is for `stdout`,
  * `stderr`, etc. and to indicate the length of the entry. This class/object takes care of decoding these log streams.
  */
object LogDecoder {

  def decode[F[_]]: Pipe[F, Byte, Containers.Log] = { in =>
    decodingHeader(Chunk.empty, in).stream
  }

  /**
    * Represents the state of the decoder where we're still accumulating bytes for a full header (8 bytes).
    */
  private def decodingHeader[F[_]](current: Chunk[Byte], s: Stream[F, Byte]): Pull[F, Containers.Log, Unit] = {
    current.size match {
      case size if size < 8 =>
        s.pull.uncons.flatMap[F, Containers.Log, Unit]({
          case Some((hd, tail)) =>
            decodingHeader(Chunk.concat(Seq(current, hd)), tail)

          // Drain the buffer as normal stdout log entries, if we couldn't even consume enough bytes for the header.
          case None if !current.isEmpty =>
            Pull.output1(
              Containers.Log(Containers.Stream.StdOut, new String(current.toArray, StandardCharsets.UTF_8).trim)
            ) >> Pull.done

          case None =>
            Pull.done
        })

      case _ =>
        decodeHeader(current) match {
          case Some((stream, frameSize)) =>
            val (_, frame) = current.splitAt(8)
            decodingFrame(stream, frameSize, frame, s)

          case None => fallback(current, s)
        }
    }
  }

  /**
    * Represents the state of the decoder where we're still accumulating bytes to reach the frame size we've decoded.
    *
    * @param stream indicator for the stream (`stderr`, `stdout`, or `stdin`) for which we're decoding this message
    * @param frameSize the number of bytes we should include in this frame
    */
  private def decodingFrame[F[_]](
      stream: Containers.Stream,
      frameSize: Int,
      current: Chunk[Byte],
      s: Stream[F, Byte]): Pull[F, Containers.Log, Unit] = {
    current.size match {
      case size if size < frameSize =>
        s.pull.uncons.flatMap[F, Containers.Log, Unit]({
          case Some((hd, tail)) =>
            decodingFrame(stream, frameSize, Chunk.concat(Seq(current, hd)), tail)

          case None if !current.isEmpty =>
            Pull.output1(
              Containers.Log(stream, new String(current.toArray, StandardCharsets.UTF_8).trim)
            ) >> Pull.done

          case None => Pull.done
        })

      case _ =>
        val (frame, next) = current.splitAt(frameSize)
        val output: Containers.Log =
          Containers.Log(stream, new String(frame.toArray, StandardCharsets.UTF_8).trim)
        Pull.output1(output) >> decodingHeader(next, s)

    }
  }

  /**
    * Decodes a header in the format `[STREAM_TYPE, 0, 0, 0, SIZE1, SIZE2, SIZE3, SIZE4]` for a log frame. If no
    * header can be decoded, e.g. because it doesn't follow the format specified, `None` will be returned.
    */
  private[docker4s] def decodeHeader(header: Chunk[Byte]): Option[(Containers.Stream, Int)] = {
    if (header(1) != 0
        || header(2) != 0
        || header(3) != 0) {
      None
    } else {
      val maybeStream = header(0) match {
        case 0x00 => Some(Containers.Stream.StdIn)
        case 0x01 => Some(Containers.Stream.StdOut)
        case 0x02 => Some(Containers.Stream.StdErr)
        case _    => None
      }

      val result = maybeStream.map({ stream =>
        val frameSize = ByteBuffer.wrap(header.toArray).getInt(4)
        (stream, frameSize)
      })

      result
    }
  }

  /**
    * Some log streams don't include header frames at all. Once we encounter one of those, we switch into this
    * fallback mode, where we don't even try decoding headers and just assume they are all lines intended for
    * `stdout`.
    */
  private def fallback[F[_]](buffer: Chunk[Byte], s: Stream[F, Byte]): Pull[F, Containers.Log, Unit] = {
    val stream = Pull.output(buffer).stream.append(s)
    stream
      .through(text.utf8Decode)
      .through(text.lines)
      .map({ line =>
        Containers.Log(Containers.Stream.StdOut, line)
      })
      .pull
      .echo
  }

}
