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

import io.circe.{Decoder, KeyDecoder}

case class PortBinding(
    ipAddress: Option[String] = None,
    privatePort: Int,
    publicPort: Option[Int] = None,
    `type`: PortBinding.Type = PortBinding.Type.TCP)

object PortBinding {

  sealed abstract class Type(val name: String)

  object Type {

    case object TCP extends Type("tcp")
    case object UDP extends Type("udp")

    /** Stream control transmission protocol - widely used as a transport protocol for cellular networks. */
    case object SCTP extends Type("sctp")

  }

  // -------------------------------------------- Circe decoders

  private val typeDecoder: Decoder[Type] = Decoder.decodeString.emap({
    case "tcp"  => Right(Type.TCP)
    case "udp"  => Right(Type.UDP)
    case "sctp" => Right(Type.SCTP)
    case str    => Left(s"Cannot decode $str as a port binding type.")
  })

  val decoder: Decoder[PortBinding] = Decoder.instance({ c =>
    for {
      ip <- c.downField("IP").as[Option[String]].right
      privatePort <- c.downField("PrivatePort").as[Int].right
      publicPort <- c.downField("PublicPort").as[Option[Int]].right
      tpe <- c.downField("Type").as(typeDecoder).right
    } yield PortBinding(ip, privatePort, publicPort, tpe)
  })

}

object PortBindings {

  /**
    * Port bindings can also be encoded as JSON maps/objects using the container's port and protocol as key, e.g.
    * {{{
    * {
    *   "80/udp": [
    *     {
    *       "HostIp": "",
    *       "HostPort": ""
    *   ]
    * }
    * }}}
    *
    * This decoder takes care of those cases and returns them in a flattened format.
    */
  val decoder: Decoder[List[PortBinding]] = {
    type HostIpAndPort = (Option[String], Option[Int])

    val hostIpAndPortDecoder: Decoder[HostIpAndPort] = Decoder.instance({ c =>
      for {
        hostIp <- c.downField("HostIp").as[Option[String]].right
        hostPort <- c
          .downField("HostPort")
          .as(
            Decoder
              .decodeOption(Decoder.decodeString)
              .emap[Option[Int]]({
                case None     => Right(None)
                case Some("") => Right(None)
                case Some(port) =>
                  try {
                    Right(Some(port.toInt))
                  } catch {
                    case scala.util.control.NonFatal(ex) =>
                      Left(s"Cannot decode '$port' as a port. It is not a valid integer [${ex.getMessage}].")
                  }
              }))
          .right
      } yield (hostIp.filter(_.nonEmpty), hostPort)
    })

    type ContainerPortAndType = (Int, PortBinding.Type)
    def decodeBindingKey(key: String): Either[String, ContainerPortAndType] = {
      def safeParse(port: String, `type`: PortBinding.Type): Either[String, ContainerPortAndType] = {
        try {
          Right((port.toInt, `type`))
        } catch {
          case scala.util.control.NonFatal(ex) =>
            Left(s"Cannot decode '$port' as a port. It is not a valid integer [${ex.getMessage}].")
        }
      }

      key.split("/") match {
        case Array(port, "tcp")  => safeParse(port, PortBinding.Type.TCP)
        case Array(port, "udp")  => safeParse(port, PortBinding.Type.UDP)
        case Array(port, "sctp") => safeParse(port, PortBinding.Type.SCTP)

        case _ => Left(s"Cannot decode '$key' as a port binding key. It is not in the format `<port>/<protocol>`.")
      }
    }

    Decoder
      .decodeMap(
        KeyDecoder.decodeKeyString,
        Decoder
          .decodeOption(
            Decoder.decodeList(hostIpAndPortDecoder)
          )
          .map(_.getOrElse(List.empty))
      )
      // At this point we now get a map with these container binding keys (e.g. "80/udp") pointing to
      // a list of all the host configurations. Note that there can be many, reflecting something like
      // "0.0.0.0:32770->8888/tcp, 0.0.0.0:32771->8888/tcp", for example.
      .emap({ bindings =>
        bindings
          .foldLeft[Either[String, Map[ContainerPortAndType, List[HostIpAndPort]]]](Right(Map.empty))({
            case (Left(error), _) => Left(error)
            case (Right(acc), (bindingKey, configs)) =>
              for {
                portAndType <- decodeBindingKey(bindingKey).right
              } yield acc + (portAndType -> configs)
          })
      })
      // Flatten the structure into something that is easier to work with
      .map({ bindings =>
        for {
          ((privatePort, tpe), configs) <- bindings.toList
          (hostIp, hostPort) <- if (configs.isEmpty) {
            // If no configuration is available at all, then at least expose the binding we have
            // configured with no host configuration for it. This basically indicates a port that
            // has been exposed in the container but not in the host, i.e.
            // "8888/tcp" vs "0.0.0.0:8888->8888/tcp", for example.
            List((None, None))
          } else configs
        } yield PortBinding(hostIp, privatePort, hostPort, tpe)
      })
  }

}
