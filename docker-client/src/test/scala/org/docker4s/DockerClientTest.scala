package org.docker4s

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

  private def main(client: DockerClient[IO]): IO[List[Containers.Log]] = {
//    for {
//      layers <- client.images.history(
//        Image.Id("sha256:353d7641c769b651ecaf0d72aca46b886372e3ccf15ab2a6ce8be857bae85daa"))
//    } yield {
//      println(s"Layers: ")
//      layers.foreach(println)
//    }

//    for {
//      volumes1 <- client.volumes.list()
//      // _ <- client.volumes.remove(volumes1.volumes.head.name)
//      // _ = println(s"Deleted ${volumes1.volumes.head.name}")
//      pruned <- client.volumes.prune()
//      volumes2 <- client.volumes.list()
//    } yield {
//      println("Before: " + volumes1)
//      println("Pruned: " + pruned)
//      println("After: " + volumes2)
//    }

    import org.docker4s.api.Containers.LogCriterion._

    val stream = for {
      line <- client.containers.get(Container.Id("12f1e1e9aff4")).logs(stdout, stderr, follow, showTimestamps)
    } yield {
      line.stream match {
        case Containers.Stream.StdOut => java.lang.System.out.println(">>> " + line.message)
        case Containers.Stream.StdErr => java.lang.System.err.println(">>> " + line.message)
        case _                        =>
      }

      line
    }

    stream.compile.toList
  }

}
