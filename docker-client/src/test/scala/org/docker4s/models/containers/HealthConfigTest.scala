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

import io.circe.syntax._
import org.docker4s.models.ModelsSpec

import scala.concurrent.duration._

class HealthConfigTest extends ModelsSpec {

  "Encoding health configs into JSON" should "encode inherited tests" in {
    val json = HealthConfig.inherited
      .withInterval(1.second)
      .withTimeout(1.second)
      .asJson
    json.noSpaces should be("""{"Test":[],"Interval":1000000000,"Timeout":1000000000,"Retries":0,"StartPeriod":0}""")
  }

  "Decoding JSON into health configs" should "decode inherited tests" in {
    val healthConfig = decodeHealthConfig("""{
      |  "Test": [ ],
      |  "Interval": 1000000000,
      |  "Timeout": 1000000000
      |}""".stripMargin)
    healthConfig should be(HealthConfig.inherited.withInterval(1.second).withTimeout(1.second))
  }

  "Encoding health configs into JSON" should "encode `NONE` tests" in {
    val json = HealthConfig.disabled
      .withInterval(1.second)
      .withTimeout(1.second)
      .asJson
    json.noSpaces should be(
      """{"Test":["NONE"],"Interval":1000000000,"Timeout":1000000000,"Retries":0,"StartPeriod":0}""")
  }

  "Decoding JSON into health configs" should "decode `NONE` tests" in {
    val healthConfig = decodeHealthConfig("""{
      |  "Test": [
      |    "NONE"
      |  ],
      |  "Interval": 1000000000,
      |  "Timeout": 1000000000
      |}""".stripMargin)
    healthConfig should be(HealthConfig.disabled.withInterval(1.second).withTimeout(1.second))
  }

  "Encoding health configs into JSON" should "encode `CMD` tests" in {
    val json = HealthConfig
      .cmd("curl", "-f", "http://localhost:5000")
      .withInterval(3.seconds)
      .withRetries(5)
      .asJson
    json.noSpaces should be(
      """{"Test":["CMD","curl","-f","http://localhost:5000"],"Interval":3000000000,"Timeout":0,"Retries":5,"StartPeriod":0}""")
  }

  "Decoding JSON into health configs" should "decode `CMD` tests" in {
    val healthConfig = decodeHealthConfig("""{
      |  "Test": [
      |    "CMD",
      |    "curl",
      |    "-f",
      |    "http://localhost:5000"
      |  ],
      |  "Interval": 3000000000,
      |  "Retries": 5
      |}""".stripMargin)
    healthConfig should be(
      HealthConfig
        .cmd("curl", "-f", "http://localhost:5000")
        .withInterval(3.seconds)
        .withRetries(5)
    )
  }

  "Encoding health configs into JSON" should "encode `CMD-SHELL` tests" in {
    val json = HealthConfig.cmdShell("stat /etc/passwd || exit 1").withInterval(2.seconds).asJson
    json.noSpaces should be(
      """{"Test":["CMD-SHELL","stat /etc/passwd || exit 1"],"Interval":2000000000,"Timeout":0,"Retries":0,"StartPeriod":0}""")
  }

  "Decoding JSON into health configs" should "decode `CMD-SHELL` tests" in {
    val healthConfig = decodeHealthConfig("""{
      |  "Test": [
      |    "CMD-SHELL",
      |    "stat /etc/passwd || exit 1"
      |  ],
      |  "Interval": 2000000000
      |}""".stripMargin)
    healthConfig should be(HealthConfig.cmdShell("stat /etc/passwd || exit 1").withInterval(2.seconds))
  }

  private def decodeHealthConfig(str: String): HealthConfig = decode(str, HealthConfig.decoder)

}
