package org.docker4s

import cats.effect.{Effect, IO}
import org.docker4s.api.{Images, System, Volumes}
import org.docker4s.models.images.Image
import org.docker4s.models.system.Event

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
//    for {
//      layers <- client.images.history(
//        Image.Id("sha256:353d7641c769b651ecaf0d72aca46b886372e3ccf15ab2a6ce8be857bae85daa"))
//    } yield {
//      println(s"Layers: ")
//      layers.foreach(println)
//    }

    for {
      volumes1 <- client.volumes.list()
      // _ <- client.volumes.remove(volumes1.volumes.head.name)
      // _ = println(s"Deleted ${volumes1.volumes.head.name}")
      pruned <- client.volumes.prune()
      volumes2 <- client.volumes.list()
    } yield {
      println("Before: " + volumes1)
      println("Pruned: " + pruned)
      println("After: " + volumes2)
    }

//    val stream = for {
//      event <- client.system.events(System.EventsCriterion.action(Event.Action.Create))
//    } yield {
//      println(s"Event: $event")
//    }
//
//    stream.compile.drain
  }

}
