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
package org.docker4s.models.containers

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

// @RunWith(classOf[JUnitRunner])
class ContainerSummarySpec extends FlatSpec with Matchers {

  "Decoding JSON into container summaries" should "successfully decode hello-world containers" in {
    val containerSummary =
      decodeContainerSummary("""{
      |  "Command": "/hello",
      |  "Created": 1552349820,
      |  "HostConfig": {
      |    "NetworkMode": "default"
      |  },
      |  "Id": "191bdb92cc525e317d8efd897e109aeb6439688fa477f057309e76387b4fb43a",
      |  "Image": "hello-world",
      |  "ImageID": "sha256:fce289e99eb9bca977dae136fbe2a82b6b7d4c372474c9235adc1741675f587e",
      |  "Labels": {},
      |  "Mounts": [],
      |  "Names": [
      |    "/fervent_chatelet"
      |  ],
      |  "NetworkSettings": {
      |    "Networks": {
      |      "bridge": {
      |        "Aliases": null,
      |        "DriverOpts": null,
      |        "EndpointID": "",
      |        "Gateway": "",
      |        "GlobalIPv6Address": "",
      |        "GlobalIPv6PrefixLen": 0,
      |        "IPAMConfig": null,
      |        "IPAddress": "",
      |        "IPPrefixLen": 0,
      |        "IPv6Gateway": "",
      |        "Links": null,
      |        "MacAddress": "",
      |        "NetworkID": "42d2ef1d4ca95d5e6b2c80c1f6dff08f905e1c0a6f94c2ba8bcbd50d9ea13bb9"
      |      }
      |    }
      |  },
      |  "Ports": [],
      |  "State": "exited",
      |  "Status": "Exited (0) 6 days ago"
      |}""".stripMargin)
  }

  // -------------------------------------------- Utility methods

  /** Decodes the given string as a [[ContainerSummary]] or throws an exception if something goes wrong. */
  private def decodeContainerSummary(str: String): ContainerSummary = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(ContainerSummary.decoder).fold(throw _, Predef.identity)
  }

}
