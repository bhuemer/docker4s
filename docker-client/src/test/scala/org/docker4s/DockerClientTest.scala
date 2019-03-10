package org.docker4s
import java.time.ZonedDateTime

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
        val stream = for {
          event <- client.system.events(until = Some(ZonedDateTime.now().plusSeconds(60)))
        } yield {
          println(s"[${Thread.currentThread().getName}] Event: $event")
        }

        stream.compile.drain
      })
      .unsafeRunSync()
  }

}
