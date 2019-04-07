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

import org.scalatest.Matchers

class VolumesIntegrationTest extends ClientSpec with Matchers {

  "The client" should "support creating and removing volumes" given { client =>
    for {
      volume <- client.volumes.create(name = Some("test-volume-1"))

      volumes1 <- client.volumes.list()
      _ = volumes1.volumes.map(_.name) should contain(volume.name)

      _ <- client.volumes.remove(volume.name, force = true)

      volumes2 <- client.volumes.list()
      _ = volumes2.volumes.map(_.name) shouldNot contain(volume.name)
    } yield ()
  }

  "The client" should "support pruning unused volumes" given { client =>
    for {
      volume <- client.volumes.create(name = Some("test-volume-1"))

      volumes1 <- client.volumes.list()
      _ = volumes1.volumes.map(_.name) should contain(volume.name)

      pruned <- client.volumes.prune()
      _ = pruned.volumes should contain(volume.name)

      volumes2 <- client.volumes.list()
      _ = volumes2.volumes.map(_.name) shouldNot contain(volume.name)
    } yield ()
  }

}
