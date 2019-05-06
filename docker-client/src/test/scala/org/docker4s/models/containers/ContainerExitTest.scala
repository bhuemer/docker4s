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
package org.docker4s.models.containers

import org.docker4s.models.ModelsSpec

class ContainerExitTest extends ModelsSpec {

  "Decoding JSON into container exits" should "work with error messages" in {
    decodeContainerExit("""{
        |  "StatusCode": 0,
        |  "Error": {
        |    "Message": "string"
        |  }
        |}""".stripMargin) should be(ContainerExit(0, errorMessage = Some("string")))
  }

  /**
    * Makes sure that parsing of container exit JSONs doesn't fail if the error message isn't specified.
    */
  "Decoding JSON into container exits" should "work without error messages" in {
    decodeContainerExit("""{
        |  "StatusCode": 0
        |}""".stripMargin) should be(ContainerExit(0, errorMessage = None))

    decodeContainerExit("""{
        |  "StatusCode": 0,
        |  "Error": null
        |}""".stripMargin) should be(ContainerExit(0, errorMessage = None))

    decodeContainerExit("""{
        |  "StatusCode": 0,
        |  "Error": { }
        |}""".stripMargin) should be(ContainerExit(0, errorMessage = None))

    decodeContainerExit("""{
        |  "StatusCode": 0,
        |  "Error": {
        |    "Message": null
        |  }
        |}""".stripMargin) should be(ContainerExit(0, errorMessage = None))
  }

  private def decodeContainerExit(str: String): ContainerExit = decode(str, ContainerExit.decoder)

}
