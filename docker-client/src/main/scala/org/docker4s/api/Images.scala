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
import org.docker4s.api.Images.ListCriterion
import org.docker4s.models.images.{Image, ImageSummary}

import scala.language.higherKinds

/**
  * Docker client methods related to managing images on the server. Similar to `docker image ...` commands.
  * @tparam F the effect type for evaluations, e.g. `IO`
  */
trait Images[F[_]] {

  /** Returns a list of images on the server. Similar to the `docker image list` or `docker images` command. */
  def list(criteria: Criterion[ListCriterion]*): F[List[ImageSummary]]

  /** Returns low-level information about an image. Similar to the `docker image inspect` command. */
  def inspect(id: Image.Id): F[Image]

}

object Images {

  // type tag for criteria used in the `list` method
  sealed trait ListCriterion

  object ListCriterion {

    /**
      * Show all images. Only images from a final layer (no children) are shown by default.
      */
    def showAll: Criterion[ListCriterion] = Criterion.Query("all", "true")

    /**
      * Show digest information as `RepoDigests` field on each image.
      */
    def showDigests: Criterion[ListCriterion] = Criterion.Query("digests", "true")

    def hideDigests: Criterion[ListCriterion] = Criterion.Query("digests", "false")

    /**
      * Show dangling images only, i.e. images without a repository name.
      *
      * By default both dangling and non-dangling images will be shown.
      */
    def showDangling: Criterion[ListCriterion] = Criterion.Filter("dangling", "true")

    def hideDangling: Criterion[ListCriterion] = Criterion.Filter("dangling", "false")

  }

}
