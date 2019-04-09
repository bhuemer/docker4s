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
import org.docker4s.api.Criterion.{filter, query}
import org.docker4s.models.images._

import scala.language.higherKinds

/**
  * Docker client methods related to managing images on the server. Similar to `docker image ...` commands.
  * @tparam F the effect type for evaluations, e.g. `IO`
  */
trait Images[F[_]] {

  def get(id: Image.Id): ImageRef[F] = ImageRef(this, id)

  /** Returns a list of images on the server. Similar to the `docker image list` or `docker images` command. */
  def list(criteria: Criterion[Images.ListCriterion]*): F[List[ImageSummary]]

  /**
    * Saves one or more images to a TAR archive. Similar to the `docker image save` command.
    */
  def save(id: Image.Id, ids: Image.Id*): Stream[F, Byte] = save(Seq(id) ++ ids.toSeq)

  /**
    * Saves one or more images to a TAR archive. Similar to the `docker image save` command.
    */
  def save(id: Seq[Image.Id]): Stream[F, Byte]

  /** Returns low-level information about an image. Similar to the `docker image inspect` command. */
  def inspect(id: Image.Id): F[Image]

  /**
    * Pulls the given docker container image.
    */
  def pull(name: String, tag: Option[String] = None): Stream[F, PullEvent]

  /**
    * Removes the given image, along with any untagged parent images.
    *
    * Similar to the `docker image rm` command.
    */
  def remove(id: Image.Id, force: Boolean = false, noprune: Boolean = false): F[ImagesRemoved]

  /** Returns the history of the image, i.e. its parent layers. Similar to the `docker history` command. */
  def history(id: Image.Id): F[List[ImageHistory]]

  /**
    * Removes all dangling images.
    */
  // TODO: Allow filtering of what to prune.
  def prune(): F[ImagesPruned]

}

object Images {

  // type tag for criteria used in the `list` method
  sealed trait ListCriterion

  object ListCriterion {

    /**
      * Show all images. Only images from a final layer (no children) are shown by default.
      */
    def showAll: Criterion[ListCriterion] = query("all", "true")

    /**
      * Show digest information as `RepoDigests` field on each image.
      */
    def showDigests: Criterion[ListCriterion] = query("digests", "true")

    def hideDigests: Criterion[ListCriterion] = query("digests", "false")

    /**
      * Show dangling images only, i.e. images without a repository name.
      *
      * By default both dangling and non-dangling images will be shown.
      */
    def showDangling: Criterion[ListCriterion] = filter("dangling", "true")

    def hideDangling: Criterion[ListCriterion] = filter("dangling", "false")

  }

}
