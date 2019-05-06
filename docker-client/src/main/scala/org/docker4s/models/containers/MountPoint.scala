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

/**
  * Represents a mount point configuration inside a container.
  */
case class MountPoint(
    `type`: MountPoint.Type,
    name: Option[String],
    source: String,
    destination: String,
    driver: Option[String],
    mode: String,
    rw: Boolean,
    propagation: Option[MountPoint.Propagation])

object MountPoint {

  sealed trait Type

  object Type {
    case object Bind extends Type
    case object Tmpfs extends Type
    case object Volume extends Type
    case object Npipe extends Type
  }

  sealed trait Propagation

  object Propagation {
    case object RPrivate extends Propagation
    case object Private extends Propagation
    case object RShared extends Propagation
    case object Shared extends Propagation
    case object RSlave extends Propagation
    case object Slave extends Propagation
  }

  // -------------------------------------------- Circe decoders

  private val typeDecoder: Decoder[Type] = Decoder.decodeString.emap({
    case "bind"   => Right(Type.Bind)
    case "tmpfs"  => Right(Type.Tmpfs)
    case "volume" => Right(Type.Volume)
    case "npipe"  => Right(Type.Npipe)

    case str => Left(s"Cannot decode $str as a mount type.")
  })

  private val propagationDecoder: Decoder[Option[Propagation]] = Decoder
    .decodeOption(Decoder.decodeString)
    .emap({
      case Some("rprivate") => Right(Some(Propagation.RPrivate))
      case Some("private")  => Right(Some(Propagation.Private))
      case Some("rshared")  => Right(Some(Propagation.RShared))
      case Some("shared")   => Right(Some(Propagation.Shared))
      case Some("rslave")   => Right(Some(Propagation.RSlave))
      case Some("slave")    => Right(Some(Propagation.Slave))

      case Some("") => Right(None)
      case None     => Right(None)

      case Some(str) => Left(s"Cannot decode $str as a mount propagation type.")
    })

  val decoder: Decoder[MountPoint] = Decoder.instance({ c =>
    for {
      tpe <- c.downField("Type").as(typeDecoder).right
      name <- c.downField("Name").as[Option[String]].right
      source <- c.downField("Source").as[String].right
      destination <- c.downField("Destination").as[String].right
      driver <- c.downField("Driver").as[Option[String]].right
      mode <- c.downField("Mode").as[String].right
      rw <- c.downField("RW").as[Boolean].right
      propagation <- c.downField("Propagation").as(propagationDecoder).right
    } yield MountPoint(tpe, name, source, destination, driver, mode, rw, propagation)
  })

}
