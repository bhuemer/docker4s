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

import com.typesafe.scalalogging.LazyLogging
import fs2.Stream
import org.docker4s.models.images.ImageSummary
import org.docker4s.syntax._
import org.scalatest.Matchers

class ImagesIntegrationTest extends ClientSpec with Matchers with LazyLogging {

  "Pulling images" should "pick the latest image by default" given { client =>
    def busyboxImage(images: List[ImageSummary]): Option[ImageSummary] =
      images.find(_.repoTags.contains("busybox:latest"))

    for {
      imagesBefore <- client.images.list()

      statusAndDigest <- client.images.pull("busybox").result
      _ = logger.info(s"Result from pulling busybox: $statusAndDigest")

      imagesAfter <- client.images.list()
    } yield {
      (busyboxImage(imagesBefore), busyboxImage(imagesAfter)) match {
        // Case #1: busybox:latest didn't need to be updated.
        case (Some(busybox1), Some(busybox2)) if busybox1 == busybox2 =>
          statusAndDigest.status should be(Some("Image is up to date for busybox:latest"))
          List(s"busybox@${statusAndDigest.digest.get}") should be(busybox1.repoDigests)
          List(s"busybox@${statusAndDigest.digest.get}") should be(busybox2.repoDigests)

        // Case #2: busybox:latest existed before already but has been updated since.
        case (Some(busybox1), Some(busybox2)) =>
          statusAndDigest.status should be(Some("Downloaded newer image for busybox:latest"))
          List(s"busybox@${statusAndDigest.digest.get}") should be(busybox2.repoDigests)

        // Case #3: busybox:latest was never downloaded before.
        case (None, Some(busybox2)) =>
          statusAndDigest.status should be(Some("Downloaded newer image for busybox:latest"))
          List(s"busybox@${statusAndDigest.digest.get}") should be(busybox2.repoDigests)
      }
    }
  }

  "The client" should "support savind images to and loading images from TAR archives" given { client =>
    for {
      _ <- client.images.pull(name = "busybox").compile.drain

      busybox <- client.images.inspect(name = "busybox")

      saved <- client.images.save(busybox.id).compile.toList
      _ = logger.info(s"Saved image in memory. Size: ${saved.size} bytes.")

      // Delete it so that we can reload it from the saved version.
      _ <- client.images.remove(busybox.id, force = true)

      loaded <- client.images.load(Stream.emits(saved))
      _ = loaded.id should be(busybox.id)

      images <- client.images.list()
      _ = images.map(_.id) should contain(busybox.id)
    } yield ()
  }

}
