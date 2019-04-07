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
package org.docker4s.models.containers

import org.scalatest.{FlatSpec, Matchers}

class ContainerChangeTest extends FlatSpec with Matchers {

  "Decoding JSON into container changes" should "work" in {
    decodeContainerChange("""{
      |  "Path": "/dev",
      |  "Kind": 0
      |}""".stripMargin) should be(ContainerChange("/dev", ContainerChange.Kind.Modified))

    decodeContainerChange("""{
      |  "Path": "/dev/kmsg",
      |  "Kind": 1
      |}""".stripMargin) should be(ContainerChange("/dev/kmsg", ContainerChange.Kind.Added))

    decodeContainerChange("""{
      |  "Path": "/home/hello.txt",
      |  "Kind": 2
      |}""".stripMargin) should be(ContainerChange("/home/hello.txt", ContainerChange.Kind.Deleted))
  }

  // -------------------------------------------- Utility methods

  /** Decodes the given string as a [[ContainerChange]] or throws an exception if something goes wrong. */
  private def decodeContainerChange(str: String): ContainerChange = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(ContainerChange.decoder).fold(throw _, Predef.identity)
  }

}
