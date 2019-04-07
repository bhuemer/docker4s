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

class SecretsIntegrationTest extends ClientSpec with Matchers {

  "The client" should "support creating and removing secrets" given { client =>
    for {
      created <- client.secrets.create("test-secret-1", "Hello World".getBytes)

      secrets1 <- client.secrets.list()
      _ = secrets1.map(_.id) should contain(created.id)

      _ <- client.secrets.remove(created.id)

      secrets2 <- client.secrets.list()
      _ = secrets2.map(_.id) shouldNot contain(created.id)
    } yield ()
  }

  "The client" should "support updating labels" given { client =>
    for {
      created <- client.secrets.create("test-secret-2", "Hello World".getBytes)

      secret1 <- client.secrets.inspect(created.id)
      _ = secret1.spec.labels should be(Map.empty)

      _ <- client.secrets.update(secret1, Map("org.example.key" -> "value"))

      secret2 <- client.secrets.inspect(created.id)
      _ = secret2.spec.labels should be(Map("org.example.key" -> "value"))

      _ <- client.secrets.remove(created.id)
    } yield ()
  }

}
