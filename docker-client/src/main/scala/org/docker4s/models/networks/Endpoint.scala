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

import io.circe.Decoder

case class Endpoint(name: String, id: Endpoint.Id, macAddress: String, ipv4Address: String, ipv6Address: String)

object Endpoint {

  /** Identifier for the service endpoint. */
  case class Id(value: String)

  case class IPAMConfig(ipv4Address: String, ipv6Address: String, linkLocalIps: List[String])

  /**
    * Configuration for a network endpoint.
    *
    * @param ipamConfig IPAM configuration
    * @param links
    * @param aliases
    * @param networkId Unique ID of the network
    * @param endpointId Unique ID for the service endpoint in a sandbox
    * @param gateway Gateway address for this network
    * @param ipAddress IPv4 address
    * @param ipPrefixLen Mask length of the IPv4 address
    * @param ipv6Gateway IPv6 gateway address
    * @param globalIpv6Address Global IPv6 address
    * @param globalIpv6PrefixLen Mask length of the global IPv6 address
    * @param macAddress MAC address for the endpoint on this network
    * @param driverOpts Mapping of driver options and values that are directly passed to the driver
    */
  case class Settings(
      ipamConfig: Option[IPAMConfig],
      links: List[String],
      aliases: List[String],
      networkId: Network.Id,
      endpointId: Endpoint.Id,
      gateway: String,
      ipAddress: String,
      ipPrefixLen: Int,
      ipv6Gateway: String,
      globalIpv6Address: String,
      globalIpv6PrefixLen: Int,
      macAddress: String,
      driverOpts: Map[String, String]
  )

  object Settings {

    private val ipamConfigDecoder: Decoder[IPAMConfig] = Decoder.instance({ c =>
      for {
        ipv4Address <- c.downField("IPv4Address").as[String].right
        ipv6Address <- c.downField("IPv6Address").as[String].right
        linkLocalIPs <- c.downField("LinkLocalIPs").as[List[String]].right
      } yield IPAMConfig(ipv4Address, ipv6Address, linkLocalIPs)
    })

    val decoder: Decoder[Settings] = Decoder.instance({ c =>
      for {
        ipamConfig <- c.downField("IPAMConfig").as(Decoder.decodeOption(ipamConfigDecoder)).right
        links <- c.downField("Links").as[Option[List[String]]].right
        aliases <- c.downField("Aliases").as[Option[List[String]]].right
        networkId <- c.downField("NetworkID").as[String].right
        endpointId <- c.downField("EndpointID").as[String].right
        gateway <- c.downField("Gateway").as[String].right
        ipAddress <- c.downField("IPAddress").as[String].right
        ipPrefixLen <- c.downField("IPPrefixLen").as[Int].right
        ipv6Gateway <- c.downField("IPv6Gateway").as[String].right
        globalIpv6Address <- c.downField("GlobalIPv6Address").as[String].right
        globalIpv6PrefixLen <- c.downField("GlobalIPv6PrefixLen").as[Int].right
        macAddress <- c.downField("MacAddress").as[String].right
        driverOpts <- c.downField("DriverOpts").as[Option[Map[String, String]]].right
      } yield
        Settings(
          ipamConfig = ipamConfig,
          links = links.getOrElse(List.empty),
          aliases = aliases.getOrElse(List.empty),
          networkId = Network.Id(networkId),
          endpointId = Endpoint.Id(endpointId),
          gateway = gateway,
          ipAddress = ipAddress,
          ipPrefixLen = ipPrefixLen,
          ipv6Gateway = ipv6Gateway,
          globalIpv6Address = globalIpv6Address,
          globalIpv6PrefixLen = globalIpv6PrefixLen,
          macAddress = macAddress,
          driverOpts = driverOpts.getOrElse(Map.empty)
        )
    })

  }

  val decoder: Decoder[Endpoint] = Decoder.instance({ c =>
    for {
      name <- c.downField("Name").as[String].right
      id <- c.downField("EndpointID").as[String].right
      macAddress <- c.downField("MacAddress").as[String].right
      ipv4Address <- c.downField("IPv4Address").as[String].right
      ipv6Address <- c.downField("IPv6Address").as[String].right
    } yield Endpoint(name, Endpoint.Id(id), macAddress, ipv4Address, ipv6Address)
  })

}
