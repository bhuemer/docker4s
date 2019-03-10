package org.docker4s

import cats.effect.{ConcurrentEffect, IO}
import org.docker4s.models.images.Image

object DockerClientTest {

  def main(args: Array[String]): Unit = {
    import cats.effect.{ContextShift, Timer}
    import scala.concurrent.ExecutionContext.global

    implicit val cs: ContextShift[IO] = IO.contextShift(global)
    implicit val timer: Timer[IO] = IO.timer(global)
    val cf: ConcurrentEffect[IO] = implicitly[ConcurrentEffect[IO]]

    DockerClient
      .fromEnvironment(cf, global)
      .use({ client =>
        main(client)
      })
      .unsafeRunSync()
  }

  private def main(client: DockerClient[IO]): IO[Unit] = {
    for {
      info <- client.system.info
    } yield {
      println(s"Info: $info")
    }
  }

}
