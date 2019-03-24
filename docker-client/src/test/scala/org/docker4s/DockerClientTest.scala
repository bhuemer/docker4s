package org.docker4s

import fs2.Stream
import cats.effect.{Effect, IO}
import org.docker4s.api.Containers
import org.docker4s.models.containers.Container

object DockerClientTest {

  def main(args: Array[String]): Unit = {
    import cats.effect.{ContextShift, Timer}
    import scala.concurrent.ExecutionContext.global

    implicit val cs: ContextShift[IO] = IO.contextShift(global)
    implicit val timer: Timer[IO] = IO.timer(global)
    val cf: Effect[IO] = implicitly[Effect[IO]]

    val lines = DockerClient
      .fromEnvironment(cf, global)
      .use({ client =>
        main(client)
      })
      .unsafeRunSync()
    println()
  }

  private def main(client: DockerClient[IO]): IO[Unit] = {
    for {
      containers1 <- client.containers.list()
      container = containers1.head

      _ <- client.containers.pause(containers1.head.id)
      _ = println(s"Paused container with the ID ${container.id.value}.")

      _ <- client.containers.unpause(container.id)
      _ = println(s"Unpaused container with the ID ${container.id.value}.")

      _ = println(s"Killing container ${container.id.value}.")
      _ <- client.containers.kill(container.id)
//      _ = println(s"Waiting until container ${container.id.value} is finished.")

      containers2 <- client.containers.list()
    } yield {
      containers1.foreach({ container =>
        println(s"Before: $container")
      })

      containers2.foreach({ container =>
        println(s"After: $container")
      })
    }
  }

}
