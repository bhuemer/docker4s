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
import org.docker4s.api.Criterion.{filter, query}
import org.docker4s.models.containers._

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

trait Containers[F[_]] { self =>

  def get(id: Container.Id): ContainerRef[F] = new ContainerRef[F] {

    override def rename(name: String): F[Unit] = self.rename(id, name)

    override def start: F[Unit] = self.start(id)

    override def stop: F[Unit] = self.stop(id)

    override def stop(timeout: FiniteDuration): F[Unit] = self.stop(id, timeout)

    override def restart: F[Unit] = self.restart(id)

    override def restart(timeout: FiniteDuration): F[Unit] = self.restart(id, timeout)

    override def kill: F[Unit] = self.kill(id)

    override def kill(signal: String): F[Unit] = self.kill(id, signal)

    override def pause: F[Unit] = self.pause(id)

    override def unpause: F[Unit] = self.unpause(id)

    override def await: F[ContainerExit] = self.await(id)

    override def remove: F[Unit] = self.remove(id)

    override def logs(criteria: Criterion[Containers.LogCriterion]*): Stream[F, Containers.Log] =
      self.logs(id, criteria: _*)

  }

  /**
    * Returns differences in the given container's file system since it was started.
    */
  def diff(id: Container.Id): F[List[ContainerChange]]

  /**
    * Returns a list of containers. Similar to the `docker ps` or `docker container ls` commands.
    */
  def list(criteria: Criterion[Containers.ListCriterion]*): F[List[ContainerSummary]]

  def logs(id: Container.Id, criteria: Criterion[Containers.LogCriterion]*): Stream[F, Containers.Log]

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

  sealed trait LogCriterion

  object LogCriterion {

    /**
      * Return the logs as a stream.
      */
    def follow: Criterion[LogCriterion] = query("follow", true)

    /**
      * Return logs from `stdout`.
      */
    def stdout: Criterion[LogCriterion] = query("stdout", true)

    /**
      * Return logs from `stderr`.
      */
    def stderr: Criterion[LogCriterion] = query("stderr", true)

    /**
      * Show logs since this timestamp.
      */
    def since(timestamp: ZonedDateTime): Criterion[LogCriterion] = query("since", timestamp.toInstant.getEpochSecond)

    /**
      * Show logs until this timestamp.
      */
    def until(timestamp: ZonedDateTime): Criterion[LogCriterion] = query("until", timestamp.toInstant.getEpochSecond)

    /**
      * Adds timestamps to every log line.
      */
    def showTimestamps: Criterion[LogCriterion] = query("timestamps", true)

    /**
      * Do not add timestamps to every log line.
      */
    def hideTimestamps: Criterion[LogCriterion] = query("timestamps", false)

    /**
      * Only returns `n` lines from the end of the logs.
      */
    def tail(n: Int): Criterion[LogCriterion] = query("tail", n)

  }

  sealed trait ListCriterion

  object ListCriterion {

    def showAll: Criterion[ListCriterion] = query("all", true)

    /**
      * Return this number of most recently created containers, including non-running ones.
      */
    def limit(n: Int): Criterion[ListCriterion] = query("limit", n)

    def withSize: Criterion[ListCriterion] = query("size", true)

    /**
      * Only show containers that exited with the given exit code.
      */
    def exited(exitCode: Int): Criterion[ListCriterion] = filter("exited", exitCode.toString)

    /**
      * Only show containers with the given name (or part of the given name).
      */
    def name(name: String): Criterion[ListCriterion] = filter("name", name)

    /**
      * Only show containers with the given status.
      */
    def status(status: Container.Status): Criterion[ListCriterion] = filter("status", status.name)

    /**
      *
      */
    def volume(name: String): Criterion[ListCriterion] = filter("volume", name)

  }

}
