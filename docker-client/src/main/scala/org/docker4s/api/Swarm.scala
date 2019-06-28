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

import org.docker4s.models.swarm.Node

import scala.language.higherKinds

trait Swarm[F[_]] {

  /**
    *
    * Similar to the `docker swarm init` command.
    *
    * @param listenAddress
    *
    * @return the identifier of this current node in the swarm
    */
  def init(listenAddress: String, forceNewCluster: Option[Boolean] = None): F[Node.Id]

  /**
    * Leaves the swarm.
    *
    * Similar to the `docker swarm leave` command.
    *
    * @param force If true, forces to leave the swarm even if this is the last manager or that it will break the cluster.
    */
  def leave(force: Option[Boolean] = None): F[Unit]

}

object Swarm {}
