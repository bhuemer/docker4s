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
import io.circe.Json
import org.docker4s.api.{Containers, Images, System, Volumes}
import org.docker4s.errors.{ImageNotFoundException, VolumeNotFoundException}
import org.docker4s.models.containers.Container
import org.docker4s.models.system.{Event, Info, Version}
import org.docker4s.models.images.{Image, ImageHistory, ImageSummary}
import org.docker4s.models.volumes.{Volume, VolumeList, VolumesPruned}
import org.docker4s.transport.Client
import org.docker4s.util.LogDecoder
import org.http4s.Status
import org.http4s.circe.jsonEncoder

import scala.language.higherKinds

private[docker4s] class Http4sDockerClient[F[_]: Effect](private val client: Client[F]) extends DockerClient[F] {

  override val containers: Containers[F] = new Containers[F] {

    override def logs(id: Container.Id, criteria: Criterion[Containers.LogCriterion]*): Stream[F, Containers.Log] = {
      client
        .get(s"/containers/${id.value}/logs")
        .criteria(criteria)
        .stream
        .through(LogDecoder.decode)
    }

  }

  override val system: System[F] = new System[F] {

    /**
      * Returns system-wide information. Similar to the `docker system info` command.
      */
    override def info: F[Info] = {
      client.get("/info").expect(Info.decoder)
    }

    /**
      * Streams real-time events from the server. Similar to the `docker system events` command.
      */
    override def events(criteria: Criterion[System.EventsCriterion]*): Stream[F, Event] = {
      client.get("/events").criteria(criteria).stream(Event.decoder)
    }

    /**
      * Returns version information from the server. Similar to the `docker version` command.
      */
    override def version: F[Version] = {
      client.get("/version").expect(Version.decoder)
    }

  }

  override val images: Images[F] = new Images[F] {

    /** Returns a list of images on the server. Similar to the `docker image list` or `docker images` command. */
    override def list(criteria: Criterion[Images.ListCriterion]*): F[List[ImageSummary]] = {
      client
        .get("/images/json")
        .criteria(criteria)
        .expectMany(ImageSummary.decoder)
    }

    /** Returns low-level information about an image. Similar to the `docker image inspect` command. */
    override def inspect(id: Image.Id): F[Image] = {
      client
        .get(s"/images/${id.value}/json")
        .handleStatusWith({
          case Status.NotFound => new ImageNotFoundException(id.value, "")
        })
        .expect(Image.decoder)
    }

    /** Returns the history of the image, i.e. its parent layers. Similar to the `docker history` command. */
    override def history(id: Image.Id): F[List[ImageHistory]] = {
      client
        .get(s"/images/${id.value}/history")
        .expectMany(ImageHistory.decoder)
    }

  }

  override val volumes: Volumes[F] = new Volumes[F] {

    /**
      * Returns volumes currently registered by the docker daemon. Similar to the `docker volume ls` command.
      */
    override def list(criteria: Criterion[Volumes.ListCriterion]*): F[VolumeList] = {
      client.get(s"/volumes").criteria(criteria).expect(VolumeList.decoder)
    }

    /**
      * Creates and registers a named volume. Similar to the `docker volume create` command.
      */
    override def create(
        name: Option[String],
        driver: Option[String],
        options: Map[String, String],
        labels: Map[String, String]): F[Volume] = {
      client
        .post(s"/volumes/create")
        .body(
          Json.obj(
            "Name" -> name.fold(Json.Null)(Json.fromString),
            "Driver" -> Json.fromString(driver.getOrElse("local")),
            "DriverOpts" -> Json.obj(
              options.mapValues(Json.fromString).toSeq: _*
            ),
            "Labels" -> Json.obj(
              labels.mapValues(Json.fromString).toSeq: _*
            )
          )
        )
        .expect(Volume.decoder)
    }

    /**
      * Returns volume information by name. Similar to the `docker volume inspect` command.
      */
    override def inspect(name: String): F[Volume] = {
      client
        .get(s"/volumes/$name")
        .handleStatusWith({
          case Status.NotFound => new VolumeNotFoundException(name, "")
        })
        .expect(Volume.decoder)
    }

    /**
      * Removes the given volume. Similar to the `docker volume rm` command.
      *
      * @param name Name of the volume to remove
      * @param force Force the removal of the volume
      */
    override def remove(name: String, force: Boolean): F[Unit] = {
      client
        .delete(s"/volumes/$name")
        .queryParam("force", force)
        .handleStatusWith({
          case Status.NotFound => new VolumeNotFoundException(name, "")
        })
        .execute
    }

    /**
      * Removes unused volumes. Similar to the `docker volume prune` command.
      */
    override def prune(): F[VolumesPruned] = {
      client
        .post("/volumes/prune")
        .expect(VolumesPruned.decoder)
    }

  }

}
