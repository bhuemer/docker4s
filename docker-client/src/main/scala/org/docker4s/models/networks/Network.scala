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

import io.circe.{Decoder, KeyDecoder}

case class Network(
    id: Network.Id,
    name: String,
    createdAt: ZonedDateTime,
    scope: Network.Scope,
    driver: String,
    enableIPv6: Boolean,
    ipam: Network.IPAM,
    internal: Boolean,
    attachable: Boolean,
    ingress: Option[Boolean],
    configOnly: Option[Boolean],
    containers: Map[String, Endpoint],
    options: Map[String, String],
    labels: Map[String, String])

object Network {

  case class Id(value: String)

  sealed trait Scope

  object Scope {
    case object Swarm extends Scope
    case object Local extends Scope
    case object Global extends Scope
  }

  case class IPAM(driver: String, options: Map[String, String], configs: List[IPAM.Config])

  object IPAM {
    case class Config(subnet: Option[String], ipRange: Option[String], gateway: Option[String])
  }

  // -------------------------------------------- Circe decoders

  private val scopeDecoder: Decoder[Scope] = Decoder.decodeString.emap({
    case "swarm"  => Right(Scope.Swarm)
    case "local"  => Right(Scope.Local)
    case "global" => Right(Scope.Global)
    case str      => Left(s"Cannot decode $str as a network scope.")
  })

  private val ipamConfigDecoder: Decoder[IPAM.Config] = Decoder.instance({ c =>
    for {
      subnet <- c.downField("Subnet").as[Option[String]].right
      ipRange <- c.downField("IPRange").as[Option[String]].right
      gateway <- c.downField("Gateway").as[Option[String]].right
    } yield IPAM.Config(subnet, ipRange, gateway)
  })

  private val ipamDecoder: Decoder[IPAM] = Decoder.instance({ c =>
    for {
      driver <- c.downField("Driver").as[String].right
      options <- c.downField("Options").as[Option[Map[String, String]]].right
      configs <- c.downField("Config").as(Decoder.decodeOption(Decoder.decodeList(ipamConfigDecoder))).right
    } yield IPAM(driver, options.getOrElse(Map.empty), configs.getOrElse(List.empty))
  })

  val decoder: Decoder[Network] = Decoder.instance({ c =>
    for {
      id <- c.downField("Id").as[String].right
      name <- c.downField("Name").as[String].right
      created <- c.downField("Created").as[ZonedDateTime].right
      scope <- c.downField("Scope").as(scopeDecoder).right
      driver <- c.downField("Driver").as[String].right
      enableIPv6 <- c.downField("EnableIPv6").as[Boolean].right
      ipam <- c.downField("IPAM").as(ipamDecoder).right
      internal <- c.downField("Internal").as[Boolean].right
      attachable <- c.downField("Attachable").as[Boolean].right
      ingress <- c.downField("Ingress").as[Option[Boolean]].right
      configOnly <- c.downField("ConfigOnly").as[Option[Boolean]].right
      containers <- c
        .downField("Containers")
        .as(Decoder.decodeOption(Decoder.decodeMap(KeyDecoder.decodeKeyString, Endpoint.decoder)))
      options <- c.downField("Options").as[Option[Map[String, String]]].right
      labels <- c.downField("Labels").as[Option[Map[String, String]]].right
    } yield
      Network(
        id = Id(id),
        name = name,
        createdAt = created,
        scope = scope,
        driver = driver,
        enableIPv6 = enableIPv6,
        ipam = ipam,
        internal = internal,
        attachable = attachable,
        ingress = ingress,
        configOnly = configOnly,
        containers = containers.getOrElse(Map.empty),
        options = options.getOrElse(Map.empty),
        labels = labels.getOrElse(Map.empty)
      )
  })

}
