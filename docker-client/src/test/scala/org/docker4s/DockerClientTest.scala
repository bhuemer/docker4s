package org.docker4s

import java.time.{Duration, ZonedDateTime}

import cats.effect._
import org.docker4s.api.Containers
import org.docker4s.models.containers.Container
import org.docker4s.models.networks.Network

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
      _ <- client.images.pull(name = "mysql").take(10).map(println).compile.drain
      _ = println("Pulled mysql")
    } yield {
      println("Finished everything!")
    }
  }

}
