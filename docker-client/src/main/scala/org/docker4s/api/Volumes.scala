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

import org.docker4s.api.Parameter.filter
import org.docker4s.models.volumes.{Volume, VolumeList, VolumesPruned}

import scala.language.higherKinds

trait Volumes[F[_]] {

  /**
    * Returns volumes configured in the docker host. Similar to the `docker volume ls` command.
    */
  def list(parameters: Parameter[Volumes.ListCriterion]*): F[VolumeList]

  /**
    * Creates and registers a named volume. Similar to the `docker volume create` command.
    *
    * @param name The new volume's name. If not specified, Docker generates a name.
    * @param driver Name of the volume driver to use (`local` by default)
    * @param options A mapping of driver options and values. These options are passed directly to the driver and are
    *                driver specific.
    * @param labels User-defined key/value metadata.
    *
    * @return The volume that was created.
    */
  def create(
      name: Option[String] = None,
      driver: Option[String] = None,
      options: Map[String, String] = Map.empty,
      labels: Map[String, String] = Map.empty): F[Volume]

  /**
    * Returns volume information by name. Similar to the `docker volume inspect` command.
    */
  def inspect(name: String): F[Volume]

  /**
    * Removes the given volume. Similar to the `docker volume rm` command.
    * @param name Name of the volume to remove
    * @param force Force the removal of the volume
    */
  def remove(name: String, force: Boolean = false): F[Unit]

  /**
    * Removes unused volumes. Similar to the `docker volume prune` command.
    */
  def prune(): F[VolumesPruned]

}

object Volumes {

  sealed trait ListCriterion

  object ListCriterion {

    /**
      * Show dangling volumes only, i.e. all volumes that are not in use by a container.
      */
    def showDangling: Parameter[ListCriterion] = filter("dangling", "true")

    /**
      * Show non-dangling volumes only, i.e. all volumes that are in use by one or mor containers.
      */
    def hideDangling: Parameter[ListCriterion] = filter("dangling", "false")

    /**
      * Show volumes with a matching driver name.
      */
    def withDriver(name: String): Parameter[ListCriterion] = filter("driver", name)

    /**
      * Show volumes with a label with the given name, regardless of the label's value.
      */
    def withLabel(name: String): Parameter[ListCriterion] = filter("label", name)

    /**
      * Show volumes with the given label and value combination.
      */
    def withLabel(name: String, value: String): Parameter[ListCriterion] = filter("label", s"$name:$value")

    /**
      * Show volumes with the given name or part of the given name.
      */
    def withName(name: String): Parameter[ListCriterion] = filter("name", name)

  }

}
