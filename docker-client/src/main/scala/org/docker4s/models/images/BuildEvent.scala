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

sealed trait BuildEvent

object BuildEvent {

  case class Pull(event: org.docker4s.models.images.PullEvent) extends BuildEvent

  case class Stream(str: String) extends BuildEvent

  case class Built(id: Image.Id) extends BuildEvent

  // -------------------------------------------- Circe decoders

  private val infoDecoder: Decoder[BuildEvent] = Decoder.instance({ c =>
    c.downField("stream").as[String].map(Stream)
  })

  private val pullDecoder: Decoder[BuildEvent] = PullEvent.decoder.map(Pull)

  private val builtDecoder: Decoder[BuildEvent] = Decoder.instance({ c =>
    c.downField("aux").downField("ID").as[String].map(id => Built(Image.Id(id)))
  })

  val decoder: Decoder[BuildEvent] = {
    List(infoDecoder, builtDecoder, pullDecoder).reduce(_ or _)
  }

}
