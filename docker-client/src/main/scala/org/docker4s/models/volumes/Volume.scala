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
package org.docker4s.models.volumes

import java.time.ZonedDateTime

import io.circe.Decoder

case class Volume(
    name: String,
    driver: String,
    mountpoint: String,
    status: Map[String, String],
    labels: Map[String, String],
    options: Map[String, String],
    scope: Volume.Scope,
    createdAt: Option[ZonedDateTime])

object Volume {

  sealed trait Scope

  object Scope {

    /** For machine-level volumes */
    case object Local extends Scope

    /** For cluster-wide volumes */
    case object Global extends Scope
  }

  // -------------------------------------------- Circe decoders

  private val scopeDecoder: Decoder[Volume.Scope] = Decoder.decodeString.emap({
    case "local"  => Right(Scope.Local)
    case "global" => Right(Scope.Global)
    case str      => Left(s"Cannot decode $str as an event scope.")
  })

  val decoder: Decoder[Volume] = Decoder.instance({ c =>
    for {
      name <- c.downField("Name").as[String].right
      driver <- c.downField("Driver").as[String].right
      mountpoint <- c.downField("Mountpoint").as[String].right
      createdAt <- c.downField("CreatedAt").as[Option[ZonedDateTime]].right
      status <- c.downField("Status").as[Option[Map[String, String]]].right
      labels <- c.downField("Labels").as[Option[Map[String, String]]].right
      options <- c.downField("Options").as[Option[Map[String, String]]].right
      scope <- c.downField("Scope").as(scopeDecoder).right
    } yield
      Volume(
        name = name,
        driver = driver,
        mountpoint = mountpoint,
        status = status.getOrElse(Map.empty),
        labels = labels.getOrElse(Map.empty),
        options = options.getOrElse(Map.empty),
        scope = scope,
        createdAt = createdAt
      )
  })

}
