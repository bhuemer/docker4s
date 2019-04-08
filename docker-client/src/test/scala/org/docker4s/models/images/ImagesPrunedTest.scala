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

import org.docker4s.models.images.Image.Id
import org.docker4s.models.images.ImagesRemoved.Ref.{Deleted, Untagged}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Contains test cases related to parsing [[ImagesPruned]] objects from JSON response bodies.
  */
class ImagesPrunedTest extends FlatSpec with Matchers {

  "Decoding JSON into images pruned" should "work" in {
    val imagesPruned = decodeImagesPruned(
      """{
      |  "ImagesDeleted": [
      |    { "Deleted": "sha256:ecad69f6c9e8313706fdbe26d4306c3bf04feb7f7af2615f18afe5c5d47f695c" },
      |    { "Deleted": "sha256:73aab8755cc5c2db2459090735451c57843796733a2c836b3a0b629f8a0ceae2" },
      |    { "Deleted": "sha256:9d0e9358344f3b3babed81872649b9592267271c455e582432cdc5d87c07d867" },
      |    { "Untagged": "ubuntu@sha256:7a47ccc3bbe8a451b500d2b53104868b46d60ee8f5b35a24b41a86077c650210" },
      |    { "Deleted": "sha256:47b19964fb500f3158ae57f20d16d8784cc4af37c52c49d3b4f5bc5eede49541" }
      |  ],
      |  "SpaceReclaimed": 177226237
      |}""".stripMargin)

    imagesPruned should be(
      ImagesPruned(
        images = List(
          Deleted(Id("sha256:ecad69f6c9e8313706fdbe26d4306c3bf04feb7f7af2615f18afe5c5d47f695c")),
          Deleted(Id("sha256:73aab8755cc5c2db2459090735451c57843796733a2c836b3a0b629f8a0ceae2")),
          Deleted(Id("sha256:9d0e9358344f3b3babed81872649b9592267271c455e582432cdc5d87c07d867")),
          Untagged("ubuntu@sha256:7a47ccc3bbe8a451b500d2b53104868b46d60ee8f5b35a24b41a86077c650210"),
          Deleted(Id("sha256:47b19964fb500f3158ae57f20d16d8784cc4af37c52c49d3b4f5bc5eede49541"))
        ),
        spaceReclaimed = 177226237L
      )
    )
  }

  "Decoding JSON into images pruned" should "cope with empty lists" in {
    decodeImagesPruned("""{
      |  "ImagesDeleted" : null,
      |  "SpaceReclaimed" : 0
      |}
     """.stripMargin) should be(ImagesPruned(images = List.empty, spaceReclaimed = 0))

    decodeImagesPruned("""{
      |  "ImagesDeleted" : [],
      |  "SpaceReclaimed" : 0
      |}
     """.stripMargin) should be(ImagesPruned(images = List.empty, spaceReclaimed = 0))
  }

  // -------------------------------------------- Utility methods

  /** Decodes the given string as an [[ImagesPruned]] or throws an exception if something goes wrong. */
  private def decodeImagesPruned(str: String): ImagesPruned = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(ImagesPruned.decoder).fold(throw _, Predef.identity)
  }

}
