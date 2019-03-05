package org.docker4s.transport.unix

import cats.effect.{ConcurrentEffect, IO}
import io.circe.Json
import org.http4s.{Header, Method, Request, Uri}

object DockerSocketClientTest {

  def main(args: Array[String]): Unit = {
    import cats.effect.{ContextShift, Timer}
    import scala.concurrent.ExecutionContext.global
    import org.http4s.circe.jsonDecoder

    implicit val cs: ContextShift[IO] = IO.contextShift(global)
    implicit val timer: Timer[IO] = IO.timer(global)
    val cf: ConcurrentEffect[IO] = implicitly[ConcurrentEffect[IO]]

    val response = DomainSocketClient()(cf, global)
      .use({ client =>
        client
          .fetchAs[Json](
            Request[IO]()
              .withMethod(Method.GET)
              .withHeaders(Header("Host", "localhost"))
              .withUri(Uri.unsafeFromString("http://localhost/info")))
      })
      .unsafeRunSync()

    println("Response: " + response)
  }

}
