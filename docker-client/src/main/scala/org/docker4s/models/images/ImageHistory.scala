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
package org.docker4s.models.images
import java.time.{Instant, ZoneId, ZonedDateTime}

import io.circe.Decoder

case class ImageHistory(
    id: ImageHistory.Id,
    createdAt: ZonedDateTime,
    createdBy: String,
    tags: List[String],
    size: Long,
    comment: Option[String])

object ImageHistory {

  case class Id(value: String)

  // -------------------------------------------- Circe decoders

  val decoder: Decoder[ImageHistory] = Decoder.instance({ c =>
    for {
      id <- c.downField("Id").as[String].right
      createdAt <- c.downField("Created").as[Long].right
      createdBy <- c.downField("CreatedBy").as[String].right
      tags <- c.downField("Tags").as[Option[List[String]]].right
      size <- c.downField("Size").as[Long].right
      comment <- c.downField("Comment").as[Option[String]].right
    } yield
      ImageHistory(
        id = ImageHistory.Id(id),
        createdAt = Instant.ofEpochSecond(createdAt).atZone(ZoneId.of("Z")),
        createdBy = createdBy,
        tags = tags.getOrElse(List.empty),
        size = size,
        comment = comment.filter(_.nonEmpty)
      )
  })

}
