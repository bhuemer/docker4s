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
package org.docker4s.models.plugins

import io.circe.Decoder

case class Plugin(id: Plugin.Id, name: String, enabled: Boolean)

object Plugin {

  case class Id(value: String)

  case class Settings(args: List[String], devices: List[Device], env: List[String], mounts: List[Mount])

  case class Config(dockerVersion: String, description: String, documentation: String)

  case class Device(description: String, name: String, path: String, settable: List[String])

  case class Mount(
      description: String,
      destination: String,
      name: String,
      options: List[String],
      settable: List[String],
      source: String,
      `type`: String)

  // -------------------------------------------- Circe decoders

  private val deviceDecoder: Decoder[Device] = Decoder.instance({ c =>
    for {
      description <- c.downField("Description").as[String].right
      name <- c.downField("Name").as[String].right
      path <- c.downField("Path").as[String].right
      settable <- c.downField("Settable").as[List[String]].right
    } yield Device(description, name, path, settable)
  })

  private val mountDecoder: Decoder[Mount] = Decoder.instance({ c =>
    for {
      description <- c.downField("Description").as[String].right
      destination <- c.downField("Destination").as[String].right
      name <- c.downField("Name").as[String].right
      options <- c.downField("Options").as[List[String]].right
      settable <- c.downField("Settable").as[List[String]].right
      source <- c.downField("Source").as[String].right
      tpe <- c.downField("Type").as[String].right
    } yield Mount(description, destination, name, options, settable, source, tpe)
  })

  private val settingsDecoder: Decoder[Settings] = Decoder.instance({ c =>
    for {
      args <- c.downField("Args").as[List[String]].right
      devices <- c.downField("Devices").as(Decoder.decodeList(deviceDecoder)).right
      env <- c.downField("Env").as[List[String]].right
      mounts <- c.downField("Mounts").as(Decoder.decodeList(mountDecoder)).right
    } yield Settings(args, devices, env, mounts)
  })

  val decoder: Decoder[Plugin] = Decoder.instance({ c =>
    ???
  })

}
