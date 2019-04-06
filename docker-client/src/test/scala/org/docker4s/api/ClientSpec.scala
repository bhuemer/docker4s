package org.docker4s.api

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import org.docker4s.DockerClient
import org.scalactic.source
import org.scalatest.FlatSpecLike
import org.scalatest.words.ResultOfStringPassedToVerb

import scala.concurrent.ExecutionContext.global

trait ClientSpec extends FlatSpecLike {

  implicit protected class TestMethodDeclaration(resultOfStringPassedToVerb: ResultOfStringPassedToVerb) {

    def given(testFun: DockerClient[IO] => IO[Any])(implicit pos: source.Position): Unit = {
      new InAndIgnoreMethods(resultOfStringPassedToVerb).in(testFun = {
        implicit val cs: ContextShift[IO] = IO.contextShift(global)
        implicit val timer: Timer[IO] = IO.timer(global)
        val clientResource = DockerClient.fromEnvironment(implicitly[ConcurrentEffect[IO]], global)
        clientResource.use(testFun).unsafeRunSync()
      })
    }

  }

}
