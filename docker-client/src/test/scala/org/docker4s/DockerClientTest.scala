package org.docker4s

import java.io.{File, FileOutputStream}

import cats.effect._
import org.docker4s.models.images.Image

import scala.concurrent.ExecutionContext

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
    println()
  }

  private def main(client: DockerClient[IO])(implicit cs: ContextShift[IO], timer: Timer[IO]): IO[Unit] = {
    client.images
      .prune()
      .map({ pruned =>
        println(s"Pruned $pruned")
      })

//    for {
//      containers1 <- client.containers.list()
//      container = containers1.head
//
//      _ <- client.containers.pause(containers1.head.id)
//      _ = println(s"Paused container with the ID ${container.id.value}.")
//
//      _ <- client.containers.unpause(container.id)
//      _ = println(s"Unpaused container with the ID ${container.id.value}.")
//
//      _ = println(s"Restarting container ${container.id.value}.")
//      _ <- client.containers.restart(container.id)
//      _ = println(s"Restarted container ${container.id.value}.")
////      _ = println(s"Waiting until container ${container.id.value} is finished.")
//
//      // _ <- IO.sleep(FiniteDuration(5, "s"))
//
//      containers2 <- client.containers.list()
//
//      pruned <- client.containers.prune()
//    } yield {
//      containers1.foreach({ container =>
//        println(s"Before: $container")
//      })
//
//      containers2.foreach({ container =>
//        println(s"After: $container")
//      })
//
//      println(s"Pruned: $pruned")
//    }
  }

}
