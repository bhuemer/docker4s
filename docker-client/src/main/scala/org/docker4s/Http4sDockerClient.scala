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
package org.docker4s

import java.time.ZonedDateTime

import cats.effect.Effect
import fs2.Stream
import io.circe.Decoder
import org.docker4s.models.system.{Event, Info, Version}
import org.docker4s.models.images.{Image, ImageSummary}
import org.docker4s.transport.Client
import org.http4s.{Header, Method, Request, Uri}

import scala.language.higherKinds

private[docker4s] class Http4sDockerClient[F[_]: Effect](private val client: Client[F], private val uri: Uri)
    extends DockerClient[F] {

  override def system: api.System[F] = new api.System[F] {

    /**
      * Returns system-wide information. Similar to the `docker system info` command.
      */
    override def info: F[Info] = {
      client.expect[Info](GET.withUri(uri.withPath("/info")))(Info.decoder)
    }

    /**
      * Streams real-time events from the server. Similar to the `docker system events` command.
      */
    override def events(since: Option[ZonedDateTime], until: Option[ZonedDateTime]): Stream[F, Event] = {
      client.stream(
        GET.withUri(
          uri
            .withPath("/events")
            .withOptionQueryParam("since", since.map(_.toInstant.getEpochSecond))
            .withOptionQueryParam("until", until.map(_.toInstant.getEpochSecond)))
      )(Event.decoder)
    }

    /**
      * Returns version information from the server. Similar to the `docker version` command.
      */
    override def version: F[Version] = {
      client.expect[Version](GET.withUri(uri.withPath("/version")))(Version.decoder)
    }

  }

  override def images: api.Images[F] = new api.Images[F] {

    /** Returns a list of images on the server. Similar to the `docker image list` or `docker images` command. */
    override def list: F[List[ImageSummary]] = {
      implicit val decoder: Decoder[ImageSummary] = ImageSummary.decoder
      client.expect[List[ImageSummary]](GET.withUri(uri.withPath("/images/json")))
    }

    /** Returns low-level information about an image. Similar to the `docker image inspect` command. */
    override def inspect(id: Image.Id): F[Image] = {
      client.expect[Image](GET.withUri(uri.withPath(s"/images/${id.value}/json")))(Image.decoder)
    }

  }

  private def GET: Request[F] =
    Request[F]()
      .withMethod(Method.GET)
      .withHeaders(Header("Host", uri.host.map(_.value).getOrElse("localhost")))

}
