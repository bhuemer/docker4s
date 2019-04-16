package org.docker4s.models.images

import org.scalatest.{FlatSpec, Matchers}

class ImageLoadedTest extends FlatSpec with Matchers {

  "Decoding JSON into images loaded" should "work" in {
    val imageLoaded = decodeImageLoaded(
      """{
         |  "stream" : "Loaded image ID: sha256:af2f74c517aac1d26793a6ed05ff45b299a037e1a9eefeae5eacda133e70a825\n"
         |}""".stripMargin)
    imageLoaded should be(
      ImageLoaded(Image.Id("sha256:af2f74c517aac1d26793a6ed05ff45b299a037e1a9eefeae5eacda133e70a825")))
  }

  private def decodeImageLoaded(str: String): ImageLoaded = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(ImageLoaded.decoder).fold(throw _, Predef.identity)
  }

}
