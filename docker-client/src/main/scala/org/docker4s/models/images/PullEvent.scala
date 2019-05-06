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

import io.circe.{Decoder, DecodingFailure}

import scala.concurrent.duration.Duration

sealed trait PullEvent

object PullEvent {

  case class PullingImage(message: String) extends PullEvent

  sealed abstract class LayerPullEvent(id: String) extends PullEvent

  object Layer {

    /**
      * Indicates that the intermediate layer with the given ID already exists and doesn't need to be pulled.
      */
    case class Existed(id: String) extends LayerPullEvent(id)

    /**
      * Indicates that the intermediate layer with the given ID needs to be pulled.
      */
    case class Pulling(id: String) extends LayerPullEvent(id)

    case class Waiting(id: String) extends LayerPullEvent(id)

    case class Downloading(id: String, current: Long, total: Long) extends LayerPullEvent(id)

    case class Verifying(id: String) extends LayerPullEvent(id)

    case class Downloaded(id: String) extends LayerPullEvent(id)

    case class Extracting(id: String, current: Long, total: Long) extends LayerPullEvent(id)

    case class Pulled(id: String) extends LayerPullEvent(id)

    case class Retrying(id: String, duration: Duration) extends LayerPullEvent(id)

  }

  case class Digest(digest: String) extends PullEvent

  case class Status(status: String) extends PullEvent

  // -------------------------------------------- Circe decoders

  val decoder: Decoder[PullEvent] = Decoder.instance({ c =>
    def layerEvent(f: String => LayerPullEvent): Either[DecodingFailure, PullEvent] = {
      for {
        id <- c.downField("id").as[String].right
      } yield f(id)
    }

    def progressEvent(f: (String, Long, Long) => LayerPullEvent): Either[DecodingFailure, PullEvent] = {
      for {
        id <- c.downField("id").as[String].right
        progress <- c
          .downField("progressDetail")
          .as(Decoder.instance({ p =>
            for {
              current <- p.downField("current").as[Long].right
              total <- p.downField("total").as[Long].right
            } yield (current, total)
          }))
      } yield f(id, progress._1, progress._2)
    }

    def retryingEvent(str: String): Either[DecodingFailure, PullEvent] = {
      for {
        id <- c.downField("id").as[String].right
      } yield Layer.Retrying(id, Duration(str.substring("Retrying in".length)))
    }

    c.downField("status")
      .as[String]
      .right
      .flatMap({
        case pulling if pulling.startsWith("Pulling from") =>
          for {
            id <- c.downField("id").as[String].right
          } yield PullingImage(s"$id: $pulling")

        case "Already exists"     => layerEvent(Layer.Existed)
        case "Pulling fs layer"   => layerEvent(Layer.Pulling)
        case "Waiting"            => layerEvent(Layer.Waiting)
        case "Downloading"        => progressEvent(Layer.Downloading)
        case "Verifying Checksum" => layerEvent(Layer.Verifying)
        case "Download complete"  => layerEvent(Layer.Downloaded)
        case "Extracting"         => progressEvent(Layer.Extracting)
        case "Pull complete"      => layerEvent(Layer.Pulled)

        case retrying if retrying.startsWith("Retrying in") => retryingEvent(retrying)

        case digest if digest.startsWith("Digest: ") => Right(Digest(digest.substring("Digest: ".length)))
        case status if status.startsWith("Status: ") => Right(Status(status.substring("Status: ".length)))

        case otherwise =>
          Left(DecodingFailure(s"Unknown pull event status $otherwise.", c.history))
      })
  })

}
