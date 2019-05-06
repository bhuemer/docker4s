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
package org.docker4s.util

import io.circe.Json
import org.scalatest.{FlatSpec, Matchers}

class JsonUtilsTest extends FlatSpec with Matchers {

  "Merging JSON objects" should "union arrays" in {
    val merged = JsonUtils.merge(
      lhs = Json.obj(
        "HostConfig" -> Json.obj(
          "PortBindings" -> Json.obj(
            "5678/tcp" -> Json.arr(
              Json.obj("HostIp" -> Json.fromString(""), "HostPort" -> Json.fromString("1234"))
            )
          )
        )),
      rhs = Json.obj(
        "HostConfig" -> Json.obj(
          "PortBindings" -> Json.obj(
            "5678/tcp" -> Json.arr(
              Json.obj("HostIp" -> Json.fromString(""), "HostPort" -> Json.fromString("1235"))
            )
          )
        ))
    )

    merged should be(
      Json.obj(
        "HostConfig" -> Json.obj(
          "PortBindings" -> Json.obj(
            "5678/tcp" -> Json.arr(
              Json.obj("HostIp" -> Json.fromString(""), "HostPort" -> Json.fromString("1234")),
              Json.obj("HostIp" -> Json.fromString(""), "HostPort" -> Json.fromString("1235"))
            )
          )
        ))
    )
  }

}
