/*
 * Copyright (c) 2019 Bernhard Huemer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.docker4s.api

import cats.effect.{ContextShift, IO, Timer}
import com.typesafe.scalalogging.LazyLogging
import org.docker4s.akka.AkkaDockerClient
import org.docker4s.http4s.Http4sDockerClient
import org.docker4s.{DockerClient, DockerHost, Environment}
import org.scalactic.source
import org.scalatest.FlatSpecLike
import org.scalatest.words.ResultOfStringPassedToVerb

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global

trait ClientSpec extends FlatSpecLike {

  implicit private val cs: ContextShift[IO] = IO.contextShift(global)
  implicit private val timer: Timer[IO] = IO.timer(global)
  implicit private val ec: ExecutionContext = global

  implicit protected class TestMethodDeclaration(resultOfStringPassedToVerb: ResultOfStringPassedToVerb)
      extends LazyLogging {

    def given(testFun: DockerClient[IO] => IO[Any])(implicit pos: source.Position): Unit = {
      new InAndIgnoreMethods(resultOfStringPassedToVerb).in(testFun = {
        logger.info("Running this test with the Http4s implementation.")
        Http4sDockerClient.fromHost[IO](dockerHost).use(testFun).unsafeRunSync()

        logger.info("Running this test with the Akka implementation.")
        AkkaDockerClient.managed[IO](dockerHost).use(testFun).unsafeRunSync()
      })
    }

  }

  /**
    * TODO: Create a new CircleCI job profile that doesn't use a docker executor, but a machine instead. That would
    * allow us to expose ports in docker containers we're starting up. In the meantime we'll just disable tests when
    * they shouldn't run.
    */
  protected def runningOnCircleCI: Boolean = Environment.Live.getProperty("CIRCLECI").contains("true")

  protected def dockerHost: DockerHost = DockerHost.fromEnvironment(Environment.Live)

}
