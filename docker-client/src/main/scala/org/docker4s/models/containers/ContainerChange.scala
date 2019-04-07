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
package org.docker4s.models.containers

import io.circe.Decoder

case class ContainerChange(path: String, kind: ContainerChange.Kind)

object ContainerChange {

  sealed trait Kind

  object Kind {
    case object Added extends Kind
    case object Deleted extends Kind
    case object Modified extends Kind
  }

  // -------------------------------------------- Circe decoders

  private val kindDecoder: Decoder[Kind] = Decoder.decodeInt.emap({
    case 0 => Right(Kind.Modified)
    case 1 => Right(Kind.Added)
    case 2 => Right(Kind.Deleted)
    case i => Left(s"Cannot decode $i as a container change kind.")
  })

  val decoder: Decoder[ContainerChange] = Decoder.instance({ c =>
    for {
      path <- c.downField("Path").as[String].right
      kind <- c.downField("Kind").as(kindDecoder).right
    } yield ContainerChange(path, kind)
  })

}
