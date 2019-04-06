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

import fs2.Stream
import org.docker4s.models.containers.ContainerExit

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

trait ContainerRef[F[_]] {

  def rename(name: String): F[Unit]

  def start: F[Unit]

  def stop: F[Unit]

  def stop(timeout: FiniteDuration): F[Unit]

  def restart: F[Unit]

  def restart(timeout: FiniteDuration): F[Unit]

  def kill: F[Unit]

  def kill(signal: String): F[Unit]

  def pause: F[Unit]

  def unpause: F[Unit]

  /**
    * Waits until this container stops, then returns the exit code. Similar to the `docker container wait` command.
    */
  def await: F[ContainerExit]

  def remove: F[Unit]

  def logs(criteria: Criterion[Containers.LogCriterion]*): Stream[F, Containers.Log]

}
