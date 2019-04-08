package org.docker4s

import java.time.ZonedDateTime

import cats.effect._

object DockerClientTest {

  def main(args: Array[String]): Unit = {
    import cats.effect.{ContextShift, Timer}
    import scala.concurrent.ExecutionContext.global

    implicit val cs: ContextShift[IO] = IO.contextShift(global)
    implicit val timer: Timer[IO] = IO.timer(global)
    val cf: ConcurrentEffect[IO] = implicitly[ConcurrentEffect[IO]]

    DockerClient
      .fromEnvironment(Environment.Live)(cf, global)
      .use({ client =>
        main(client)
      })
      .unsafeRunSync()
    println()
  }

  private def main(client: DockerClient[IO])(implicit cs: ContextShift[IO], timer: Timer[IO]): IO[Unit] = {
    val before = ZonedDateTime.now()
    for {
      _ <- client.images.pull(name = "mysql").map(println).compile.drain
      _ = println("Pulled mysql")

      images <- client.images.list()
      image = images.filter(image => image.repoDigests.exists(_.contains("mysql"))).head

      _ = println(s"Deleting image $image.")
      _ <- client.images.remove(image.id)
    } yield {
      println("Finished everything!")
    }
  }

}
