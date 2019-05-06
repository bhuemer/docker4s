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

import scala.concurrent.duration._

class PullEventTest extends ModelsSpec {

  "Decoding JSON into pull events" should "decode `pulling image` events" in {
    val pullEvent = decodePullEvent("""
        |{ "status": "Pulling from library/jetty", "id": "latest" }
      """.stripMargin)
    pullEvent should be(PullEvent.PullingImage("latest: Pulling from library/jetty"))
  }

  "Decoding JSON into pull events" should "decode `existed` events" in {
    val pullEvent = decodePullEvent("""
        |{ "status": "Already exists", "progressDetail": {}, "id": "6fba9447437b" }
      """.stripMargin)
    pullEvent should be(PullEvent.Layer.Existed("6fba9447437b"))
  }

  "Decoding JSON into pull events" should "decode `downloading` events" in {
    val pullEvent =
      decodePullEvent("""
        |{
        |  "status": "Downloading",
        |  "progressDetail":{
        |    "current": 490234,
        |    "total": 852890
        |  },
        |  "progress": "[============================\u003e                      ]  490.2kB/852.9kB",
        |  "id": "4afad9c4aba6"
        |}
      """.stripMargin)
    pullEvent should be(PullEvent.Layer.Downloading("4afad9c4aba6", 490234, 852890))
  }

  "Decoding JSON into pull events" should "decode `verifying` events" in {
    val pullEvent =
      decodePullEvent("""
          |{ "status": "Verifying Checksum", "progressDetail": {}, "id": "4afad9c4aba6" }
        """.stripMargin)
    pullEvent should be(PullEvent.Layer.Verifying("4afad9c4aba6"))
  }

  "Decoding JSON into pull events" should "decode `downloaded` events" in {
    val pullEvent = decodePullEvent("""
        |{ "status": "Download complete", "progressDetail": {}, "id": "4afad9c4aba6" }
      """.stripMargin)
    pullEvent should be(PullEvent.Layer.Downloaded("4afad9c4aba6"))
  }

  "Decoding JSON into pull events" should "decode `extracting` events" in {
    val pullEvent =
      decodePullEvent("""
        |{
        |  "status": "Extracting",
        |  "progressDetail":{
        |    "current": 103055360,
        |    "total": 122122910
        |  },
        |  "progress": "[==========================================\u003e        ]  103.1MB/122.1MB",
        |  "id": "4c2afc6ff72d"
        |}
      """.stripMargin)
    pullEvent should be(PullEvent.Layer.Extracting("4c2afc6ff72d", 103055360, 122122910))
  }

  "Decoding JSON into pull events" should "decode `pulled` events" in {
    val pullEvent = decodePullEvent("""
        |{ "status": "Pull complete", "progressDetail": {}, "id": "0dd81083d77e" }
      """.stripMargin)
    pullEvent should be(PullEvent.Layer.Pulled("0dd81083d77e"))
  }

  "Decoding JSON into pull events" should "decode `retrying` events" in {
    var pullEvent = decodePullEvent("""
        {"status":"Retrying in 5 seconds","progressDetail":{},"id":"f05cd8a9cc4b"}""")
    pullEvent should be(PullEvent.Layer.Retrying("f05cd8a9cc4b", 5.seconds))

    pullEvent = decodePullEvent("""
        {"status":"Retrying in 2 ms","progressDetail":{},"id":"f05cd8a9cc4b"}""")
    pullEvent should be(PullEvent.Layer.Retrying("f05cd8a9cc4b", 2.milliseconds))

    pullEvent = decodePullEvent("""
        {"status":"Retrying in 3 minutes","progressDetail":{},"id":"f05cd8a9cc4b"}""")
    pullEvent should be(PullEvent.Layer.Retrying("f05cd8a9cc4b", 3.minutes))
  }

  "Decoding JSON into pull events" should "decode `digest` events" in {
    val pullEvent =
      decodePullEvent("""
        |{ "status": "Digest: sha256:36bf904c5a2ce97da398cefacf725a870c388c022ba07ef64d1cd516870e49a8" }
      """.stripMargin)
    pullEvent should be(PullEvent.Digest("sha256:36bf904c5a2ce97da398cefacf725a870c388c022ba07ef64d1cd516870e49a8"))
  }

  "Decoding JSON into pull events" should "decode `status` events" in {
    val pullEvent =
      decodePullEvent("""
          |{ "status": "Status: Downloaded newer image for jetty:latest" }
        """.stripMargin)
    pullEvent should be(PullEvent.Status("Downloaded newer image for jetty:latest"))
  }

  "Decoding JSON into pull events" should "cause errors if it's an unknown status" in {
    an[Exception] should be thrownBy decodePullEvent(
      """
        |{ "status": "Something else", "progressDetail": {}, "id": "6fba9447437b" }
      """.stripMargin)
  }

  private def decodePullEvent(str: String): PullEvent = decode(str, PullEvent.decoder)

}
