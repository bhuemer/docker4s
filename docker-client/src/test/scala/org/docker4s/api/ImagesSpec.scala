package org.docker4s.api

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ImagesSpec extends FlatSpec with ClientAwareSpec with Matchers {

  "Pulling images" should "pick the latest image by default" in {
    clientResource
      .use({ client =>
        for {
          before <- client.images.list()
          _ = before should be(List.empty)

          _ <- client.images.pull("busybox").compile.drain

          after <- client.images.list()
          _ = after.size should be(1)
        } yield ()
      })
      .unsafeRunSync()
  }

}
