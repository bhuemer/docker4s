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
package org.docker4s.models.volumes

import java.time.ZonedDateTime

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class VolumeSpec extends FlatSpec with Matchers {

  "Decoding JSON into volumes" should "extract all the relevant pieces of information" in {
    val volume = decodeVolume("""{
      |    "CreatedAt": "2019-03-12T22:35:19Z",
      |    "Driver": "local",
      |    "Labels": {
      |      "com.example.some-label": "some-value",
      |      "com.example.some-other-label": "some-other-value"
      |    },
      |    "Mountpoint": "/var/lib/docker/volumes/my-vol/_data",
      |    "Name": "my-vol",
      |    "Options": {
      |      "device": "tmpfs",
      |      "o": "size=100m,uid=1000",
      |      "type": "tmpfs"
      |    },
      |    "Scope": "local"
      |}""".stripMargin)

    volume should be(
      Volume(
        name = "my-vol",
        driver = "local",
        mountpoint = "/var/lib/docker/volumes/my-vol/_data",
        status = Map.empty,
        labels = Map(
          "com.example.some-label" -> "some-value",
          "com.example.some-other-label" -> "some-other-value"
        ),
        options = Map(
          "device" -> "tmpfs",
          "o" -> "size=100m,uid=1000",
          "type" -> "tmpfs"
        ),
        scope = Volume.Scope.Local,
        createdAt = ZonedDateTime.parse("2019-03-12T22:35:19Z")
      )
    )
  }

  // -------------------------------------------- Utility methods

  /** Decodes the given string as a [[Volume]] or throws an exception if something goes wrong. */
  private def decodeVolume(str: String): Volume = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(Volume.decoder).fold(throw _, Predef.identity)
  }

}
