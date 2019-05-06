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

import io.circe.Decoder

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
