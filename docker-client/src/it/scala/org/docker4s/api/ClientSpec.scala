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

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import org.docker4s.{DockerClient, Environment}
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
        val clientResource = DockerClient.fromEnvironment(Environment.Live)(implicitly[ConcurrentEffect[IO]], global)
        clientResource.use(testFun).unsafeRunSync()
      })
    }

  }

}