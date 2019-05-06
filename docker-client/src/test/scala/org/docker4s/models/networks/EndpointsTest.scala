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
package org.docker4s.models.networks

import org.docker4s.models.ModelsSpec

class EndpointsTest extends ModelsSpec {

  "Decoding JSON into endpoint settings" should "work" in {
    val endpointSettings =
      decodeEndpointSettings("""{
      |  "IPAMConfig": null,
      |  "Links": null,
      |  "Aliases": null,
      |  "NetworkID": "571ae7e7d45659f5189010692f7be150f0946ca727e1260254c1a4fc6769b9e7",
      |  "EndpointID": "baed16f3189af10849e4eab3d651e0fd652a09bfa8261e8808cf951f9123038f",
      |  "Gateway": "172.17.0.1",
      |  "IPAddress": "172.17.0.2",
      |  "IPPrefixLen": 16,
      |  "IPv6Gateway": "",
      |  "GlobalIPv6Address": "",
      |  "GlobalIPv6PrefixLen": 0,
      |  "MacAddress": "02:42:ac:11:00:02",
      |  "DriverOpts": null
      |}""".stripMargin)
    endpointSettings should be(
      Endpoint.Settings(
        ipamConfig = None,
        links = List.empty,
        aliases = List.empty,
        networkId = Network.Id("571ae7e7d45659f5189010692f7be150f0946ca727e1260254c1a4fc6769b9e7"),
        endpointId = Endpoint.Id("baed16f3189af10849e4eab3d651e0fd652a09bfa8261e8808cf951f9123038f"),
        gateway = "172.17.0.1",
        ipAddress = "172.17.0.2",
        ipPrefixLen = 16,
        ipv6Gateway = "",
        globalIpv6Address = "",
        globalIpv6PrefixLen = 0,
        macAddress = "02:42:ac:11:00:02",
        driverOpts = Map.empty
      ))
  }

  // -------------------------------------------- Utility methods

  private def decodeEndpointSettings(str: String): Endpoint.Settings = decode(str, Endpoint.Settings.decoder)

}
