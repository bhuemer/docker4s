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

import io.circe.Decoder

case class ImagesRemoved(images: List[ImagesRemoved.Ref])

object ImagesRemoved {

  sealed trait Ref

  object Ref {
    case class Deleted(id: Image.Id) extends Ref
    case class Untagged(name: String) extends Ref
  }

  // -------------------------------------------- Circe decoders

  private[docker4s] val refDecoder: Decoder[ImagesRemoved.Ref] = {
    def decode(key: String, f: String => ImagesRemoved.Ref): Decoder[Ref] =
      Decoder.instance(_.downField(key).as[String].map(f))

    List(
      decode("Deleted", ref => ImagesRemoved.Ref.Deleted(Image.Id(ref))),
      decode("Untagged", ref => ImagesRemoved.Ref.Untagged(ref))
    ).reduceLeft(_.or(_))
  }

  val decoder: Decoder[ImagesRemoved] =
    Decoder
      .decodeOption(Decoder.decodeList(refDecoder))
      .map(_.getOrElse(List.empty))
      .map(ImagesRemoved(_))

}
