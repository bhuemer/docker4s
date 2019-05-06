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

import cats.effect.IO
import fs2.Stream
import org.scalatest.{FlatSpec, Matchers}

class BuildResultTest extends FlatSpec with Matchers {

  "Evaluating build event streams" should "fall back to parsing stream messages without explicit image IDs" in {
    val stream = Stream
      .emits(
        Seq(
          BuildEvent.Stream("Step 1/2 : FROM busybox:latest\n"),
          BuildEvent.Stream(" ---> af2f74c517aa\n"),
          BuildEvent.Stream("Step 2/2 : CMD echo Hello World from a container built with docker4s\n"),
          BuildEvent.Stream(" ---> Running in 848a2421f58f\n"),
          BuildEvent.Stream(" ---> 64e27c29b9a0\n"),
          BuildEvent.Stream("Removing intermediate container 848a2421f58f\n"),
          BuildEvent.Stream("Successfully built 64e27c29b9a0\n")
        ))
      .covary[IO]

    BuildResult.evaluate(stream).unsafeRunSync() should be(BuildResult(Some(Image.Id("64e27c29b9a0"))))
  }

  "Evaluating build event streams" should "pick up the proper image ID if available" in {
    val stream = Stream
      .emits(
        Seq(
          BuildEvent.Stream("Step 1/2 : FROM busybox:latest"),
          BuildEvent.Stream("\n"),
          BuildEvent.Stream(" ---> af2f74c517aa\n"),
          BuildEvent.Stream("Step 2/2 : CMD [\"echo\", \"Hello World from a container built with docker4s\"]"),
          BuildEvent.Stream("\n"),
          BuildEvent.Stream(" ---> Using cache\n"),
          BuildEvent.Stream(" ---> 261813e22459\n"),
          BuildEvent.Built(Image.Id("sha256:261813e224596ef4dafd09e54f131c05f49f3da7996fce42c2259641f8132b1e")),
          BuildEvent.Stream("Successfully built 261813e22459"),
          BuildEvent.Stream("Successfully tagged docker4s-in-memory-test:latest\n")
        ))
      .covary[IO]

    BuildResult.evaluate(stream).unsafeRunSync() should be(
      BuildResult(Some(Image.Id("sha256:261813e224596ef4dafd09e54f131c05f49f3da7996fce42c2259641f8132b1e"))))
  }

}
