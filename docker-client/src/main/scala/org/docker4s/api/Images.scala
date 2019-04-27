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

import java.net.URI
import java.time.ZonedDateTime

import fs2.Stream
import org.docker4s.models.images._
import org.docker4s.transport.Parameter
import org.docker4s.transport.Parameter.{filter, query, queryMap}

import scala.language.higherKinds

/**
  * Docker client methods related to managing images on the server. Similar to `docker image ...` commands.
  * @tparam F the effect type for evaluations, e.g. `IO`
  */
trait Images[F[_]] {

  def get(id: Image.Id): ImageRef[F] = ImageRef(this, id)

  /**
    * Returns a list of images on the server.
    *
    * Similar to the `docker image list` or `docker images` command.
    *
    * @example {{{
    * import org.docker4s.api.Images.ListParameter._
    * import org.docker4s.models.images.{Image, ImageSummary}
    *
    * val program = for {
    *   images <- client.images.list(showDigests)
    *   _ = images.foreach({ image =>
    *     println(s"Image: $image")
    *   })
    * } yield ()
    * }}}
    */
  def list(parameters: Parameter[Images.ListParameter]*): F[List[ImageSummary]]

  /**
    * Saves one or more images to a TAR archive. Similar to the `docker image save` command.
    */
  def save(id: Image.Id, ids: Image.Id*): Stream[F, Byte] = save(Seq(id) ++ ids.toSeq)

  /**
    * Saves one or more images to a TAR archive. Similar to the `docker image save` command.
    */
  def save(id: Seq[Image.Id]): Stream[F, Byte]

  def load(image: Stream[F, Byte], quiet: Boolean = false): F[ImageLoaded]

  /**
    *
    */
  def build(image: Stream[F, Byte], name: Option[String] = None): Stream[F, BuildEvent]

  /**
    * Returns low-level information about an image.
    *
    * Similar to the `docker image inspect` command.
    */
  def inspect(id: Image.Id): F[Image] = inspect(id.value)

  /**
    * Returns low-level information about an image.
    *
    * Similar to the `docker image inspect` command.
    */
  def inspect(name: String): F[Image]

  /**
    * Pulls the given docker container image.
    */
  def pull(name: String, tag: Option[String] = None): Stream[F, PullEvent]

  /**
    * Removes the given image, along with any untagged parent images.
    *
    * Similar to the `docker image rm` command.
    *
    * @param id ID of the image you want to remove
    * @param force Remove the image even if it is being used by stopped containers or has other tags
    * @param prune Delete untagged parent images
    */
  def remove(id: Image.Id, force: Boolean = false, prune: Boolean = true): F[ImagesRemoved]

  /**
    * Returns the history of the image, i.e. its parent layers.
    *
    * Similar to the `docker history` command.
    */
  def history(id: Image.Id): F[List[ImageHistory]]

  def tag(id: Image.Id, repo: String, tag: Option[String] = None): F[Unit]

  /**
    * Removes all dangling images.
    */
  def prune(parameters: Parameter[Images.PruneParameter]*): F[ImagesPruned]

}

object Images {

  // type tag for criteria used in the `list` method
  sealed trait ListParameter

  object ListParameter {

    /**
      * Show all images. Only images from a final layer (no children) are shown by default.
      */
    def showAll: Parameter[ListParameter] = query("all", "true")

    /**
      * Show digest information as `RepoDigests` field on each image.
      */
    def showDigests: Parameter[ListParameter] = query("digests", "true")

    def hideDigests: Parameter[ListParameter] = query("digests", "false")

    /**
      * Show dangling images only, i.e. images without a repository name.
      *
      * By default both dangling and non-dangling images will be shown.
      */
    def showDangling: Parameter[ListParameter] = filter("dangling", "true")

    def hideDangling: Parameter[ListParameter] = filter("dangling", "false")

    def withAfter(image: String): Parameter[ListParameter] = filter("after", image)

    def withLabel(key: String): Parameter[ListParameter] = filter("label", key)

    def withLabel(key: String, value: String): Parameter[ListParameter] = filter("label", s"$key=$value")

    def withBefore(image: String): Parameter[ListParameter] = filter("before", image)

  }

  sealed trait BuildParameter

  object BuildParameter {

    def withExtraHost(extraHost: String): Parameter[BuildParameter] = query("extrahosts", extraHost)

    def withSecurityOpt(securityOpt: String): Parameter[BuildParameter] = query("securityopt", securityOpt)

    def withTag(tag: String): Parameter[BuildParameter] = query("t", tag)

    /**
      * Suppresses verbose build output.
      */
    def withOutputSuppressed: Parameter[BuildParameter] = query("q", "1")

    def withRemote(uri: URI): Parameter[BuildParameter] = query("remote", uri.toString)

    def withNocache(nocache: Boolean): Parameter[BuildParameter] = query("nocache", nocache)

    /**
      * Specifies the value for a build argument/variable.
      *
      * @see https://docs.docker.com/engine/reference/builder/#arg
      */
    def withBuildArg(key: String, value: String): Parameter[BuildParameter] =
      queryMap("buildargs", key, value)

    def withLabel(key: String, value: String): Parameter[BuildParameter] =
      queryMap("labels", key, value)

    /**
      * Sets the networking mode for the run commands during build. Supported standard values are: `bridge`, `host`,
      * `none`, and `container:<name|id>`. Any other value is taken as a custom network's name to which this container
      * should connect to.
      */
    def withNetworkMode(networkMode: String): Parameter[BuildParameter] =
      query("networkmode", networkMode)

  }

  sealed trait PruneParameter

  object PruneParameter {

    /**
      * Prune images created before this timestamp.
      */
    def until(until: ZonedDateTime): Parameter[PruneParameter] = filter("until", until.toEpochSecond.toString)

    def withLabel(key: String): Parameter[PruneParameter] = filter("label", key)

    def withLabel(key: String, value: String): Parameter[PruneParameter] = filter("label", s"$key=$value")

  }

}
