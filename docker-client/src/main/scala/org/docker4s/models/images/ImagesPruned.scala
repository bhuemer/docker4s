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

import io.circe.Decoder

/**
  * Information about images that were pruned / removed because they were unused.
  *
  * @see [[https://docs.docker.com/engine/reference/commandline/image_prune/ Docker CLI]]
  * @param images Images that were deleted or untagged
  * @param spaceReclaimed Disk space reclaimed in bytes
  */
case class ImagesPruned(images: List[ImagesPruned.Ref], spaceReclaimed: Long)

object ImagesPruned {

  sealed trait Ref

  object Ref {
    case class Deleted(id: Image.Id) extends Ref
    case class Untagged(name: String) extends Ref
  }

  // -------------------------------------------- Circe decoders

  implicit private val refDecoder: Decoder[ImagesPruned.Ref] = {
    def decode(key: String, f: String => ImagesPruned.Ref): Decoder[ImagesPruned.Ref] =
      Decoder.instance(_.downField(key).as[String].map(f))

    List(
      decode("Deleted", ref => ImagesPruned.Ref.Deleted(Image.Id(ref))),
      decode("Untagged", ref => ImagesPruned.Ref.Untagged(ref))
    ).reduceLeft(_.or(_))
  }

  val decoder: Decoder[ImagesPruned] = Decoder.instance({ c =>
    for {
      images <- c.downField("ImagesDeleted").as[Option[List[ImagesPruned.Ref]]].right
      spaceReclaimed <- c.downField("SpaceReclaimed").as[Long]
    } yield ImagesPruned(images.getOrElse(List.empty), spaceReclaimed)
  })

}
