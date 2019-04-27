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
