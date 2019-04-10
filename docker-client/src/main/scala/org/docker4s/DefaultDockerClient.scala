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

import java.util.Base64

import cats.effect.Effect
import cats.syntax.all._
import com.typesafe.scalalogging.LazyLogging
import fs2.Stream
import io.circe.Json
import org.docker4s.api.{Containers, Criterion, Images, Networks, Secrets, System, Volumes}
import org.docker4s.models.containers._
import org.docker4s.models.system.{Event, Info, Version}
import org.docker4s.models.images._
import org.docker4s.models.networks.{Network, NetworkCreated, NetworksPruned}
import org.docker4s.models.secrets.{Secret, SecretCreated}
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

    /**
      * Returns differences in the given container's file system since it was started.
      *
      * Similar to the `docker container diff` command.
      */
    override def diff(id: Container.Id): F[List[ContainerChange]] = {
      F.delay(logger.info(s"Diffing the container ${id.value}.")) *>
        client
          .get(s"/containers/${id.value}/changes")
          .expectMany(ContainerChange.decoder)
    }

    /**
      * Returns a list of containers. Similar to the `docker ps` or `docker container ls` commands.
      */
    override def list(criteria: Criterion[Containers.ListCriterion]*): F[List[ContainerSummary]] = {
      F.delay(logger.info(s"Listing containers [criteria: ${Criterion.toDebugString(criteria)}].")) *>
        client
          .get("/containers/json")
          .criteria(criteria)
          .expectMany(ContainerSummary.decoder)
    }

    /**
      * Renames the given Docker container.
      */
    override def rename(id: Container.Id, name: String): F[Unit] = {
      F.delay(logger.info(s"Renaming the container ${id.value} to $name.")) *>
        client
          .post(s"/containers/${id.value}/rename")
          .queryParam("name", name)
          .execute
    }

    override def create(image: String): F[ContainerCreated] = {
      F.delay(logger.info(s"Creating a new container using the image '$image'.")) *>
        client
          .post(s"/containers/create")
          .body(Json.obj("image" -> Json.fromString(image)))
          .expect(ContainerCreated.decoder)
    }

    override def start(id: Container.Id): F[Unit] = {
      F.delay(logger.info(s"Starting the container ${id.value}.")) *>
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
      F.delay(logger.info(s"Stopping the container ${id.value} [timeout: $timeout].")) *>
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
      F.delay(logger.info(s"Restarting the container ${id.value} [timeout: $timeout].")) *>
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
      F.delay(logger.info(s"Killing the container ${id.value} [signal: $signal].")) *>
        client
          .post(s"/containers/${id.value}/kill")
          .queryParam("signal", signal)
          .execute
    }

    /**
      * Pauses the given docker container. Similar to the `docker container pause` command.
      */
    override def pause(id: Container.Id): F[Unit] = {
      F.delay(logger.debug(s"Pausing the container ${id.value}.")) *>
        client.post(s"/containers/${id.value}/pause").execute
    }

    /**
      * Unpauses the given docker container. Similar to the `docker container unpause` command.
      */
    override def unpause(id: Container.Id): F[Unit] = {
      F.delay(logger.info(s"Unpausing the container ${id.value}.")) *>
        client.post(s"/containers/${id.value}/unpause").execute
    }

    /**
      * Waits until a container stops, then returns the exit code. Similar to the `docker container wait` command.
      */
    override def await(id: Container.Id): F[ContainerExit] = {
      F.delay(logger.info(s"Waiting for the container ${id.value} to stop.")) *>
        client
          .post(s"s/containers/${id.value}/wait")
          .expect(ContainerExit.decoder)
    }

    /**
      * List processes running inside a container. Similar to the `docker container ps` command.
      */
    def top(id: Container.Id, psArgs: Option[String]): F[Processes] = {
      F.delay(logger.info(s"Listing processes for container ${id.value}.")) *>
        client
          .get(s"/containers/${id.value}/top")
          .queryParam("ps_args", psArgs)
          .expect(Processes.decoder)
    }

    /**
      * Removes the given container. Similar to the `docker rm` command.
      */
    override def remove(id: Container.Id): F[Unit] = {
      F.delay(logger.info(s"Removing the container ${id.value}.")) *>
        client.delete(s"/containers/${id.value}").execute
    }

    override def logs(id: Container.Id, criteria: Criterion[Containers.LogCriterion]*): Stream[F, Containers.Log] = {
      Stream
        .eval(
          F.delay(logger.info(s"Pulling logs for the container ${id.value} " +
            s"[criteria: ${Criterion.toDebugString(criteria)}].")))
        .flatMap({ _ =>
          client
            .get(s"/containers/${id.value}/logs")
            .criteria(criteria)
            .stream
            .through(LogDecoder.decode)
        })
    }

    /**
      * Delete stopped containers. Similar to the `docker container prune` command.
      */
    override def prune(): F[ContainersPruned] = {
      F.delay(logger.info(s"Pruning containers.")) *>
        client
          .post("/containers/prune")
          .expect(ContainersPruned.decoder)
    }

  }

  override val secrets: Secrets[F] = new Secrets[F] {

    /**
      * Returns secrets configured in the docker host. Similar to the `docker secret ls` command.
      */
    override def list(criteria: Criterion[Secrets.ListCriterion]*): F[List[Secret]] = {
      F.delay(logger.info(s"Listing secrets [criteria: ${Criterion.toDebugString(criteria)}].")) *>
        client.get("/secrets").criteria(criteria).expectMany(Secret.decoder)
    }

    /**
      * Returns detailed information for the given secret. Similar to the `docker secret inspect` command.
      */
    override def inspect(id: Secret.Id): F[Secret] = {
      F.delay(logger.info(s"Inspecting secret ${id.value}.")) *>
        client.get(s"/secrets/${id.value}").expect(Secret.decoder)
    }

    /**
      * Creates a new secret with the given data in the docker host. Similar to the `docker secret create` command.
      */
    override def create(name: String, data: Array[Byte]): F[SecretCreated] = {
      F.delay(logger.info(s"Creating the secret $name.")) *>
        client
          .post("/secrets/create")
          .body(
            Json.obj(
              "Name" -> Json.fromString(name),
              "Data" -> Json.fromString(
                Base64.getEncoder.encodeToString(data)
              )
            ))
          .expect(SecretCreated.decoder)
    }

    /**
      * Updates a secret in the docker host. Currently only label updates are supported.
      */
    override def update(id: Secret.Id, version: Long, name: String, labels: Map[String, String]): F[Unit] = {
      F.delay(logger.info(s"Updating the secret ${id.value}.")) *>
        client
          .post(s"/secrets/${id.value}/update")
          .queryParam("version", version)
          .body(
            Json.obj(
              "Name" -> Json.fromString(name),
              "Labels" -> Json.obj(labels.mapValues(Json.fromString).toSeq: _*)
            ))
          .execute
    }

    /**
      * Removes the given secret from the docker host. Similar to the `docker secret rm` command.
      */
    override def remove(id: Secret.Id): F[Unit] = {
      F.delay(logger.info(s"Removing the secret ${id.value}.")) *>
        client.delete(s"/secrets/${id.value}").execute
    }

  }

  override val system: System[F] = new System[F] {

    /**
      * Returns system-wide information. Similar to the `docker system info` command.
      */
    override def info: F[Info] = {
      F.delay(logger.info("Fetching general information from the docker host.")) *>
        client.get("/info").expect(Info.decoder)
    }

    /**
      * Streams real-time events from the server. Similar to the `docker system events` command.
      */
    override def events(criteria: Criterion[System.EventsCriterion]*): Stream[F, Event] = {
      Stream
        .eval(
          F.delay(logger.info(s"Fetching events from the docker host " +
            s"[criteria: ${Criterion.toDebugString(criteria)}].")))
        .flatMap({ _ =>
          client.get("/events").criteria(criteria).stream(Event.decoder)
        })
        .evalTap({ event =>
          F.delay(logger.debug(s"System event: $event"))
        })
    }

    /**
      * Returns version information from the server. Similar to the `docker version` command.
      */
    override def version: F[Version] = {
      F.delay(logger.info("Fetching version information from the docker host.")) *>
        client.get("/version").expect(Version.decoder)
    }

  }

  override val images: Images[F] = new Images[F] {

    /** Returns a list of images on the server. Similar to the `docker image list` or `docker images` command. */
    override def list(criteria: Criterion[Images.ListCriterion]*): F[List[ImageSummary]] = {
      F.delay(logger.info(s"Listing images [criteria: ${Criterion.toDebugString(criteria)}].")) *>
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
      // TODO: Authentication
      val response = ids match {
        case Seq(id) =>
          client.get(s"/images/${id.value}/get").stream

        case _ =>
          client
            .get(s"/images/get")
            .queryParam("names", ids.map(_.value))
            .stream
      }

      Stream.eval(F.delay(logger.info(s"Saving the images $ids."))).flatMap(_ => response)
    }

    /**
      * Returns low-level information about an image. Similar to the `docker image inspect` command.
      */
    override def inspect(id: Image.Id): F[Image] = {
      F.delay(logger.info(s"Inspecting the image ${id.value}.")) *>
        client
          .get(s"/images/${id.value}/json")
          .expect(Image.decoder)
    }

    /**
      * Pulls the given docker container image.
      */
    override def pull(name: String, tag: Option[String]): Stream[F, PullEvent] = {
      Stream
        .eval(F.delay(logger.info(s"Pulling the image $name.")))
        .flatMap({ _ =>
          client
            .post("/images/create")
            .queryParam("fromImage", name)
            .queryParam("tag", tag.getOrElse("latest"))
            .stream(PullEvent.decoder)
            .evalTap({ event =>
              F.delay(logger.debug(s"Pull event ($name): $event"))
            })
        })
    }

    /**
      *
      */
    override def build(image: Stream[F, Byte], name: Option[String]): Stream[F, BuildEvent] = {
      client
        .post("/build")
        .queryParam("t", name)
        .body(image)
        .stream(BuildEvent.decoder)
        .evalTap({ event =>
          F.delay(logger.debug(s"Build event: $event"))
        })
    }

    /**
      * Removes the given image, along with any untagged parent images.
      *
      * Similar to the `docker image rm` command.
      */
    override def remove(id: Image.Id, force: Boolean = false, noprune: Boolean = false): F[ImagesRemoved] = {
      F.delay(logger.info(s"Removing the image ${id.value} [force: $force, noprune: $noprune].")) *>
        client
          .delete(s"/images/${id.value}")
          .expect(ImagesRemoved.decoder)
    }

    /** Returns the history of the image, i.e. its parent layers. Similar to the `docker history` command. */
    override def history(id: Image.Id): F[List[ImageHistory]] = {
      F.delay(logger.info(s"Fetching the history for the image ${id.value}.")) *>
        client
          .get(s"/images/${id.value}/history")
          .expectMany(ImageHistory.decoder)
    }

    /**
      * Removes all dangling images.
      */
    override def prune(): F[ImagesPruned] = {
      F.delay(logger.info("Pruning all images.")) *>
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
      F.delay(logger.info(s"Listing volumes [criteria: ${Criterion.toDebugString(criteria)}].")) *>
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
      F.delay({
        val nameStr = name.getOrElse("with no name")
        logger.info(s"Creating the volume $nameStr.")
      }) *>
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
      F.delay(logger.info(s"Inspecting the volume $name.")) *>
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
      F.delay(logger.info(s"Removing the volume $name [force: $force].")) *>
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
      F.delay(logger.info("Pruning all volumes.")) *>
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
      F.delay(logger.info(s"Listing networks [criteria: ${Criterion.toDebugString(criteria)}].")) *>
        client.get("/networks").criteria(criteria).expectMany(Network.decoder)
    }

    /**
      * Returns the information config for the given network.
      */
    override def inspect(id: Network.Id): F[Network] = {
      F.delay(logger.info(s"Inspecting the network ${id.value}.")) *>
        client.get(s"/networks/${id.value}").expect(Network.decoder)
    }

    /**
      * Removes the given network from the docker host.
      */
    override def remove(id: Network.Id): F[Unit] = {
      F.delay(logger.info(s"Removing the network ${id.value}.")) *>
        client.delete(s"/networks/${id.value}").execute
    }

    override def create(name: String): F[NetworkCreated] = {
      F.delay(logger.info(s"Creating a network with the name $name.")) *>
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
      F.delay(logger.info(s"Connecting the container ${container.value} to the network ${network.value}.")) *>
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
      F.delay(
        logger.info(s"Disconnecting the container ${container.value} " +
          s"from the network ${network.value} [force: $force].")) *>
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
      F.delay(logger.info("Pruning all networks.")) *>
        client.post(s"/networks/prune").expect(NetworksPruned.decoder)
    }

  }

}
