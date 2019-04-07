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
package org.docker4s.transport

import cats.effect.{ContextShift, IO, Resource, Timer}
import io.circe.Json
import org.http4s.{EntityEncoder, Response, Status, Uri}
import org.http4s.circe.jsonEncoder
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.global

class ClientTest extends FlatSpec with Matchers {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  "Error responses" should "contain the messages and request information" in {
    // @formatter:off
    val client = newClient(Status.InternalServerError, body = Json.obj(
      "message" -> Json.fromString("Something bad happened"))
    )
    // @formatter:on

    val ex = the[Exception] thrownBy client.get("/networks/list").execute.unsafeRunSync()
    ex.getMessage should include("Something bad happened")
    ex.getMessage should include("/networks/list")
  }

  "Error responses" should "contain the actual bodies if it's not JSON" in {
    val client = newClient(Status.InternalServerError, body = "Some other response")

    val ex = the[Exception] thrownBy client.get("/networks/list").execute.unsafeRunSync()
    ex.getMessage should include("Some other response")
    ex.getMessage should include("/networks/list")
  }

  // -------------------------------------------- Utility methods

  private def newClient[T](status: Status, body: T)(implicit encoder: EntityEncoder[IO, T]): Client[IO] = {
    Client.from(org.http4s.client.Client({ _ =>
      Resource.pure(Response[IO]().withStatus(status).withEntity(body))
    }), uri = Uri.unsafeFromString("http://localhost"))
  }

}
