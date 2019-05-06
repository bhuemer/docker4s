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
package org.docker4s.models.images

import cats.effect.Effect
import cats.syntax.all._
import com.typesafe.scalalogging.LazyLogging
import fs2.Stream

import scala.language.higherKinds

case class BuildResult(imageId: Option[Image.Id])

object BuildResult extends LazyLogging {

  /**
    * Evaluates the given stream of build events, collecting the image ID in the process.
    */
  def evaluate[F[_]: Effect](stream: Stream[F, BuildEvent]): F[BuildResult] = {
    stream.compile
      .fold(BuildResult(None))({
        case (_, BuildEvent.Built(imageId)) => BuildResult(Some(imageId))

        // Only fall back to this workaround, if we haven't found an image ID yet in any other way.
        case (BuildResult(None), BuildEvent.Stream(str)) if str.startsWith("Successfully built ") =>
          val imageId = str.substring("Successfully built ".length)
          BuildResult(Some(Image.Id(imageId.trim)))

        case (result, _) => result
      })
      .map({ result =>
        result.imageId match {
          case Some(imageId) => logger.info(s"Built the image ${imageId.value}.")
          case None          => logger.warn("Couldn't find an image ID in the build event stream.")
        }

        result
      })
  }

}
