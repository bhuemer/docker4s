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
package org.docker4s

import org.docker4s.api.{Containers, Execs, Images, Networks, Secrets, System, Volumes}

import scala.language.higherKinds

/**
  * Allows you to communicate with a docker daemon.
  * @tparam F the effect type for evaluations, e.g. `IO`
  */
trait DockerClient[F[_]] {

  /**
    * Returns an object for managing containers on the server.
    */
  def containers: Containers[F]

  /**
    * Returns an object for executing commands in containers.
    */
  def execs: Execs[F]

  /**
    * Returns an object for managing images on the server.
    */
  def images: Images[F]

  /**
    * Returns an object for managing networks on the docker host.
    */
  def networks: Networks[F]

  /**
    * Returns an object for managing secrets on the docker host.
    */
  def secrets: Secrets[F]

  /**
    * Returns an object for inspecting the system on the docker host.
    */
  def system: System[F]

  /**
    * Returns an object for managing volumes on the docker host.
    */
  def volumes: Volumes[F]

}
