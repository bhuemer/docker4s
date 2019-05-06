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

package org.docker4s.models.images

import org.docker4s.models.ModelsSpec

class ImageLoadedTest extends ModelsSpec {

  "Decoding JSON into images loaded" should "work" in {
    val imageLoaded = decodeImageLoaded(
      """{
         |  "stream" : "Loaded image ID: sha256:af2f74c517aac1d26793a6ed05ff45b299a037e1a9eefeae5eacda133e70a825\n"
         |}""".stripMargin)
    imageLoaded should be(
      ImageLoaded(Image.Id("sha256:af2f74c517aac1d26793a6ed05ff45b299a037e1a9eefeae5eacda133e70a825")))
  }

  private def decodeImageLoaded(str: String): ImageLoaded = decode(str, ImageLoaded.decoder)

}
