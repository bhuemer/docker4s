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

import org.docker4s.Criterion
import org.docker4s.Criterion.filter
import org.docker4s.models.volumes.{Volume, VolumeList}

import scala.language.higherKinds

trait Volumes[F[_]] {

  /**
    * Returns volumes currently registered by the docker daemon. Similar to the `docker volume ls` command.
    */
  def list(criteria: Criterion[Volumes.ListCriterion]*): F[VolumeList]

  /**
    * Creates and registers a named volume. Similar to the `docker volume create` command.
    */
  def create(
      name: Option[String] = None,
      driver: String = "local",
      options: Map[String, String] = Map.empty,
      labels: Map[String, String] = Map.empty): F[Volume]

}

object Volumes {

  sealed trait ListCriterion

  object ListCriterion {

    /**
      * Show dangling volumes only, i.e. all volumes that are not in use by a container.
      */
    def showDangling: Criterion[ListCriterion] = filter("dangling", "true")

    /**
      * Show non-dangling volumes only, i.e. all volumes that are in use by one or mor containers.
      */
    def hideDangling: Criterion[ListCriterion] = filter("dangling", "false")

    /**
      * Show volumes with a matching driver name.
      */
    def driver(name: String): Criterion[ListCriterion] = filter("driver", name)

    /**
      * Show volumes with a label with the given name, regardless of the label's value.
      */
    def label(name: String): Criterion[ListCriterion] = filter("label", name)

    /**
      * Show volumes with the given label and value combination.
      */
    def label(name: String, value: String): Criterion[ListCriterion] = filter("label", s"$name:$value")

  }

}
