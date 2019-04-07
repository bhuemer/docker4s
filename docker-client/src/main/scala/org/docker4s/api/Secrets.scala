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

import org.docker4s.models.secrets.{Secret, SecretCreated}

import scala.language.higherKinds

trait Secrets[F[_]] {

  /**
    * Returns secrets configured in the docker host. Similar to the `docker secret ls` command.
    */
  def list(criteria: Criterion[Secrets.ListCriterion]*): F[List[Secret]]

  /**
    * Returns detailed information for the given secret. Similar to the `docker secret inspect` command.
    */
  def inspect(id: Secret.Id): F[Secret]

  /**
    * Creates a new secret with the given data in the docker host. Similar to the `docker secret create` command.
    */
  def create(name: String, data: Array[Byte]): F[SecretCreated]

  def update(secret: Secret, labels: Map[String, String]): F[Unit] =
    update(secret.id, secret.version, secret.spec.name, labels)

  /**
    * Updates a secret in the docker host. Currently only label updates are supported.
    * @param version the current version of the secret, for concurrent updates
    * @param name the current name of the secret
    * @param labels
    */
  def update(id: Secret.Id, version: Long, name: String, labels: Map[String, String]): F[Unit]

  /**
    * Removes the given secret from the docker host. Similar to the `docker secret rm` command.
    */
  def remove(id: Secret.Id): F[Unit]

}

object Secrets {

  sealed trait ListCriterion

  object ListCriterion {}

}
