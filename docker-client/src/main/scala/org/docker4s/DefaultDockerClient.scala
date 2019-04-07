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
import com.typesafe.scalalogging.LazyLogging
import fs2.Stream
import io.circe.Json
import org.docker4s.api.swarm.Secrets
import org.docker4s.api.{Containers, Criterion, Images, Networks, System, Volumes}
import org.docker4s.models.containers._
import org.docker4s.models.system.{Event, Info, Version}
import org.docker4s.models.images._
import org.docker4s.models.networks.{Network, NetworkCreated, NetworksPruned}
import org.docker4s.models.secrets.Secret
import org.docker4s.models.volumes.{Volume, VolumeList, VolumesPruned}
import org.docker4s.transport.Client
import org.docker4s.util.LogDecoder
import org.http4s.circe.jsonEncoder

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

private[docker4s] class DefaultDockerClient[F[_]](private val client: Client[F])(implicit F: Effect[F])
    extends DockerClient[F]
    with LazyLogging {

  override val containers: Containers[F] = new Containers[F] {

    override def diff(id: Container.Id): F[List[ContainerChange]] = {
      client
        .get(s"/containers/${id.value}/changes")
        .expectMany(ContainerChange.decoder)
    }

    /**
      * Returns a list of containers. Similar to the `docker ps` or `docker container ls` commands.
      */
    override def list(criteria: Criterion[Containers.ListCriterion]*): F[List[ContainerSummary]] = {
      client
        .get("/containers/json")
        .criteria(criteria)
        .expectMany(ContainerSummary.decoder)
    }

    /**
      * Renames the given Docker container.
      */
    override def rename(id: Container.Id, name: String): F[Unit] = {
      client
        .post(s"/containers/${id.value}/rename")
        .queryParam("name", name)
        .execute
    }

    override def create(image: Option[String]): F[ContainerCreated] = {
      client
        .post(s"/containers/create")
        .body(Json.obj("image" -> image.fold(Json.Null)(Json.fromString)))
        .expect(ContainerCreated.decoder)
    }

    override def start(id: Container.Id): F[Unit] = {
      client
        .post(s"/containers/${id.value}/start")
//        .handleStatusWith({
//          case Status.NotFound => (_, _) => new ContainerNotFoundException(id.value, "")
//        })
        .execute
    }

    /**
      * Stops the given container. Similar to the `docker stop` command.
      *
      * @param timeout Amount of time to give the container to stop before killing it. Defaults to 10 seconds.
      */
    override def stop(id: Container.Id, timeout: FiniteDuration): F[Unit] = {
      client
        .post(s"/containers/${id.value}/stop")
        .queryParam("t", timeout.toSeconds)
        .execute
    }

    /**
      * Restart the given container. Similar to the `docker restart` command.
      *
      * @param timeout Amount of time to give the container to stop before killing it. Defaults to 10 seconds.
      */
    override def restart(id: Container.Id, timeout: FiniteDuration): F[Unit] = {
      client
        .post(s"/containers/${id.value}/restart")
        .queryParam("t", timeout.toSeconds)
        .execute
    }

    /**
      * Kills the given docker container by sending a POSIX signal such as SIGKILL.
      *
      * @param signal Signal to send to the container, e.g. SIGKILL, SIGINT, ..
      */
    override def kill(id: Container.Id, signal: String): F[Unit] = {
      client
        .post(s"/containers/${id.value}/kill")
        .queryParam("signal", signal)
        .execute
    }

    /**
      * Pauses the given docker container. Similar to the `docker container pause` command.
      */
    override def pause(id: Container.Id): F[Unit] = {
      client.post(s"/containers/${id.value}/pause").execute
    }

    /**
      * Unpauses the given docker container. Similar to the `docker container unpause` command.
      */
    override def unpause(id: Container.Id): F[Unit] = {
      client.post(s"/containers/${id.value}/unpause").execute
    }

    /**
      * Waits until a container stops, then returns the exit code. Similar to the `docker container wait` command.
      */
    override def await(id: Container.Id): F[ContainerExit] = {
      client
        .post(s"s/containers/${id.value}/wait")
        .expect(ContainerExit.decoder)
    }

    /**
      * Removes the given container. Similar to the `docker rm` command.
      */
    override def remove(id: Container.Id): F[Unit] = {
      client.delete(s"/containers/${id.value}").execute
    }

    override def logs(id: Container.Id, criteria: Criterion[Containers.LogCriterion]*): Stream[F, Containers.Log] = {
      client
        .get(s"/containers/${id.value}/logs")
        .criteria(criteria)
        .stream
        .through(LogDecoder.decode)
    }

    /**
      * Delete stopped containers. Similar to the `docker container prune` command.
      */
    override def prune(): F[ContainersPruned] = {
      client
        .post("/containers/prune")
        .expect(ContainersPruned.decoder)
    }

  }

  override val secrets: Secrets[F] = new Secrets[F] {

    /**
      * Lists all secrets.
      */
    override def list(criteria: Criterion[Secrets.ListCriterion]*): F[List[Secret]] = {
      client.get("/secrets").criteria(criteria).expectMany(Secret.decoder)
    }

    override def inspect(id: Secret.Id): F[Secret] = {
      client.get(s"/secrets/${id.value}").expect(Secret.decoder)
    }

    /**
      * Deletes the secret with the given ID.
      */
    override def delete(id: Secret.Id): F[Unit] = {
      client.delete(s"/secrets/${id.value}").execute
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

    /**
      * Saves one or more images to a TAR archive. Similar to the `docker image save` command.
      */
    override def save(ids: Seq[Image.Id]): Stream[F, Byte] = {
      // TODO: Error handling: 404 when one of the images cannot be found ("No such image: busybox")
      // TODO: Authenatication
      ids match {
        case Seq(id) =>
          client.get(s"/images/${id.value}/get").stream

        case _ =>
          client
            .get(s"/images/get")
            .queryParam("names", ids.map(_.value))
            .stream
      }
    }

    /** Returns low-level information about an image. Similar to the `docker image inspect` command. */
    override def inspect(id: Image.Id): F[Image] = {
      client
        .get(s"/images/${id.value}/json")
//        .handleStatusWith({
//          case Status.NotFound => (_, _) => new ImageNotFoundException(id.value, "")
//        })
        .expect(Image.decoder)
    }

    /**
      * Pulls the given docker container image.
      */
    override def pull(name: String, tag: Option[String]): Stream[F, PullEvent] = {
      client
        .post("/images/create")
        .queryParam("fromImage", name)
        .queryParam("tag", tag.getOrElse("latest"))
        .stream(PullEvent.decoder)
    }

    /** Returns the history of the image, i.e. its parent layers. Similar to the `docker history` command. */
    override def history(id: Image.Id): F[List[ImageHistory]] = {
      client
        .get(s"/images/${id.value}/history")
        .expectMany(ImageHistory.decoder)
    }

    /**
      * Removes all dangling images.
      */
    override def prune(): F[ImagesPruned] = {
      client
        .post("/images/prune")
        .expect(ImagesPruned.decoder)
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
//        .handleStatusWith({
//          case Status.NotFound => (_, _) => new VolumeNotFoundException(name, "")
//        })
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
//        .handleStatusWith({
//          case Status.NotFound => (_, _) => new VolumeNotFoundException(name, "")
//        })
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

  override val networks: Networks[F] = new Networks[F] {

    /**
      * Returns the list of networks configured in the docker host.
      */
    override def list(criteria: Criterion[Networks.ListCriterion]*): F[List[Network]] = {
      client.get("/networks").criteria(criteria).expectMany(Network.decoder)
    }

    /**
      * Returns the information config for the given network.
      */
    override def inspect(id: Network.Id): F[Network] = {
      client.get(s"/networks/${id.value}").expect(Network.decoder)
    }

    /**
      * Removes the given network from the docker host.
      */
    override def remove(id: Network.Id): F[Unit] = {
      client.delete(s"/networks/${id.value}").execute
    }

    override def create(name: String): F[NetworkCreated] = {
      client
        .post("/networks/create")
        .body(
          Json.obj(
            "Name" -> Json.fromString(name)
          ))
        .expect(NetworkCreated.decoder)
    }

    /**
      * Connects the container to the given network in the docker host.
      */
    override def connect(network: Network.Id, container: Container.Id): F[Unit] = {
      client
        .post(s"/networks/${network.value}/connect")
        .body(
          Json.obj(
            "Container" -> Json.fromString(container.value)
          ))
        .execute
    }

    /**
      * Disconnects the container from the given network in the docker host.
      */
    override def disconnect(network: Network.Id, container: Container.Id, force: Boolean): F[Unit] = {
      client
        .post(s"/networks/${network.value}/disconnect")
        .body(
          Json.obj(
            "Container" -> Json.fromString(container.value),
            "Force" -> Json.fromBoolean(force)
          ))
        .execute
    }

    /**
      * Removes unused networks from the docker host.
      */
    override def prune(): F[NetworksPruned] = {
      client.post(s"/networks/prune").expect(NetworksPruned.decoder)
    }

  }

}
