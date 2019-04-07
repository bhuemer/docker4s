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
package org.docker4s.models.secrets

import java.time.ZonedDateTime

import io.circe.Decoder

case class Secret(id: Secret.Id, version: Long, createdAt: ZonedDateTime, updatedAt: ZonedDateTime, spec: SecretSpec)

object Secret {

  case class Id(value: String)

  // -------------------------------------------- Circe decoders

  val decoder: Decoder[Secret] = Decoder.instance({ c =>
    for {
      id <- c.downField("ID").as[String].right
      version <- c.downField("Version").downField("Index").as[Long].right
      createdAt <- c.downField("CreatedAt").as[ZonedDateTime].right
      updatedAt <- c.downField("UpdatedAt").as[ZonedDateTime].right
      spec <- c.downField("Spec").as(SecretSpec.decoder).right
    } yield Secret(Id(id), version, createdAt, updatedAt, spec)
  })

}
