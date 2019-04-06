package org.docker4s

import cats.effect._
import org.docker4s.api.Containers
import org.docker4s.models.containers.Container

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
    client.containers
      .await(Container.Id("01076bf6717e51f3fac194ea0b6242345132fb9287ce4097fb3aff64ba9605a"))
      .map({ response =>
        println(s"Response $response")
      })
  }

}
