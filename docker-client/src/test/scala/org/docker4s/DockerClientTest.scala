package org.docker4s

import cats.effect.{Effect, IO}

object DockerClientTest {

  def main(args: Array[String]): Unit = {
    import cats.effect.{ContextShift, Timer}
    import scala.concurrent.ExecutionContext.global

    implicit val cs: ContextShift[IO] = IO.contextShift(global)
    implicit val timer: Timer[IO] = IO.timer(global)
    val cf: Effect[IO] = implicitly[Effect[IO]]

    DockerClient
      .fromEnvironment(cf, global)
      .use({ client =>
        main(client)
      })
      .unsafeRunSync()
  }

  private def main(client: DockerClient[IO]): IO[Unit] = {
    for {
      images <- client.images.list
    } yield {
      println(s"Images: ")
      images.foreach(println)
    }
  }

}
