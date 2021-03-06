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

import fs2.Stream
import org.docker4s.models.containers.{Container, ContainerExit}
import org.docker4s.transport.Parameter

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

  def await: F[ContainerExit]

  def remove: F[Unit]

  def logs(criteria: Parameter[Containers.LogParameter]*): Stream[F, Containers.Log]

}

object ContainerRef {

  def apply[F[_]](containers: Containers[F], id: Container.Id): ContainerRef[F] = new ContainerRef[F] {
    override def rename(name: String): F[Unit] = containers.rename(id, name)
    override def start: F[Unit] = containers.start(id)
    override def stop: F[Unit] = containers.stop(id)
    override def stop(timeout: FiniteDuration): F[Unit] = containers.stop(id, timeout)
    override def restart: F[Unit] = containers.restart(id)
    override def restart(timeout: FiniteDuration): F[Unit] = containers.restart(id)
    override def kill: F[Unit] = containers.kill(id)
    override def kill(signal: String): F[Unit] = containers.kill(id, signal)
    override def pause: F[Unit] = containers.pause(id)
    override def unpause: F[Unit] = containers.unpause(id)
    override def await: F[ContainerExit] = containers.await(id)
    override def remove: F[Unit] = containers.remove(id)
    override def logs(criteria: Parameter[Containers.LogParameter]*): Stream[F, Containers.Log] =
      containers.logs(id, criteria: _*)
  }

}
