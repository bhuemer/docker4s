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

import org.docker4s.api.Criterion.filter
import org.docker4s.models.containers.Container
import org.docker4s.models.networks.{Network, NetworkCreated, NetworksPruned}

import scala.language.higherKinds

trait Networks[F[_]] {

  /**
    * Returns the list of networks configured in the docker host.
    */
  def list(criteria: Criterion[Networks.ListCriterion]*): F[List[Network]]

  /**
    * Returns the information config for the given network.
    */
  def inspect(id: Network.Id): F[Network]

  /**
    * Removes the given network from the docker host.
    */
  def remove(id: Network.Id): F[Unit]

  def create(name: String): F[NetworkCreated]

  def connect(network: Network.Id, container: Container.Id): F[Unit]

  /**
    * Disconnects the container from the given network in the docker host.
    */
  def disconnect(network: Network.Id, container: Container.Id, force: Boolean = false): F[Unit]

  /**
    * Removes unused networks from the docker host.
    */
  def prune(): F[NetworksPruned]

}

object Networks {

  sealed trait ListCriterion

  object ListCriterion {

    /**
      * Matches a network's driver.
      */
    def driver(name: String): Criterion[ListCriterion] = filter("driver", name)

    /**
      * Matches all or part of a network ID.
      */
    def id(id: String): Criterion[ListCriterion] = filter("id", id)

    def id(id: Network.Id): Criterion[ListCriterion] = filter("id", id.value)

    /**
      * Matches all or part of a network name.
      */
    def name(name: String): Criterion[ListCriterion] = filter("name", name)

    def scope(scope: Network.Scope): Criterion[ListCriterion] = scope match {
      case Network.Scope.Swarm  => filter("scope", "swarm")
      case Network.Scope.Local  => filter("scope", "local")
      case Network.Scope.Global => filter("scope", "global")
    }

    def custom: Criterion[ListCriterion] = filter("type", "custom")

    def builtin: Criterion[ListCriterion] = filter("type", "builtin")

  }

}
