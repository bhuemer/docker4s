package org.docker4s

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.global

@RunWith(classOf[JUnitRunner])
class DockerClientSpec extends FlatSpec with Matchers {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  "Docker client" should "list images" in {
    val builder = new StringBuilder()

    val clientResource = DockerClient.fromEnvironment(implicitly[ConcurrentEffect[IO]], global)
    val images = clientResource
      .use({ client =>
        client.images.list()
      })
      .unsafeRunSync()

    builder.append(s"Number of images: ${images.size}")
    images.foreach(builder.append(_).append("\n"))

    throw new IllegalStateException(builder.toString())
  }

}
