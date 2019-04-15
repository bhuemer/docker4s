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

import org.docker4s.api.Containers.CreateParameter._
import org.docker4s.api.Containers.LogParameter._
import org.scalatest.Matchers

class ContainersCreateIntegrationTest extends ClientSpec with Matchers {

  /**
    * Makes sure that you can override the default specified in an image's `CMD`.
    */
  "The client" should "support specifying additional commands when creating a container" given { client =>
    for {
      _ <- client.images.pull("busybox").compile.drain

      created <- client.containers.create(withImage("busybox"), withCmd("echo", "Hello from docker4s"))
      _ <- client.containers.start(created.id)
      _ <- client.containers.stop(created.id)
      _ <- client.containers.await(created.id)

      logs <- client.containers.logs(created.id, stdout).compile.toList
      _ = logs should be(List(Containers.Log(Containers.Stream.StdOut, "Hello from docker4s")))
    } yield ()
  }

}
