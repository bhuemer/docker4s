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
package org.docker4s.models.images

import java.time.ZonedDateTime

import io.circe.Decoder

/**
  * Detailed information about a particular image, as returned by e.g. `/images/{name}/json`.
  */
case class Image(
    id: Image.Id,
    parentId: Image.Id,
    createdAt: ZonedDateTime,
    comment: String,
    container: String,
    author: String,
    dockerVersion: String,
    architecture: String,
    os: String,
    osVersion: Option[String],
    size: Long,
    virtualSize: Long)

object Image {

  case class Id(value: String) {

    /**
      * Returns the ID of the image truncated to 10 characters, plus the `sha256:` prefix.
      */
    def shortId: String = {
      if (value.startsWith("sha256:")) {
        value.substring(0, "sha256:".length + 10)
      } else {
        value.substring(0, 10)
      }
    }

  }

  // -------------------------------------------- Circe decoders

  val decoder: Decoder[Image] = Decoder.instance({ c =>
    for {
      id <- c.downField("Id").as[String].right
      parentId <- c.downField("Parent").as[String].right
      created <- c.downField("Created").as[String].right
      comment <- c.downField("Comment").as[String].right
      container <- c.downField("Container").as[String].right
      author <- c.downField("Author").as[String].right
      dockerVersion <- c.downField("DockerVersion").as[String].right
      architecture <- c.downField("Architecture").as[String].right
      os <- c.downField("Os").as[String].right
      osVersion <- c.downField("OsVersion").as[Option[String]].right
      size <- c.downField("Size").as[Long].right
      virtualSize <- c.downField("VirtualSize").as[Long].right
    } yield
      Image(
        id = Image.Id(id),
        parentId = Image.Id(parentId),
        createdAt = ZonedDateTime.parse(created),
        comment = comment,
        container = container,
        author = author,
        dockerVersion = dockerVersion,
        architecture = architecture,
        os = os,
        osVersion = osVersion,
        size = size,
        virtualSize = virtualSize
      )
  })

}
