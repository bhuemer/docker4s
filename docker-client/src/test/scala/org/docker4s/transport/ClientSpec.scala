package org.docker4s.transport

import java.nio.charset.StandardCharsets

import fs2.{Stream, text}
import org.scalatest.{FlatSpec, Matchers}

class ClientSpec extends FlatSpec with Matchers {

  "" should "" in {
    val line1 = withStdInHeader("Hello")
    val line2 = withStdInHeader("World")

    println()

//    val input = Stream.emits(
//      "2019-03-18T21:14:52.147249000Z / # \r/ # \u001B[Jecho Hello\n2019-03-18T21:14:52.147321200Z Hello".getBytes(
//        StandardCharsets.UTF_8))
//    val lines = input.through(text.utf8Decode).through(text.lines).compile.toList
//    lines.foreach({ line =>
//      println("Line: {")
//      print(line)
//      println("}")
//    })
  }

  def withStdInHeader(line: String): Array[Byte] = {
    val bytes = line.getBytes

    val result = Array.fill[Byte](bytes.length + 9)(0)
    System.arraycopy(bytes, 0, result, 8, bytes.length)
    result(result.length - 1) = '\n'

    result
  }

}
