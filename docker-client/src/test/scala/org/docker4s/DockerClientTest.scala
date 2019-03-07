package org.docker4s
import cats.effect.{ConcurrentEffect, IO}

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
        client.info.flatMap({ info =>
          IO.delay(println(s"Info: $info"))
        })
      })
      .unsafeRunSync()
  }

}
