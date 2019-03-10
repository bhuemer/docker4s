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

case class Image(
    id: String,
    parentId: String,
    createdAt: ZonedDateTime,
    containers: Int,
    labels: Map[String, String],
    repoTags: List[String],
    repoDigests: List[String]) {

  /**
    * Returns the maintainer of this image, if one is known.
    */
  def maintainer: Option[String] = labels.get("maintainer")

  /**
    * Returns the ID of the image truncated to 10 characters, plus the `sha256:` prefix.
    */
  def shortId: String = {
    if (id.startsWith("sha256:")) {
      id.substring(0, "sha256:".length + 10)
    } else {
      id.substring(0, 10)
    }
  }

}

object Image {

  // -------------------------------------------- Circe decoders

  val decoder: Decoder[Image] = Decoder.instance({ c =>
    for {
      containers <- c.downField("Containers").as[Int].right
      created <- c.downField("Created").as[Long].right
      id <- c.downField("Id").as[String].right
      labels <- c.downField("Labels").as[Option[Map[String, String]]].right
      parentId <- c.downField("ParentId").as[String].right
      repoDigests <- c.downField("RepoDigests").as[Option[List[String]]].right
      repoTags <- c.downField("RepoTags").as[Option[List[String]]].right
    } yield
      Image(
        id = id,
        parentId = parentId,
        createdAt = Instant.ofEpochSecond(created).atZone(ZoneId.of("Z")),
        containers = containers,
        labels = labels.getOrElse(Map()),
        // automatically remove tags and digests that aren't valid as such
        repoTags = repoTags.getOrElse(List()).filter(!"<none>:<none>".equalsIgnoreCase(_)),
        repoDigests = repoDigests.getOrElse(List()).filter(!"<none>@<none>".equalsIgnoreCase(_))
      )
  })

}
