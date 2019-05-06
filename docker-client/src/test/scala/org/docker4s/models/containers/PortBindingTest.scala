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
package org.docker4s.models.containers

import org.docker4s.models.ModelsSpec

class PortBindingTest extends ModelsSpec {

  "Decoding JSON into port bindings" should "not require public ports" in {
    val portBinding = decodePortBinding("""{
      |  "PrivatePort" : 3306,
      |  "Type" : "tcp"
      |}""".stripMargin)
    portBinding should be(
      PortBinding(
        ipAddress = None,
        privatePort = 3306,
        publicPort = None,
        `type` = PortBinding.Type.TCP
      ))
  }

  "Decoding JSON into port bindings" should "cope with empty objects" in {
    decodePortBindings("""{}""") should be(List.empty)
  }

  "Decoding JSON into port bindings" should "cope with empty lists for host configs" in {
    decodePortBindings("""{
      |  "8888/tcp": []
      |}""".stripMargin) should be(
      List(
        PortBinding(ipAddress = None, privatePort = 8888, publicPort = None, PortBinding.Type.TCP)
      ))

    decodePortBindings("""{
      |  "8888/tcp": null
      |}""".stripMargin) should be(
      List(
        PortBinding(ipAddress = None, privatePort = 8888, publicPort = None, PortBinding.Type.TCP)
      ))
  }

  "Decoding JSON into port bindings" should "cope with empty host configs" in {
    decodePortBindings("""{
      |  "8888/tcp": [{}]
      |}""".stripMargin) should be(
      List(
        PortBinding(ipAddress = None, privatePort = 8888, publicPort = None, PortBinding.Type.TCP)
      ))

    decodePortBindings("""{
      |  "8888/tcp": [
      |    {
      |      "HostIp": "",
      |      "HostPort": ""
      |    }
      |  ]
      |}""".stripMargin) should be(
      List(
        PortBinding(ipAddress = None, privatePort = 8888, publicPort = None, PortBinding.Type.TCP)
      ))
  }

  "Decoding JSON into port bindings" should "work for several host ports bound to the same container port" in {
    val portBindings = decodePortBindings("""{
        |  "8888/tcp": [
        |    {
        |      "HostIp": "0.0.0.0",
        |      "HostPort": "32770"
        |    },
        |    {
        |      "HostIp": "0.0.0.0",
        |      "HostPort": "32771"
        |    }
        |  ],
        |  "8889/tcp": [
        |    {
        |      "HostIp": "0.0.0.0",
        |      "HostPort": "32772"
        |    }
        |  ]
        |}""".stripMargin)
    portBindings should be(
      List(
        PortBinding(ipAddress = Some("0.0.0.0"), privatePort = 8888, publicPort = Some(32770), PortBinding.Type.TCP),
        PortBinding(ipAddress = Some("0.0.0.0"), privatePort = 8888, publicPort = Some(32771), PortBinding.Type.TCP),
        PortBinding(ipAddress = Some("0.0.0.0"), privatePort = 8889, publicPort = Some(32772), PortBinding.Type.TCP)
      ))
  }

  "Decoding JSON into port bindings" should "work for same container ports with different types" in {
    val portBindings = decodePortBindings("""{
        |  "8888/tcp": [
        |   {
        |     "HostIp": "0.0.0.0",
        |     "HostPort": "32770"
        |   }
        |  ],
        |  "8888/udp": [
        |   {
        |     "HostIp": "0.0.0.0",
        |     "HostPort": "32771"
        |   }
        |  ]
        |}""".stripMargin)
    portBindings should be(
      List(
        PortBinding(ipAddress = Some("0.0.0.0"), privatePort = 8888, publicPort = Some(32770), PortBinding.Type.TCP),
        PortBinding(ipAddress = Some("0.0.0.0"), privatePort = 8888, publicPort = Some(32771), PortBinding.Type.UDP)
      )
    )
  }

  "Decoding JSON into port bindings" should "fail for invalid binding keys" in {
    val ex = the[Exception] thrownBy decodePortBindings("""{ "invalid-binding-key": [] }""")
    ex.getMessage should include("Cannot decode 'invalid-binding-key' as a port binding key.")
  }

  "Decoding JSON into port bindings" should "fail for invalid host config port numbers" in {
    val ex = the[Exception] thrownBy decodePortBindings("""{
        |  "8888/tcp": [
        |    {
        |      "HostIp": "0.0.0.0",
        |      "HostPort": "aa80"
        |    }
        |  ]
        |}""".stripMargin)
    ex.getMessage should include("Cannot decode 'aa80' as a port.")
  }

  private def decodePortBinding(str: String): PortBinding = decode(str, PortBinding.decoder)

  private def decodePortBindings(str: String): List[PortBinding] = decode(str, PortBindings.decoder)

}
