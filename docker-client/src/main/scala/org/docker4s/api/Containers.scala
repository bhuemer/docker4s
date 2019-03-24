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
import org.docker4s.Criterion
import org.docker4s.Criterion.query
import org.docker4s.models.containers.{Container, ContainerExit, ContainerSummary}

import scala.language.higherKinds

trait Containers[F[_]] { self =>

  /**
    * Returns a list of containers. Similar to the `docker ps` or `docker container ls` commands.
    */
  def list(): F[List[ContainerSummary]]

  def get(id: Container.Id): ContainerRef[F] = new ContainerRef[F] {

    override def start: F[Unit] = self.start(id)

    override def kill: F[Unit] = self.kill(id)

    override def kill(signal: String): F[Unit] = self.kill(id, signal)

    override def pause: F[Unit] = self.pause(id)

    override def unpause: F[Unit] = self.unpause(id)

    override def await: F[ContainerExit] = self.await(id)

    override def logs(criteria: Criterion[Containers.LogCriterion]*): Stream[F, Containers.Log] =
      self.logs(id, criteria: _*)

  }

  def start(id: Container.Id): F[Unit]

  /**
    * Kills the given docker container by sending a POSIX signal such as SIGKILL.
    * @param signal Signal to send to the container, e.g. SIGKILL, SIGINT, ..
    */
  def kill(id: Container.Id, signal: String = "SIGKILL"): F[Unit]

  /**
    * Pauses the given docker container. Similar to the `docker container pause` command.
    */
  def pause(id: Container.Id): F[Unit]

  /**
    * Unpauses the given docker container. Similar to the `docker container unpause` command.
    */
  def unpause(id: Container.Id): F[Unit]

  /**
    * Waits until a container stops, then returns the exit code. Similar to the `docker container wait` command.
    */
  def await(id: Container.Id): F[ContainerExit]

  def logs(id: Container.Id, criteria: Criterion[Containers.LogCriterion]*): Stream[F, Containers.Log]

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

}
