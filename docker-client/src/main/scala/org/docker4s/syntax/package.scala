/*
 * Copyright (c) 2019 Bernhard Huemer (bernhard.huemer@gmail.com)
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
package org.docker4s

import cats.effect.Effect
import fs2.Stream
import org.docker4s.models.images.{BuildEvent, BuildResult, PullEvent, PullResult}

import scala.language.higherKinds

package object syntax {

  implicit class PullEventStreamOps[F[_]: Effect](private val stream: Stream[F, PullEvent]) {

    /**
      * Evaluates the given stream of pull events, collecting both the status and the digest in the process.
      */
    def result: F[PullResult] = PullResult.evaluate(stream)

  }

  implicit class BuildEventStreamOps[F[_]: Effect](private val stream: Stream[F, BuildEvent]) {

    /**
      * Evaluates the given stream of build events, collecting the image ID in the process.
      */
    def result: F[BuildResult] = BuildResult.evaluate(stream)

  }

}
