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
package org.docker4s.models.secrets

import io.circe.Decoder

case class SecretSpec(name: String, labels: Map[String, String], driver: Option[SecretSpec.Driver])

object SecretSpec {

  case class Driver(name: String, options: Map[String, String])

  // -------------------------------------------- Circe decoders

  private val driverDecoder: Decoder[SecretSpec.Driver] = Decoder.instance({ c =>
    for {
      name <- c.downField("Name").as[String].right
      options <- c.downField("Options").as[Option[Map[String, String]]].right
    } yield Driver(name, options.getOrElse(Map.empty))
  })

  val decoder: Decoder[SecretSpec] = Decoder.instance({ c =>
    for {
      name <- c.downField("Name").as[String].right
      labels <- c.downField("Labels").as[Option[Map[String, String]]].right
      driver <- c.downField("Driver").as(Decoder.decodeOption(driverDecoder)).right
    } yield SecretSpec(name, labels.getOrElse(Map.empty), driver)
  })

}
