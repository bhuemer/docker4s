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

class ImagesRemovedTest extends FlatSpec with Matchers {

  "Decoding JSON into images removed" should "work" in {
    val imagesRemoved = decodeImagesRemoved(
      """[
        |  { "Untagged": "mysql:latest" },
        |  { "Untagged": "mysql@sha256:a7cf659a764732a27963429a87eccc8457e6d4af0ee9d5140a3b56e74986eed7" },
        |  { "Deleted": "sha256:7bb2586065cd50457e315a5dab0732a87c45c5fad619c017732f5a13e58b51dd" },
        |  { "Deleted": "sha256:5362a97701b6ba3d39453e9e8b435b4cc3d7b493fb506780b8e2ec9c3ee20b8e" },
        |  { "Deleted": "sha256:91ae6166a0c2de34f36c2bb6b66ba3782ec6186337b87362961fad845f5ce236" },
        |  { "Deleted": "sha256:ae307e2a0c006325d4b96c1f0a40b1ae35cf9a034435ec9925291dafed69e01f" },
        |  { "Deleted": "sha256:b000e0eccfc8ce916a49183e5168c11b65085d940d76ef9c7c264462aa5f1f5c" },
        |  { "Deleted": "sha256:97a8302a9556468404f8aeda16ef35d94075cc95ed72249fb2bc2835984e8dd5" },
        |  { "Deleted": "sha256:4e83d49f100fcd78f57b8f1844faad134d8328c5280be14d85a61ae7c7ffb981" },
        |  { "Deleted": "sha256:11f5b5e11d25f08da50ca38993f02a3194565beaa516b66970adbbffa58141e0" },
        |  { "Deleted": "sha256:2db1d1b89506973f51991ae03cb18e24f16aa2ef2b6cda4cf172b7ae4f8c15a7" },
        |  { "Deleted": "sha256:31cd685e0a1656331ab15ee8aadf47ccb1580e80baef149a8e1dfa0e7283ce64" },
        |  { "Deleted": "sha256:5f10b55a337d5bf432b0e44e22bd5730bde67251201b6890e923e3d2c641fa64" },
        |  { "Deleted": "sha256:85c59144fcd7e0cba06a9ec8e502b993bcf75035c95524aa6c92b3072828ce94" },
        |  { "Deleted": "sha256:5dacd731af1b0386ead06c8b1feff9f65d9e0bdfec032d2cd0bc03690698feda" }
        |]""".stripMargin)

    imagesRemoved should be(
      ImagesRemoved(List(
        Untagged("mysql:latest"),
        Untagged("mysql@sha256:a7cf659a764732a27963429a87eccc8457e6d4af0ee9d5140a3b56e74986eed7"),
        Deleted(Id("sha256:7bb2586065cd50457e315a5dab0732a87c45c5fad619c017732f5a13e58b51dd")),
        Deleted(Id("sha256:5362a97701b6ba3d39453e9e8b435b4cc3d7b493fb506780b8e2ec9c3ee20b8e")),
        Deleted(Id("sha256:91ae6166a0c2de34f36c2bb6b66ba3782ec6186337b87362961fad845f5ce236")),
        Deleted(Id("sha256:ae307e2a0c006325d4b96c1f0a40b1ae35cf9a034435ec9925291dafed69e01f")),
        Deleted(Id("sha256:b000e0eccfc8ce916a49183e5168c11b65085d940d76ef9c7c264462aa5f1f5c")),
        Deleted(Id("sha256:97a8302a9556468404f8aeda16ef35d94075cc95ed72249fb2bc2835984e8dd5")),
        Deleted(Id("sha256:4e83d49f100fcd78f57b8f1844faad134d8328c5280be14d85a61ae7c7ffb981")),
        Deleted(Id("sha256:11f5b5e11d25f08da50ca38993f02a3194565beaa516b66970adbbffa58141e0")),
        Deleted(Id("sha256:2db1d1b89506973f51991ae03cb18e24f16aa2ef2b6cda4cf172b7ae4f8c15a7")),
        Deleted(Id("sha256:31cd685e0a1656331ab15ee8aadf47ccb1580e80baef149a8e1dfa0e7283ce64")),
        Deleted(Id("sha256:5f10b55a337d5bf432b0e44e22bd5730bde67251201b6890e923e3d2c641fa64")),
        Deleted(Id("sha256:85c59144fcd7e0cba06a9ec8e502b993bcf75035c95524aa6c92b3072828ce94")),
        Deleted(Id("sha256:5dacd731af1b0386ead06c8b1feff9f65d9e0bdfec032d2cd0bc03690698feda"))
      )))
  }

  // -------------------------------------------- Utility methods

  /** Decodes the given string as an [[ImagesRemoved]] or throws an exception if something goes wrong. */
  private def decodeImagesRemoved(str: String): ImagesRemoved = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(ImagesRemoved.decoder).fold(throw _, Predef.identity)
  }

}
