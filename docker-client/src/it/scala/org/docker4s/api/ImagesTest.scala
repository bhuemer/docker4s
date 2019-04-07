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

import cats.effect.IO
import org.docker4s.models.images.{ImageSummary, PullEvent}
import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ImagesTest extends ClientSpec with Matchers {

  "Pulling images" should "pick the latest image by default" given { client =>
    def pull(name: String): IO[(Option[String], Option[String])] = {
      client.images
        .pull(name)
        .compile
        .fold((Option.empty[String], Option.empty[String]))({
          case ((status, _), PullEvent.Digest(digest)) => (status, Some(digest))
          case ((_, digest), PullEvent.Status(status)) => (Some(status), digest)
          case (previous, _)                           => previous
        })
    }

    def busyboxImage(images: List[ImageSummary]): Option[ImageSummary] =
      images.find(_.repoTags.contains("busybox:latest"))

    for {
      imagesBefore <- client.images.list()

      statusAndDigest <- pull("busybox")

      imagesAfter <- client.images.list()
    } yield {
      (busyboxImage(imagesBefore), busyboxImage(imagesAfter)) match {
        // Case #1: busybox:latest didn't need to be updated.
        case (Some(busybox1), Some(busybox2)) if busybox1 == busybox2 =>
          statusAndDigest._1.get should be("Image is up to date for busybox:latest")
          List(s"busybox@${statusAndDigest._2.get}") should be(busybox1.repoDigests)
          List(s"busybox@${statusAndDigest._2.get}") should be(busybox2.repoDigests)

        // Case #2: busybox:latest existed before already but has been updated since.
        case (Some(busybox1), Some(busybox2)) =>
          statusAndDigest._1.get should be("Downloaded newer image for busybox:latest")
          List(s"busybox@${statusAndDigest._2.get}") should be(busybox2.repoDigests)

        // Case #3: busybox:latest was never downloaded before.
        case (None, Some(busybox2)) =>
          statusAndDigest._1.get should be("Downloaded newer image for busybox:latest")
          List(s"busybox@${statusAndDigest._2.get}") should be(busybox2.repoDigests)
      }
    }
  }

}
