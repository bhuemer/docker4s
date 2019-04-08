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
import org.docker4s.models.networks.Network
import org.http4s.{EntityEncoder, Response, Status, Uri}
import org.http4s.circe.jsonEncoder
import org.scalatest.matchers.{MatchResult, Matcher}
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
    ex should includeInMessage("Something bad happened")
    ex should includeInMessage("/networks/list")
  }

  "Error responses" should "contain the actual bodies if it's not JSON" in {
    val client = newClient(Status.InternalServerError, body = "Some other response")

    val ex = the[Exception] thrownBy client.get("/networks/list").execute.unsafeRunSync()
    ex should includeInMessage("Some other response")
    ex should includeInMessage("/networks/list")
  }

  "Error responses" should "contain the actual bodies if it's valid JSON, but we didn't manage to parse it" in {
    val client = newClient(
      Status.Ok,
      body = Json.arr(
        Json.obj(
          "Id" -> Json.fromString("123456"),
          "Name" -> Json.fromString("network-name"),
          "Created" -> Json.fromString("2019-04-08T12:05:25.185+01:00"),
          "Scope" -> Json.fromString("invalid-scope")
        ))
    )

    val ex = the[Exception] thrownBy client.get("/networks/list").expectMany(Network.decoder).unsafeRunSync()

    ex should includeInMessage(""""Id" : "123456"""")
    ex should includeInMessage(""""Name" : "network-name"""")
    ex should includeInMessage(""""Created" : "2019-04-08T12:05:25.185+01:00"""")
    ex should includeInMessage(""""Scope" : "invalid-scope"""")
    ex should includeInMessage("Cannot decode invalid-scope as a network scope.")

    // In addition to the details about the JSON, it should also include information about the request that failed.
    ex should includeInMessage("/networks/list")
  }

  // -------------------------------------------- Utility methods & classes

  private def newClient[T](status: Status, body: T)(implicit encoder: EntityEncoder[IO, T]): Client[IO] = {
    Client.from(org.http4s.client.Client({ _ =>
      Resource.pure(Response[IO]().withStatus(status).withEntity(body))
    }), uri = Uri.unsafeFromString("http://localhost"))
  }

  private def includeInMessage(message: String): Matcher[Exception] = new MessageMatcher(message)

  private class MessageMatcher(private val message: String) extends Matcher[Exception] {

    override def apply(left: Exception): MatchResult = {
      val messages = messagesOf(left)
      MatchResult(
        messages.exists(_.contains(message)),
        s"Exception $left did not contain the message '$message' in any of the cause's messages [$messages].",
        s"Exception $left did contain the message '$message' in one of the cause's messages [$messages]."
      )
    }

    /**
      * Returns all messages that appear anywhere in the given exception.
      */
    private def messagesOf(ex: Throwable): List[String] = {
      ex.getMessage :: (ex.getCause match {
        case null                 => List()
        case cause if cause == ex => List()
        case cause                => messagesOf(cause)
      })
    }

  }

}
