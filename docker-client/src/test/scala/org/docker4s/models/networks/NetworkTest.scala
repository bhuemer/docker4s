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
package org.docker4s.models.networks

import java.time.ZonedDateTime

import org.scalatest.{FlatSpec, Matchers}

class NetworkTest extends FlatSpec with Matchers {

  "Decoding JSON into networks" should "work" in {
    val network = decodeNetwork("""{
        |  "Name": "ingress",
        |  "Id": "kjq7p8j3jk731l7cq0z87lvex",
        |  "Created": "2019-04-07T09:02:35.6838279Z",
        |  "Scope": "swarm",
        |  "Driver": "overlay",
        |  "EnableIPv6": false,
        |  "IPAM": {
        |    "Driver": "default",
        |    "Options": null,
        |    "Config": [{
        |      "Subnet": "10.255.0.0/16",
        |      "Gateway": "10.255.0.1"
        |    }]
        |  },
        |  "Internal": false,
        |  "Attachable": false,
        |  "Ingress": true,
        |  "ConfigFrom": { "Network": "" },
        |  "ConfigOnly": false,
        |  "Containers": null,
        |  "Options": {
        |    "com.docker.network.driver.overlay.vxlanid_list": "4096"
        |  },
        |  "Labels": null
        |}""".stripMargin)
    network should be(
      Network(
        id = Network.Id("kjq7p8j3jk731l7cq0z87lvex"),
        name = "ingress",
        createdAt = ZonedDateTime.parse("2019-04-07T09:02:35.683827900Z"),
        scope = Network.Scope.Swarm,
        driver = "overlay",
        enableIPv6 = false,
        ipam = Network.IPAM(
          driver = "default",
          options = Map.empty,
          configs = List(
            Network.IPAM.Config(subnet = Some("10.255.0.0/16"), ipRange = None, gateway = Some("10.255.0.1"))
          )
        ),
        internal = false,
        attachable = false,
        ingress = true,
        configOnly = false,
        containers = Map.empty,
        options = Map(
          "com.docker.network.driver.overlay.vxlanid_list" -> "4096"
        ),
        labels = Map()
      ))
  }

  "Decoding JSON into networks" should "decode container endpoints as well" in {
    val network = decodeNetwork("""{
      |  "Name": "docker_gwbridge",
      |  "Id": "286621d9106d66b492ddbd0c22e06574076e2efa8e7c2e6ee3726c04b5d02b7d",
      |  "Created": "2019-03-12T22:31:50.4414207Z",
      |  "Scope": "local",
      |  "Driver": "bridge",
      |  "EnableIPv6": false,
      |  "IPAM": {
      |    "Driver": "default",
      |    "Options": null,
      |    "Config": [
      |      {
      |        "Subnet": "172.18.0.0/16",
      |        "Gateway": "172.18.0.1"
      |      }
      |    ]
      |  },
      |  "Internal": false,
      |  "Attachable": false,
      |  "Ingress": false,
      |  "ConfigFrom": {
      |    "Network": ""
      |  },
      |  "ConfigOnly": false,
      |  "Containers": {
      |    "ingress-sbox": {
      |      "Name": "gateway_ingress-sbox",
      |      "EndpointID": "13dbdf154f085d974e71d9f548744bad356dc491186366895f06d39da48cbdb6",
      |      "MacAddress": "02:42:ac:12:00:02",
      |      "IPv4Address": "172.18.0.2/16",
      |      "IPv6Address": ""
      |    }
      |  },
      |  "Options": {
      |    "com.docker.network.bridge.enable_icc": "false",
      |    "com.docker.network.bridge.enable_ip_masquerade": "true",
      |    "com.docker.network.bridge.name": "docker_gwbridge"
      |  },
      |  "Labels": {}
      |}""".stripMargin)
    network should be(
      Network(
        id = Network.Id("286621d9106d66b492ddbd0c22e06574076e2efa8e7c2e6ee3726c04b5d02b7d"),
        name = "docker_gwbridge",
        createdAt = ZonedDateTime.parse("2019-03-12T22:31:50.4414207Z"),
        scope = Network.Scope.Local,
        driver = "bridge",
        enableIPv6 = false,
        ipam = Network.IPAM(
          driver = "default",
          options = Map.empty,
          configs = List(
            Network.IPAM.Config(subnet = Some("172.18.0.0/16"), ipRange = None, gateway = Some("172.18.0.1"))
          )
        ),
        internal = false,
        attachable = false,
        ingress = false,
        configOnly = false,
        containers = Map(
          "ingress-sbox" -> Network.Endpoint(
            id = Network.Endpoint.Id("13dbdf154f085d974e71d9f548744bad356dc491186366895f06d39da48cbdb6"),
            name = "gateway_ingress-sbox",
            macAddress = "02:42:ac:12:00:02",
            ipv4Address = "172.18.0.2/16",
            ipv6Address = ""
          )
        ),
        options = Map(
          "com.docker.network.bridge.enable_icc" -> "false",
          "com.docker.network.bridge.enable_ip_masquerade" -> "true",
          "com.docker.network.bridge.name" -> "docker_gwbridge"
        ),
        labels = Map.empty
      ))
  }

  // -------------------------------------------- Utility methods

  /** Decodes the given string as a [[Network]] or throws an exception if something goes wrong. */
  private def decodeNetwork(str: String): Network = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(Network.decoder).fold(throw _, Predef.identity)
  }

}
