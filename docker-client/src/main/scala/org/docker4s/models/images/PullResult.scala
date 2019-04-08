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
package org.docker4s.models.images

import cats.effect.Effect
import fs2.Stream

import scala.language.higherKinds

case class PullResult(status: Option[String], digest: Option[String])

object PullResult {

  /**
    * Evaluates the given stream of pull events, collecting both the status and the digest in the process.
    */
  def evaluate[F[_]: Effect](stream: Stream[F, PullEvent]): F[PullResult] = {
    stream.compile.fold(PullResult(None, None))({
      case (result, PullEvent.Digest(digest)) => result.copy(digest = Some(digest))
      case (result, PullEvent.Status(status)) => result.copy(status = Some(status))
      case (result, _)                        => result
    })
  }

}
