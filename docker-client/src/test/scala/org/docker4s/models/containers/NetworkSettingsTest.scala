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
import org.docker4s.models.networks.{Endpoint, Network}

class NetworkSettingsTest extends ModelsSpec {

  "Decoding JSON into network settings" should "work" in {
    val networkSettings =
      decodeNetworkSettings("""{
        |	"Bridge":"",
        |	"SandboxID":"8690cbe010b702c1150999f1a90000722a0f1cc4d250123a8e3d3acc2c9c8a91",
        |	"HairpinMode":false,
        |	"LinkLocalIPv6Address":"",
        |	"LinkLocalIPv6PrefixLen":0,
        |	"Ports":{
        |		"8888/tcp":[{
        |			"HostIp":"0.0.0.0",
        |			"HostPort":"32768"
        |		}]
        |	},
        |	"SandboxKey":"/var/run/docker/netns/8690cbe010b7",
        |	"SecondaryIPAddresses":null,
        |	"SecondaryIPv6Addresses":null,
        |	"EndpointID":"e5f57721f7a1d2a067b9ae646a812a522f5215e8591416d62703688d648de34e",
        |	"Gateway":"172.17.0.1",
        |	"GlobalIPv6Address":"",
        |	"GlobalIPv6PrefixLen":0,
        |	"IPAddress":"172.17.0.2",
        |	"IPPrefixLen":16,
        |	"IPv6Gateway":"",
        |	"MacAddress":"02:42:ac:11:00:02",
        |	"Networks":{
        |		"bridge":{
        |			"IPAMConfig":null,
        |			"Links":null,
        |			"Aliases":null,
        |			"NetworkID":"5f4231b1b650acb2a247518e274deba52ca0875689a19e04826b228cac655691",
        |			"EndpointID":"e5f57721f7a1d2a067b9ae646a812a522f5215e8591416d62703688d648de34e",
        |			"Gateway":"172.17.0.1",
        |			"IPAddress":"172.17.0.2",
        |			"IPPrefixLen":16,
        |			"IPv6Gateway":"",
        |			"GlobalIPv6Address":"",
        |			"GlobalIPv6PrefixLen":0,
        |			"MacAddress":"02:42:ac:11:00:02",
        |			"DriverOpts":null
        |		}
        |	}
        |}""")
    networkSettings should be(
      NetworkSettings(
        ports = List(
          PortBinding(ipAddress = Some("0.0.0.0"), privatePort = 8888, publicPort = Some(32768), PortBinding.Type.TCP)
        ),
        networks = Map("bridge" -> Endpoint.Settings(
          ipamConfig = None,
          links = List.empty,
          aliases = List.empty,
          networkId = Network.Id("5f4231b1b650acb2a247518e274deba52ca0875689a19e04826b228cac655691"),
          endpointId = Endpoint.Id("e5f57721f7a1d2a067b9ae646a812a522f5215e8591416d62703688d648de34e"),
          gateway = "172.17.0.1",
          ipAddress = "172.17.0.2",
          ipPrefixLen = 16,
          ipv6Gateway = "",
          globalIpv6Address = "",
          globalIpv6PrefixLen = 0,
          macAddress = "02:42:ac:11:00:02",
          driverOpts = Map.empty
        ))
      ))
  }

  private def decodeNetworkSettings(str: String): NetworkSettings = decode(str, NetworkSettings.decoder)

}
