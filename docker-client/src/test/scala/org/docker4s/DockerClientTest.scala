package org.docker4s

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
    for {
      created <- client.networks.create("network-2")

      containers <- client.containers.list()
      container = containers.head

      _ <- client.networks.connect(created.id, container.id)
      _ = println(s"Connected $container to network ${created.id}.")

      _ <- client.networks.disconnect(created.id, container.id)
      _ = println(s"Disconnected $container from network ${created.id}.")
    } yield {
      println("Finished everything!")
    }
  }

}
