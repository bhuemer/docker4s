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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class VolumesPrunedTest extends FlatSpec with Matchers {

  "Decoding JSON into volumes pruned" should "work" in {
    val volumesPruned = decodeVolumesPruned("""{
      |  "VolumesDeleted": [
      |  	"0cbbeafcf00e46c364acbf56730c21205e999e405b8d991e7ba072facf9eb356",
      |  	"9ef1658e9bab5f623ca36892aa14c92f4fc1b7437cc0d8cc3c9bcc3f060af164"
      |  ],
      |  "SpaceReclaimed": 0
      |}""".stripMargin)

    volumesPruned should be(
      VolumesPruned(
        volumes = List("0cbbeafcf00e46c364acbf56730c21205e999e405b8d991e7ba072facf9eb356",
                       "9ef1658e9bab5f623ca36892aa14c92f4fc1b7437cc0d8cc3c9bcc3f060af164"),
        spaceReclaimed = 0L
      ))
  }

  "Decoding JSON into volumes pruned" should "cope with empty lists" in {
    decodeVolumesPruned("""{
        |  "VolumesDeleted": null,
        |  "SpaceReclaimed": 0
        |}
     """.stripMargin) should be(VolumesPruned(volumes = List.empty, spaceReclaimed = 0L))

    decodeVolumesPruned("""{
        |  "VolumesDeleted": [],
        |  "SpaceReclaimed": 0
        |}
     """.stripMargin) should be(VolumesPruned(volumes = List.empty, spaceReclaimed = 0L))
  }

  // -------------------------------------------- Utility methods

  /** Decodes the given string as a [[VolumesPruned]] or throws an exception if something goes wrong. */
  private def decodeVolumesPruned(str: String): VolumesPruned = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(VolumesPruned.decoder).fold(throw _, Predef.identity)
  }

}
