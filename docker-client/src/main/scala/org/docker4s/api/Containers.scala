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
package org.docker4s.api

import java.time.ZonedDateTime

import fs2.Stream
import io.circe.Json
import org.docker4s.models.containers._
import org.docker4s.models.images.Image
import org.docker4s.transport.Parameter
import org.docker4s.transport.Parameter.{body, filter, query}

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

trait Containers[F[_]] {

  def get(id: Container.Id): ContainerRef[F] = ContainerRef(this, id)

  def inspect(id: Container.Id): F[Container]

  /**
    * Returns a list of containers.
    *
    * Similar to the `docker ps` or `docker container ls` commands.
    *
    * @example {{{
    * import org.docker4s.api.Containers.ListParameter._
    * import org.docker4s.models.containers.Container
    *
    * val program = for {
    *   containers <- client.containers.list(limit(10), withStatus(Container.Status.Running))
    *   _ = containers.foreach({ container =>
    *     println(s"Currently running: $container")
    *   })
    * } yield ()
    * program.unsafeRunSync()
    * }}}
    */
  def list(parameters: Parameter[Containers.ListParameter]*): F[List[ContainerSummary]]

  /**
    * Creates a new container from the given image.
    *
    * Similar to the `docker container create` command.
    */
  def create(image: String): F[ContainerCreated] = create(Containers.CreateParameter.withImage(image))

  /**
    * Creates a new container from the given image.
    *
    * Similar to the `docker container create` command.
    */
  def create(imageId: Image.Id): F[ContainerCreated] = create(Containers.CreateParameter.withImage(imageId))

  /**
    * Creates a new container with the given parameters.
    *
    * Similar to the `docker container create` command.
    *
    * @example {{{
    * import org.docker4s.api.Containers.CreateParameter._
    * import org.docker4s.models.containers.Container
    *
    * val program = for {
    *   created <- client.containers.create(withImage("busybox"), withCmd("echo", "Hello world from docker4s!"))
    *   _ <- client.containers.start(created.id)
    * } yield ()
    * program.unsafeRunSync()
    * }}}
    */
  def create(parameters: Parameter[Containers.CreateParameter]*): F[ContainerCreated]

  /**
    * Runs the given command in the container.
    *
    * Similar to the `docker exec` command.
    */
  def exec(id: Container.Id, cmd: String, args: String*): Stream[F, Containers.Log] =
    exec(id, detach = false, cmd, args: _*)

  /**
    * Runs the given command in the container.
    *
    * Similar to the `docker exec` command.
    *
    * @param detach If `true` then the command will be run in detached mode, i.e in the background.
    */
  def exec(id: Container.Id, detach: Boolean, cmd: String, args: String*): Stream[F, Containers.Log]

  /**
    * Creates a new image from a container.
    *
    * Similar to the `docker commit` command.
    */
  def commit(
      id: Container.Id,
      repo: Option[String] = None,
      tag: Option[String] = None,
      comment: Option[String] = None,
      author: Option[String] = None,
      pause: Option[Boolean] = None): F[Image.Id] = commit(id, repo, tag, comment, author, pause, Seq.empty: _*)

  /**
    * Creates a new image from a container.
    *
    * Similar to the `docker commit` command.
    */
  def commit(
      id: Container.Id,
      repo: Option[String],
      tag: Option[String],
      comment: Option[String],
      author: Option[String],
      pause: Option[Boolean],
      parameters: Parameter[Containers.CreateParameter]*): F[Image.Id]

  /**
    * Returns differences in the given container's file system since it was started.
    *
    * Similar to the `docker container diff` command.
    */
  def diff(id: Container.Id): F[List[ContainerChange]]

  def logs(id: Container.Id, criteria: Parameter[Containers.LogParameter]*): Stream[F, Containers.Log]

  /**
    * Exports the contents of the given container as a tarball.
    *
    * Similar to the `docker container export` command.
    *
    * @param path Resource in the container’s filesystem to archive. If no path is provided, then the entire file
    *             system of the container will be exported in the TAR archive.
    */
  def export(id: Container.Id, path: Option[String] = None): Stream[F, Byte]

  /**
    * Uploads a TAR archive to be extracted to a path in the filesystem of the given container.
    */
  def upload(id: Container.Id, path: String, archive: Stream[F, Byte]): F[Unit] = upload(id, path, None, archive)

  /**
    * Uploads a TAR archive to be extracted to a path in the filesystem of the given container.
    */
  def upload(id: Container.Id, path: String, noOverwriteConflict: Option[Boolean], archive: Stream[F, Byte]): F[Unit]

  /**
    * Renames the given Docker container.
    *
    * Similar to the `docker container rename` command.
    */
  def rename(id: Container.Id, newName: String): F[Unit]

  def start(id: Container.Id): F[Unit]

  /**
    * Stops the given container.
    *
    * Similar to the `docker stop` command.
    *
    * @param timeout Amount of time to give the container to stop before killing it. Defaults to 10 seconds.
    */
  def stop(id: Container.Id, timeout: FiniteDuration = FiniteDuration(10, "s")): F[Unit]

  /**
    * Restart the given container.
    *
    * Similar to the `docker restart` command.
    *
    * @param timeout Amount of time to give the container to stop before killing it. Defaults to 10 seconds.
    */
  def restart(id: Container.Id, timeout: FiniteDuration = FiniteDuration(10, "s")): F[Unit]

  /**
    * Kills the given docker container.
    *
    * Similar to the `docker kill` or `docker container kill` command.
    *
    * @param signal Signal to send to the container, e.g. SIGKILL, SIGINT, ..
    */
  def kill(id: Container.Id, signal: String = "SIGKILL"): F[Unit]

  /**
    * Pauses the given docker container.
    *
    * Similar to the `docker pause` or `docker container pause` commands.
    */
  def pause(id: Container.Id): F[Unit]

  /**
    * Unpauses the given docker container.
    *
    * Similar to the `docker unpause` or `docker container unpause` commands.
    */
  def unpause(id: Container.Id): F[Unit]

  /**
    * Waits until a container stops, then returns the exit code.
    *
    * Similar to the `docker container wait` command.
    */
  def await(id: Container.Id): F[ContainerExit]

  /**
    * List processes running inside a container.
    *
    * Similar to the `docker container ps` command.
    */
  def top(id: Container.Id, psArgs: Option[String] = None): F[Processes]

  /**
    * Returns filesystem header information about the given path in a container.
    */
  def stat(id: Container.Id, path: String): F[PathStat]

  /**
    * Removes the given container.
    *
    * Similar to the `docker rm` command.
    */
  def remove(id: Container.Id): F[Unit]

  /**
    * Delete stopped containers.
    *
    * Similar to the `docker container prune` command.
    */
  def prune(parameters: Parameter[Containers.PruneParameter]*): F[ContainersPruned]

}

object Containers {

  sealed trait Stream

  object Stream {
    final case object StdIn extends Stream
    final case object StdOut extends Stream
    final case object StdErr extends Stream
  }

  case class Log(stream: Stream, message: String)

  sealed trait LogParameter

  object LogParameter {

    /**
      * Return the logs as a stream.
      */
    def follow: Parameter[LogParameter] = query("follow", true)

    /**
      * Return logs from `stdout`.
      */
    def stdout: Parameter[LogParameter] = query("stdout", true)

    /**
      * Return logs from `stderr`.
      */
    def stderr: Parameter[LogParameter] = query("stderr", true)

    /**
      * Show logs since this timestamp.
      */
    def since(timestamp: ZonedDateTime): Parameter[LogParameter] = query("since", timestamp.toInstant.getEpochSecond)

    /**
      * Show logs until this timestamp.
      */
    def until(timestamp: ZonedDateTime): Parameter[LogParameter] = query("until", timestamp.toInstant.getEpochSecond)

    /**
      * Adds timestamps to every log line.
      */
    def showTimestamps: Parameter[LogParameter] = query("timestamps", true)

    /**
      * Do not add timestamps to every log line.
      */
    def hideTimestamps: Parameter[LogParameter] = query("timestamps", false)

    /**
      * Only returns `n` lines from the end of the logs.
      */
    def tail(n: Int): Parameter[LogParameter] = query("tail", n)

  }

  sealed trait ListParameter

  object ListParameter {

    def showAll: Parameter[ListParameter] = query("all", true)

    /**
      * Return this number of most recently created containers, including non-running ones.
      */
    def limit(n: Int): Parameter[ListParameter] = query("limit", n)

    /**
      * Include size information of containers in the response (`SizeRw` and `SizeRootFs`).
      */
    def withSize: Parameter[ListParameter] = query("size", true)

    /**
      * Only show containers that exited with the given exit code.
      */
    def withExitCode(exitCode: Int): Parameter[ListParameter] = filter("exited", exitCode.toString)

    def withId(id: Container.Id): Parameter[ListParameter] = filter("id", id.value)

    /**
      * Only show containers with the given label.
      */
    def withLabel(key: String): Parameter[ListParameter] = filter("label", key)

    def withLabel(key: String, value: String): Parameter[ListParameter] = filter("label", s"$key=$value")

    /**
      * Only show containers with the given name (or part of the given name).
      */
    def withName(name: String): Parameter[ListParameter] = filter("name", name)

    def withNetwork(name: String): Parameter[ListParameter] = filter("network", name)

    /**
      * Only show containers with the given status.
      */
    def withStatus(status: Container.Status): Parameter[ListParameter] = filter("status", status.name)

    /**
      * Only show containers with the given volume (volume name or mount point destination).
      */
    def withVolume(name: String): Parameter[ListParameter] = filter("volume", name)

  }

  sealed trait CreateParameter

  object CreateParameter {

    def withCmd(cmd: String): Parameter[CreateParameter] = body("Cmd", cmd)

    def withCmd(cmd: String, args: String*): Parameter[CreateParameter] = body("Cmd", Seq(cmd) ++ args)

    /**
      * Includes the given key-value pair as an environment variable set inside the container.
      */
    def withEnv(key: String, value: String): Parameter[CreateParameter] =
      body("Env", Json.arr(Json.fromString(s"$key=$value")))

    /**
      * Includes the given key-value pairs as environment variables set inside the container.
      */
    def withEnv(env: (String, String)*): Parameter[CreateParameter] = withEnv(Map(env: _*))

    /**
      * Includes the given key-value pairs as environment variables set inside the container.
      */
    def withEnv(env: Map[String, String]): Parameter[CreateParameter] =
      body("Env", Json.arr(env.map(entry => Json.fromString(s"${entry._1}=${entry._2}")).toSeq: _*))

    /**
      * Removes the given variable from the environment in the container.
      */
    def withoutEnv(key: String): Parameter[CreateParameter] =
      body("Env", Json.arr(Json.fromString(key)))

    /**
      * Arguments to containers are passed as part of `Cmd` parameters as well - this just makes that more explicit.
      */
    def withArgs(args: String*): Parameter[CreateParameter] = body("Cmd", args)

    def withExposedPort(port: Int, `type`: PortBinding.Type = PortBinding.Type.TCP): Parameter[CreateParameter] =
      body("ExposedPorts", Json.obj(s"$port/${`type`.name}" -> Json.obj()))

    def withPortBinding(binding: PortBinding): Parameter[CreateParameter] =
      body(
        "HostConfig",
        Json.obj(
          "PortBindings" -> Json.obj(
            s"${binding.privatePort}/${binding.`type`.name}" -> Json.arr(
              Json.obj(
                "HostIp" -> Json.fromString(binding.ipAddress.getOrElse("")),
                "HostPort" -> Json.fromString(binding.publicPort.map(_.toString).getOrElse(""))
              )
            )
          )
        )
      )

    /**
      * Specifies the name of the image to use when creating the container.
      */
    def withImage(name: String): Parameter[CreateParameter] = body("Image", name)

    def withImage(id: Image.Id): Parameter[CreateParameter] = body("Image", id.value)

    /**
      * Assigns the specified to the container.
      */
    def withName(name: String): Parameter[CreateParameter] = query("name", name)

    def withNetworkingDisabled: Parameter[CreateParameter] = body("NetworkDisabled", false)

    /**
      * Specifies the timeout to stop a container in seconds (default: 10 seconds).
      */
    def withStopTimeout(timeout: FiniteDuration): Parameter[CreateParameter] = body("StopTimeout", timeout.toSeconds)

    /**
      * Specifies the user that commands are run as inside the container.
      */
    def withUser(user: String): Parameter[CreateParameter] = body("User", user)

    /**
      * Specifies the working directory for commands to run in.
      */
    def withWorkingDir(path: String): Parameter[CreateParameter] = body("WorkingDir", path)

  }

  sealed trait PruneParameter

  object PruneParameter {

    /**
      * Prune containers created before this timestamp.
      */
    def until(until: ZonedDateTime): Parameter[PruneParameter] = filter("until", until.toString)

    /**
      * Prune containers with the specified label.
      */
    def withLabel(key: String): Parameter[PruneParameter] = filter("label", key)

    /**
      * Prune containers with the specified label.
      */
    def withLabel(key: String, value: String): Parameter[PruneParameter] = filter("label", s"$key=$value")

  }

}
