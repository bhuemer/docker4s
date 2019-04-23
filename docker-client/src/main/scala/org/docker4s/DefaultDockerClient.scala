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
import org.docker4s.api.{Containers, Images, Networks, Parameter, Secrets, System, Volumes}
import org.docker4s.errors.ContainerNotFoundException
import org.docker4s.models.containers._
import org.docker4s.models.system.{Event, Info, Version}
import org.docker4s.models.images._
import org.docker4s.models.networks.{Network, NetworkCreated, NetworksPruned}
import org.docker4s.models.secrets.{Secret, SecretCreated}
import org.docker4s.models.volumes.{Volume, VolumeList, VolumesPruned}
import org.docker4s.transport.Client
import org.docker4s.util.LogDecoder
import org.http4s.Status

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
      * Returns a list of containers.
      *
      * Similar to the `docker ps` or `docker container ls` commands.
      */
    override def list(parameters: Parameter[Containers.ListParameter]*): F[List[ContainerSummary]] = {
      F.delay(logger.info(s"Listing containers [parameters: ${Parameter.toDebugString(parameters)}].")) *>
        client
          .get("/containers/json")
          .withParameters(parameters)
          .expectMany(ContainerSummary.decoder)
    }

    /**
      * Exports the contents of the given container as a tarball.
      *
      * Similar to the `docker container export` command.
      *
      * @param path Resource in the container’s filesystem to archive. If no path is provided, then the entire file
      *             system of the container will be exported in the TAR archive.
      */
    override def export(id: Container.Id, path: Option[String]): Stream[F, Byte] = {
      Stream
        .eval(F.delay(s"Exporting contents for the container ${id.value} [path: $path]."))
        .flatMap({ _ =>
          path match {
            case Some(_) => client.get(s"/containers/${id.value}/archive").withQueryParam("path", path).stream
            case None    => client.get(s"/containers/${id.value}/export").stream
          }
        })
    }

    /**
      * Renames the given Docker container.
      */
    override def rename(id: Container.Id, name: String): F[Unit] = {
      F.delay(logger.info(s"Renaming the container ${id.value} to $name.")) *>
        client
          .post(s"/containers/${id.value}/rename")
          .withQueryParam("name", name)
          .execute
    }

    override def create(parameters: Parameter[Containers.CreateParameter]*): F[ContainerCreated] = {
      F.delay(logger.info(s"Creating a new container [parameters: ${Parameter.toDebugString(parameters)}].")) *>
        client
          .post(s"/containers/create")
          .withParameters(parameters)
          .expect(ContainerCreated.decoder)
    }

    override def start(id: Container.Id): F[Unit] = {
      F.delay(logger.info(s"Starting the container ${id.value}.")) *>
        client
          .post(s"/containers/${id.value}/start")
          .on(Status.NotFound, new ContainerNotFoundException(_, _))
          .execute
    }

    /**
      * Stops the given container.
      *
      * Similar to the `docker stop` command.
      *
      * @param timeout Amount of time to give the container to stop before killing it. Defaults to 10 seconds.
      */
    override def stop(id: Container.Id, timeout: FiniteDuration): F[Unit] = {
      F.delay(logger.info(s"Stopping the container ${id.value} [timeout: $timeout].")) *>
        client
          .post(s"/containers/${id.value}/stop")
          .withQueryParam("t", timeout.toSeconds)
          .execute
    }

    /**
      * Restart the given container.
      *
      * Similar to the `docker restart` command.
      *
      * @param timeout Amount of time to give the container to stop before killing it. Defaults to 10 seconds.
      */
    override def restart(id: Container.Id, timeout: FiniteDuration): F[Unit] = {
      F.delay(logger.info(s"Restarting the container ${id.value} [timeout: $timeout].")) *>
        client
          .post(s"/containers/${id.value}/restart")
          .withQueryParam("t", timeout.toSeconds)
          .execute
    }

    /**
      * Kills the given docker container.
      *
      * Similar to the `docker kill` or `docker container kill` command.
      *
      * @param signal Signal to send to the container, e.g. SIGKILL, SIGINT, ..
      */
    override def kill(id: Container.Id, signal: String): F[Unit] = {
      F.delay(logger.info(s"Killing the container ${id.value} [signal: $signal].")) *>
        client
          .post(s"/containers/${id.value}/kill")
          .withQueryParam("signal", signal)
          .execute
    }

    /**
      * Pauses the given docker container.
      *
      * Similar to the `docker container pause` command.
      */
    override def pause(id: Container.Id): F[Unit] = {
      F.delay(logger.debug(s"Pausing the container ${id.value}.")) *>
        client.post(s"/containers/${id.value}/pause").execute
    }

    /**
      * Unpauses the given docker container.
      *
      * Similar to the `docker container unpause` command.
      */
    override def unpause(id: Container.Id): F[Unit] = {
      F.delay(logger.info(s"Unpausing the container ${id.value}.")) *>
        client.post(s"/containers/${id.value}/unpause").execute
    }

    /**
      * Waits until a container stops, then returns the exit code.
      *
      * Similar to the `docker container wait` command.
      */
    override def await(id: Container.Id): F[ContainerExit] = {
      F.delay(logger.info(s"Waiting for the container ${id.value} to stop.")) *>
        client
          .post(s"/containers/${id.value}/wait")
          .expect(ContainerExit.decoder)
    }

    /**
      * List processes running inside a container.
      *
      * Similar to the `docker container ps` command.
      */
    def top(id: Container.Id, psArgs: Option[String]): F[Processes] = {
      F.delay(logger.info(s"Listing processes for container ${id.value}.")) *>
        client
          .get(s"/containers/${id.value}/top")
          .withQueryParam("ps_args", psArgs)
          .expect(Processes.decoder)
    }

    /**
      * Removes the given container.
      *
      * Similar to the `docker rm` command.
      */
    override def remove(id: Container.Id): F[Unit] = {
      F.delay(logger.info(s"Removing the container ${id.value}.")) *>
        client.delete(s"/containers/${id.value}").execute
    }

    override def logs(id: Container.Id, parameters: Parameter[Containers.LogParameter]*): Stream[F, Containers.Log] = {
      Stream
        .eval(
          F.delay(logger.info(s"Pulling logs for the container ${id.value} " +
            s"[parameters: ${Parameter.toDebugString(parameters)}].")))
        .flatMap({ _ =>
          client
            .get(s"/containers/${id.value}/logs")
            .withParameters(parameters)
            .stream
            .through(LogDecoder.decode)
        })
    }

    /**
      * Delete stopped containers.
      *
      * Similar to the `docker container prune` command.
      */
    override def prune(parameters: Parameter[Containers.PruneParameter]*): F[ContainersPruned] = {
      F.delay(logger.info(s"Pruning containers [parameters: ${Parameter.toDebugString(parameters)}].")) *>
        client
          .post("/containers/prune")
          .withParameters(parameters)
          .expect(ContainersPruned.decoder)
    }

  }

  override val secrets: Secrets[F] = new Secrets[F] {

    /**
      * Returns secrets configured in the docker host. Similar to the `docker secret ls` command.
      */
    override def list(parameters: Parameter[Secrets.ListCriterion]*): F[List[Secret]] = {
      F.delay(logger.info(s"Listing secrets [parameters: ${Parameter.toDebugString(parameters)}].")) *>
        client.get("/secrets").withParameters(parameters).expectMany(Secret.decoder)
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
          .withBodyParam("Name", name)
          .withBodyParam("Data", Base64.getEncoder.encodeToString(data))
          .expect(SecretCreated.decoder)
    }

    /**
      * Updates a secret in the docker host. Currently only label updates are supported.
      */
    override def update(id: Secret.Id, version: Long, name: String, labels: Map[String, String]): F[Unit] = {
      F.delay(logger.info(s"Updating the secret ${id.value}.")) *>
        client
          .post(s"/secrets/${id.value}/update")
          .withQueryParam("version", version)
          .withBodyParam("Name", name)
          .withBodyParam("Labels", labels)
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
    override def events(parameters: Parameter[System.EventsCriterion]*): Stream[F, Event] = {
      Stream
        .eval(
          F.delay(logger.info(s"Fetching events from the docker host " +
            s"[parameters: ${Parameter.toDebugString(parameters)}].")))
        .flatMap({ _ =>
          client.get("/events").withParameters(parameters).stream(Event.decoder)
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
    override def list(parameters: Parameter[Images.ListParameter]*): F[List[ImageSummary]] = {
      F.delay(logger.info(s"Listing images [parameters: ${Parameter.toDebugString(parameters)}].")) *>
        client
          .get("/images/json")
          .withParameters(parameters)
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
            .withQueryParam("names", ids.map(_.value))
            .stream
      }

      Stream.eval(F.delay(logger.info(s"Saving/exporting the images $ids."))).flatMap(_ => response)
    }

    override def load(image: Stream[F, Byte], quiet: Boolean): F[ImageLoaded] = {
      F.delay(logger.info(s"Loading/importing an image from an archive [quiet: $quiet].")) *>
        client
          .post("/images/load")
          .withQueryParam("quiet", quiet)
          .withBody(image)
          .expect(ImageLoaded.decoder)
          .map({ loaded =>
            logger.info(s"Finished loading image ${loaded.id.value}.")
            loaded
          })
    }

    /**
      * Returns low-level information about an image. Similar to the `docker image inspect` command.
      */
    override def inspect(name: String): F[Image] = {
      F.delay(logger.info(s"Inspecting the image $name.")) *>
        client
          .get(s"/images/$name/json")
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
            .withQueryParam("fromImage", name)
            .withQueryParam("tag", tag.getOrElse("latest"))
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
        .withQueryParam("t", name)
        .withBody(image)
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
    override def remove(id: Image.Id, force: Boolean, prune: Boolean): F[ImagesRemoved] = {
      F.delay(logger.info(s"Removing the image ${id.value} [force: $force, prune: $prune].")) *>
        client
          .delete(s"/images/${id.value}")
          .withQueryParam("force", force)
          .withQueryParam("noprune", !prune)
          .expect(ImagesRemoved.decoder)
    }

    /**
      * Returns the history of the image, i.e. its parent layers. Similar to the `docker history` command.
      */
    override def history(id: Image.Id): F[List[ImageHistory]] = {
      F.delay(logger.info(s"Fetching the history for the image ${id.value}.")) *>
        client
          .get(s"/images/${id.value}/history")
          .expectMany(ImageHistory.decoder)
    }

    /**
      *
      */
    override def tag(id: Image.Id, repo: String, tag: Option[String]): F[Unit] = {
      F.delay(logger.info(s"Tagging image ${id.value} with $repo [tag: $tag].")) *>
        client
          .post(s"/images/${id.value}/tag")
          .withQueryParam("repo", repo)
          .withQueryParam("tag", tag)
          .execute
    }

    /**
      * Removes all dangling images.
      */
    override def prune(parameters: Parameter[Images.PruneParameter]*): F[ImagesPruned] = {
      F.delay(logger.info(s"Pruning images [parameters: ${Parameter.toDebugString(parameters)}].")) *>
        client
          .post("/images/prune")
          .withParameters(parameters)
          .expect(ImagesPruned.decoder)
    }

  }

  override val volumes: Volumes[F] = new Volumes[F] {

    /**
      * Returns volumes currently registered by the docker daemon. Similar to the `docker volume ls` command.
      */
    override def list(parameters: Parameter[Volumes.ListParameter]*): F[VolumeList] = {
      F.delay(logger.info(s"Listing volumes [parameters: ${Parameter.toDebugString(parameters)}].")) *>
        client.get(s"/volumes").withParameters(parameters).expect(VolumeList.decoder)
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
          .withBodyParam("Name", name)
          .withBodyParam("Driver", driver.getOrElse("local"))
          .withBodyParam("DriverOpts", options)
          .withBodyParam("Labels", labels)
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
          .withQueryParam("force", force)
//        .handleStatusWith({
//          case Status.NotFound => (_, _) => new VolumeNotFoundException(name, "")
//        })
          .execute
    }

    /**
      * Removes unused volumes. Similar to the `docker volume prune` command.
      */
    override def prune(parameters: Parameter[Volumes.PruneParameter]*): F[VolumesPruned] = {
      F.delay(logger.info(s"Pruning volumes [parameters: ${Parameter.toDebugString(parameters)}].")) *>
        client
          .post("/volumes/prune")
          .expect(VolumesPruned.decoder)
    }

  }

  override val networks: Networks[F] = new Networks[F] {

    /**
      * Returns the list of networks configured in the docker host.
      */
    override def list(parameters: Parameter[Networks.ListCriterion]*): F[List[Network]] = {
      F.delay(logger.info(s"Listing networks [parameters: ${Parameter.toDebugString(parameters)}].")) *>
        client.get("/networks").withParameters(parameters).expectMany(Network.decoder)
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
          .withBodyParam("Name", name)
          .expect(NetworkCreated.decoder)
    }

    /**
      * Connects the container to the given network in the docker host.
      */
    override def connect(network: Network.Id, container: Container.Id): F[Unit] = {
      F.delay(logger.info(s"Connecting the container ${container.value} to the network ${network.value}.")) *>
        client
          .post(s"/networks/${network.value}/connect")
          .withBodyParam("Container", container.value)
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
          .withBodyParam("Container", container.value)
          .withBodyParam("Force", force)
          .execute
    }

    /**
      * Removes unused networks from the docker host.
      */
    override def prune(): F[NetworksPruned] = {
      F.delay(logger.info("Pruning networks.")) *>
        client.post(s"/networks/prune").expect(NetworksPruned.decoder)
    }

  }

}
