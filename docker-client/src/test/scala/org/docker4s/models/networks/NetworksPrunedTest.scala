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
package org.docker4s.models.networks

import org.scalatest.{FlatSpec, Matchers}

class NetworksPrunedTest extends FlatSpec with Matchers {

  "Decoding JSON into networks pruned" should "work" in {
    val networksPruned = decodeNetworksPruned("""{
        |  "NetworksDeleted": [
        |    "kjq7p8j3jk731l7cq0z87lvex",
        |    "286621d9106d66b492ddbd0c2"
        |  ]
        |}
      """.stripMargin)
    networksPruned should be(
      NetworksPruned(
        networks = List(
          "kjq7p8j3jk731l7cq0z87lvex",
          "286621d9106d66b492ddbd0c2"
        )))
  }

  // -------------------------------------------- Utility methods

  private def decodeNetworksPruned(str: String): NetworksPruned = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(NetworksPruned.decoder).fold(throw _, Predef.identity)
  }

}
