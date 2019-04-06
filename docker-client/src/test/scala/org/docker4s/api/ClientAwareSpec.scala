package org.docker4s.api
import cats.effect._
import org.docker4s.DockerClient

import scala.concurrent.ExecutionContext.global

trait ClientAwareSpec {

  protected def clientResource: Resource[IO, DockerClient[IO]] = {
    implicit val cs: ContextShift[IO] = IO.contextShift(global)
    implicit val timer: Timer[IO] = IO.timer(global)

    DockerClient.fromEnvironment(implicitly[ConcurrentEffect[IO]], global)
  }

}
