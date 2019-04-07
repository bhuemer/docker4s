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
package org.docker4s.models.secrets

import java.time.ZonedDateTime

import org.scalatest.{FlatSpec, Matchers}

class SecretTest extends FlatSpec with Matchers {

  "Decoding JSON into secrets" should "work" in {
    val secret = decodeSecret("""{
      |  "ID": "yh8uy6o2e2fxjh7za35fcikxs",
      |  "Spec": {
      |    "Labels": {},
      |    "Name": "db_pass"
      |  },
      |  "CreatedAt": "2019-04-07T07:56:58.1931832Z",
      |  "UpdatedAt": "2019-04-07T07:56:58.1931832Z",
      |  "Version": {
      |    "Index": 55
      |  }
      |}""".stripMargin)
    secret should be(
      Secret(
        id = Secret.Id("yh8uy6o2e2fxjh7za35fcikxs"),
        version = 55,
        createdAt = ZonedDateTime.parse("2019-04-07T07:56:58.1931832Z"),
        updatedAt = ZonedDateTime.parse("2019-04-07T07:56:58.1931832Z"),
        spec = SecretSpec(name = "db_pass", labels = Map.empty, driver = None)
      )
    )
  }

  // -------------------------------------------- Utility methods

  /** Decodes the given string as a [[Secret]] or throws an exception if something goes wrong. */
  private def decodeSecret(str: String): Secret = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(Secret.decoder).fold(throw _, Predef.identity)
  }

}
