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
package org.docker4s.models.images

import java.time.ZonedDateTime

import org.scalatest.{FlatSpec, Matchers}

/**
  * Contains test cases related to parsing images from JSON response bodies.
  */
class ImageSummarySpec extends FlatSpec with Matchers {

  /** Makes sure the decoder is not overly restrictive wrt/ `null` vs empty objects/maps. */
  "Decoding JSON into images" should "cope with `null` labels" in {
    val image =
      decodeImage("""{
      |  "Containers": -1,
      |  "Created": 1530586325,
      |  "Id": "sha256:353d7641c769b651ecaf0d72aca46b886372e3ccf15ab2a6ce8be857bae85daa",
      |  "Labels": null,
      |  "ParentId": "",
      |  "RepoDigests": [
      |    "apache/zeppelin@sha256:ee0698020e03ac16e81f1abe202689c64b2de9a523b7b36df7a721c2f527f913"
      |  ],
      |  "RepoTags": [
      |    "apache/zeppelin:0.8.0"
      |  ],
      |  "SharedSize": -1,
      |  "Size": 2578524535,
      |  "VirtualSize": 2578524535
      |}""".stripMargin)

    image should be(
      ImageSummary(
        id = Image.Id("sha256:353d7641c769b651ecaf0d72aca46b886372e3ccf15ab2a6ce8be857bae85daa"),
        parentId = Image.Id(""),
        createdAt = ZonedDateTime.parse("2018-07-03T02:52:05Z"),
        containers = -1,
        labels = Map.empty,
        repoTags = List("apache/zeppelin:0.8.0"),
        repoDigests = List("apache/zeppelin@sha256:ee0698020e03ac16e81f1abe202689c64b2de9a523b7b36df7a721c2f527f913")
      ))
  }

  /** Makes sure the decoder is not overly restrictive wrt/ `null` vs empty lists. */
  "Decoding JSON into images" should "cope with `null` repo digests" in {
    val image = decodeImage("""{
      |  "Containers": -1,
      |  "Created": 1544287687,
      |  "Id": "sha256:dd1db67e5b51500664f82a4770cfad56a1abb24fafdc8de2035a9265b992f0ba",
      |  "Labels": {
      |    "maintainer": "Bernhard Huemer <bernhard.huemer@gmail.com>"
      |  },
      |  "ParentId": "sha256:8faa0e5ae99c35ace7eb793e1fa62f7e3de2a5aed8a724d59215fc98b91627f3",
      |  "RepoDigests": null,
      |  "RepoTags": [
      |    "jupyter-kernelgateway:latest",
      |    "minimal-gateway:latest"
      |  ],
      |  "SharedSize": -1,
      |  "Size": 2696502890,
      |  "VirtualSize": 2696502890
      |}""".stripMargin)

    image should be(
      ImageSummary(
        id = Image.Id("sha256:dd1db67e5b51500664f82a4770cfad56a1abb24fafdc8de2035a9265b992f0ba"),
        parentId = Image.Id("sha256:8faa0e5ae99c35ace7eb793e1fa62f7e3de2a5aed8a724d59215fc98b91627f3"),
        createdAt = ZonedDateTime.parse("2018-12-08T16:48:07Z"),
        containers = -1,
        labels = Map("maintainer" -> "Bernhard Huemer <bernhard.huemer@gmail.com>"),
        repoTags = List("jupyter-kernelgateway:latest", "minimal-gateway:latest"),
        repoDigests = List.empty
      )
    )
  }

  "Decoding JSON into images" should "automatically remove dummy repo tags/digests" in {
    val image = decodeImage("""{
      |  "Containers": -1,
      |  "Created": 1544285336,
      |  "Id": "sha256:ecad69f6c9e8313706fdbe26d4306c3bf04feb7f7af2615f18afe5c5d47f695c",
      |  "Labels": {
      |    "maintainer": "Bernhard Huemer <bernhard.huemer@gmail.com>"
      |  },
      |  "ParentId": "sha256:73aab8755cc5c2db2459090735451c57843796733a2c836b3a0b629f8a0ceae2",
      |  "RepoDigests": [
      |    "<none>@<none>"
      |  ],
      |  "RepoTags": [
      |    "<none>:<none>"
      |  ],
      |  "SharedSize": -1,
      |  "Size": 717408860,
      |  "VirtualSize": 717408860
      |}""".stripMargin)

    image should be(
      ImageSummary(
        id = Image.Id("sha256:ecad69f6c9e8313706fdbe26d4306c3bf04feb7f7af2615f18afe5c5d47f695c"),
        parentId = Image.Id("sha256:73aab8755cc5c2db2459090735451c57843796733a2c836b3a0b629f8a0ceae2"),
        createdAt = ZonedDateTime.parse("2018-12-08T16:08:56Z"),
        containers = -1,
        labels = Map("maintainer" -> "Bernhard Huemer <bernhard.huemer@gmail.com>"),
        repoTags = List.empty,
        repoDigests = List.empty
      ))
  }

  // -------------------------------------------- Utility methods

  /** Decodes the given string as an [[ImageSummary]] or throws an exception if something goes wrong. */
  private def decodeImage(str: String): ImageSummary = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(ImageSummary.decoder).fold(throw _, Predef.identity)
  }

}
