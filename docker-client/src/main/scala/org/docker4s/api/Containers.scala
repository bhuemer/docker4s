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
package org.docker4s.api

import java.time.ZonedDateTime

import fs2.Stream
import org.docker4s.api.Parameter.{filter, query}
import org.docker4s.models.containers._

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

trait Containers[F[_]] {

  def get(id: Container.Id): ContainerRef[F] = ContainerRef(this, id)

  /**
    * Returns differences in the given container's file system since it was started.
    *
    * Similar to the `docker container diff` command.
    */
  def diff(id: Container.Id): F[List[ContainerChange]]

  /**
    * Returns a list of containers.
    *
    * Similar to the `docker ps` or `docker container ls` commands.
    */
  def list(criteria: Parameter[Containers.ListParameter]*): F[List[ContainerSummary]]

  def logs(id: Container.Id, criteria: Parameter[Containers.LogParameter]*): Stream[F, Containers.Log]

  /**
    * Renames the given Docker container.
    */
  def rename(id: Container.Id, newName: String): F[Unit]

  def create(image: String): F[ContainerCreated]

  def start(id: Container.Id): F[Unit]

  /**
    * Stops the given container. Similar to the `docker stop` command.
    * @param timeout Amount of time to give the container to stop before killing it. Defaults to 10 seconds.
    */
  def stop(id: Container.Id, timeout: FiniteDuration = FiniteDuration(10, "s")): F[Unit]

  /**
    * Restart the given container. Similar to the `docker restart` command.
    * @param timeout Amount of time to give the container to stop before killing it. Defaults to 10 seconds.
    */
  def restart(id: Container.Id, timeout: FiniteDuration = FiniteDuration(10, "s")): F[Unit]

  /**
    * Kills the given docker container. Similar to the `docker kill` or `docker container kill` command.
    * @param signal Signal to send to the container, e.g. SIGKILL, SIGINT, ..
    */
  def kill(id: Container.Id, signal: String = "SIGKILL"): F[Unit]

  /**
    * Pauses the given docker container. Similar to the `docker pause` or `docker container pause` commands.
    */
  def pause(id: Container.Id): F[Unit]

  /**
    * Unpauses the given docker container. Similar to the `docker unpause` or `docker container unpause` commands.
    */
  def unpause(id: Container.Id): F[Unit]

  /**
    * Waits until a container stops, then returns the exit code. Similar to the `docker container wait` command.
    */
  def await(id: Container.Id): F[ContainerExit]

  /**
    * List processes running inside a container. Similar to the `docker container ps` command.
    */
  def top(id: Container.Id, psArgs: Option[String] = None): F[Processes]

  /**
    * Removes the given container. Similar to the `docker rm` command.
    */
  def remove(id: Container.Id): F[Unit]

  /**
    * Delete stopped containers. Similar to the `docker container prune` command.
    */
  def prune(): F[ContainersPruned]

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

    def withSize: Parameter[ListParameter] = query("size", true)

    /**
      * Only show containers that exited with the given exit code.
      */
    def withExitCode(exitCode: Int): Parameter[ListParameter] = filter("exited", exitCode.toString)

    /**
      * Only show containers with the given name (or part of the given name).
      */
    def withName(name: String): Parameter[ListParameter] = filter("name", name)

    /**
      * Only show containers with the given status.
      */
    def withStatus(status: Container.Status): Parameter[ListParameter] = filter("status", status.name)

    /**
      *
      */
    def withVolume(name: String): Parameter[ListParameter] = filter("volume", name)

  }

}
