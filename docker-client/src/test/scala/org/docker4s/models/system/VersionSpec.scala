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
package org.docker4s.models.system

import java.time.ZonedDateTime

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

/**
  * Contains test cases related to parsing [[Version]] objects from JSON response bodies.
  */
@RunWith(classOf[JUnitRunner])
class VersionSpec extends FlatSpec with Matchers {

  "Decoding a version" should "parse all the essential pieces of information" in {
    val version = decodeVersion("""{
        |  "ApiVersion": "1.39",
        |  "Arch": "amd64",
        |  "BuildTime": "2019-01-09T19:41:49.000000000+00:00",
        |  "Components": [{
        |    "Details": {
        |      "ApiVersion": "1.39",
        |      "Arch": "amd64",
        |      "BuildTime": "2019-01-09T19:41:49.000000000+00:00",
        |      "Experimental": "false",
        |      "GitCommit": "4c52b90",
        |      "GoVersion": "go1.10.6",
        |      "KernelVersion": "4.9.125-linuxkit",
        |      "MinAPIVersion": "1.12",
        |      "Os": "linux"
        |    },
        |    "Name": "Engine",
        |    "Version": "18.09.1"
        |  }],
        |  "GitCommit": "4c52b90",
        |  "GoVersion": "go1.10.6",
        |  "KernelVersion": "4.9.125-linuxkit",
        |  "MinAPIVersion": "1.12",
        |  "Os": "linux",
        |  "Platform": {
        |    "Name": "Docker Engine - Community"
        |  },
        |  "Version": "18.09.1"
        |}""".stripMargin)

    version should be(
      Version(
        version = "18.09.1",
        apiVersion = "1.39",
        minApiVersion = Some("1.12"),
        gitCommit = "4c52b90",
        goVersion = "go1.10.6",
        os = "linux",
        arch = "amd64",
        kernelVersion = "4.9.125-linuxkit",
        buildTime = ZonedDateTime.parse("2019-01-09T19:41:49.000000000+00:00")
      )
    )
  }

  // -------------------------------------------- Utility methods

  /** Decodes the given string as a [[Version]] or throws an exception if something goes wrong. */
  private def decodeVersion(str: String): Version = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(Version.decoder).fold(throw _, Predef.identity)
  }

}
