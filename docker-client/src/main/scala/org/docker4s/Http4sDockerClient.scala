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

import cats.effect.Effect
import fs2.Stream
import io.circe.{Decoder, Json}
import org.docker4s.api.{Images, System, Volumes}
import org.docker4s.models.system.{Event, Info, Version}
import org.docker4s.models.images.{Image, ImageHistory, ImageSummary}
import org.docker4s.models.volumes.{Volume, VolumeList, VolumesPruned}
import org.docker4s.transport.Client
import org.http4s.{Header, Method, Query, Request, Uri}
import org.http4s.circe.jsonEncoder

import scala.language.higherKinds

private[docker4s] class Http4sDockerClient[F[_]: Effect](private val client: Client[F], private val uri: Uri)
    extends DockerClient[F] {

  override def system: System[F] = new System[F] {

    /**
      * Returns system-wide information. Similar to the `docker system info` command.
      */
    override def info: F[Info] = {
      client.expect[Info](GET.withUri(uri.withPath("/info")))(Info.decoder)
    }

    /**
      * Streams real-time events from the server. Similar to the `docker system events` command.
      */
    override def events(criteria: Criterion[System.EventsCriterion]*): Stream[F, Event] = {
      client.stream[Event](GET.withUri(uri.withPath("/events").withCriteria(criteria)))(Event.decoder)
    }

    /**
      * Returns version information from the server. Similar to the `docker version` command.
      */
    override def version: F[Version] = {
      client.expect[Version](GET.withUri(uri.withPath("/version")))(Version.decoder)
    }

  }

  override def images: Images[F] = new Images[F] {

    /** Returns a list of images on the server. Similar to the `docker image list` or `docker images` command. */
    override def list(criteria: Criterion[Images.ListCriterion]*): F[List[ImageSummary]] = {
      implicit val decoder: Decoder[ImageSummary] = ImageSummary.decoder
      client.expect[List[ImageSummary]](GET.withUri(uri.withPath("/images/json").withCriteria(criteria)))
    }

    /** Returns low-level information about an image. Similar to the `docker image inspect` command. */
    override def inspect(id: Image.Id): F[Image] = {
      client.expect[Image](GET.withUri(uri.withPath(s"/images/${id.value}/json")))(Image.decoder)
    }

    /** Returns the history of the image, i.e. its parent layers. Similar to the `docker history` command. */
    override def history(id: Image.Id): F[List[ImageHistory]] = {
      implicit val decoder: Decoder[ImageHistory] = ImageHistory.decoder
      client.expect[List[ImageHistory]](GET.withUri(uri.withPath(s"/images/${id.value}/history")))
    }

  }

  override def volumes: Volumes[F] = new Volumes[F] {

    /**
      * Returns volumes currently registered by the docker daemon. Similar to the `docker volume ls` command.
      */
    override def list(criteria: Criterion[Volumes.ListCriterion]*): F[VolumeList] = {
      client.expect[VolumeList](GET.withUri(uri.withPath(s"/volumes").withCriteria(criteria)))(VolumeList.decoder)
    }

    /**
      * Creates and registers a named volume. Similar to the `docker volume create` command.
      */
    override def create(
        name: Option[String],
        driver: String,
        options: Map[String, String],
        labels: Map[String, String]): F[Volume] = {
      client.expect[Volume](
        POST
          .withUri(uri.withPath(s"/volumes/create"))
          .withEntity(
            Json.obj(
              "Name" -> name.fold(Json.Null)(Json.fromString),
              "Driver" -> Json.fromString(driver),
              "DriverOpts" -> Json.obj(
                options.mapValues(Json.fromString).toSeq: _*
              ),
              "Labels" -> Json.obj(
                labels.mapValues(Json.fromString).toSeq: _*
              )
            )
          ))(Volume.decoder)
    }

    /**
      * Returns volume information by name. Similar to the `docker volume inspect` command.
      */
    override def inspect(name: String): F[Volume] = {
      client.expect[Volume](GET.withUri(uri.withPath(s"/volumes/$name")))(Volume.decoder)
    }

    /**
      * Removes the given volume. Similar to the `docker volume rm` command.
      *
      * @param name Name of the volume to remove
      * @param force Force the removal of the volume
      */
    override def remove(name: String, force: Boolean): F[Unit] = {
      client.evaluate(DELETE.withUri(uri.withPath(s"/volumes/$name").withQueryParam("force", force)))
    }

    /**
      * Removes unused volumes. Similar to the `docker volume prune` command.
      */
    override def prune(): F[VolumesPruned] = {
      client.expect[VolumesPruned](POST.withUri(uri.withPath(s"/volumes/prune")))(VolumesPruned.decoder)
    }

  }

  // -------------------------------------------- Utility methods & classes

  private def GET: Request[F] =
    Request[F]()
      .withMethod(Method.GET)
      .withHeaders(Header("Host", uri.host.map(_.value).getOrElse("localhost")))

  private def POST: Request[F] =
    Request[F]()
      .withMethod(Method.POST)
      .withHeaders(Header("Host", uri.host.map(_.value).getOrElse("localhost")))

  private def DELETE: Request[F] =
    Request[F]()
      .withMethod(Method.DELETE)
      .withHeaders(Header("Host", uri.host.map(_.value).getOrElse("localhost")))

  private implicit class UriOps(private val uri: Uri) {

    def withCriteria(criteria: Seq[Criterion[_]]): Uri = {
      uri.copy(query = Query.fromMap(Criterion.compile(criteria)))
    }

  }

}
