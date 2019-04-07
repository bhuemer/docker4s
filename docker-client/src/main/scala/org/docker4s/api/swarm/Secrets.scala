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
package org.docker4s.api.swarm

import org.docker4s.api.Criterion
import org.docker4s.api.Criterion.filter
import org.docker4s.models.secrets.Secret

import scala.language.higherKinds

trait Secrets[F[_]] {

  /**
    * Lists all secrets.
    */
  def list(criteria: Criterion[Secrets.ListCriterion]*): F[List[Secret]]

  def inspect(id: Secret.Id): F[Secret]

  /**
    * Deletes the secret with the given ID.
    */
  def delete(id: Secret.Id): F[Unit]

}

object Secrets {

  sealed trait ListCriterion

  object ListCriterion {

    def name(name: String): Criterion[ListCriterion] = filter("name", name)

    def label(key: String): Criterion[ListCriterion] = filter("label", key)

    def label(key: String, value: String): Criterion[ListCriterion] = filter("label", s"$key=$value")

  }

}
