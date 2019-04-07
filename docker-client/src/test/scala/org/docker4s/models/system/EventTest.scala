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
package org.docker4s.models.system

import java.time.ZonedDateTime

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class EventTest extends FlatSpec with Matchers {

  "Decoding JSON into events" should "decode `container-start` events" in {
    val event = decodeEvent("""{
        | "status":"start",
        | "id":"0749816a7fd12bcc36385e570df0b8038051ed3ae021337eb0baab13eccd27cf",
        | "from":"hello-world",
        | "Type":"container",
        | "Action":"start",
        | "Actor":{
        |   "ID":"0749816a7fd12bcc36385e570df0b8038051ed3ae021337eb0baab13eccd27cf",
        |   "Attributes":{
        |     "image":"hello-world",
        |     "name":"wonderful_morse"
        |   }
        | },
        | "scope":"local",
        | "time":1552210993,
        | "timeNano":1552210993225713700}""".stripMargin)

    event should be(
      Event(
        Event.Type.Container,
        Event.Action.Start,
        Event.Actor("0749816a7fd12bcc36385e570df0b8038051ed3ae021337eb0baab13eccd27cf",
                    Map("image" -> "hello-world", "name" -> "wonderful_morse")),
        Event.Scope.Local,
        ZonedDateTime.parse("2019-03-10T09:43:13.225713700Z")
      )
    )
  }

  "Decoding JSON into events" should "decode `container-die` events" in {
    val event = decodeEvent("""{
        | "status":"die",
        | "id":"af66e275d4b46ed304043c395c73253d4fc2526336cd15a670e9b4f81fb9f750",
        | "from":"hello-world",
        | "Type":"container",
        | "Action":"die",
        | "Actor":{
        |   "ID":"af66e275d4b46ed304043c395c73253d4fc2526336cd15a670e9b4f81fb9f750",
        |   "Attributes":{
        |     "exitCode":"0",
        |     "image":"hello-world",
        |     "name":"brave_rosalind"
        |   }
        | },
        | "scope":"local",
        | "time":1552162751,
        | "timeNano":1552162751311229300
        |}""".stripMargin)

    event should be(
      Event(
        Event.Type.Container,
        Event.Action.Die,
        Event.Actor("af66e275d4b46ed304043c395c73253d4fc2526336cd15a670e9b4f81fb9f750",
                    Map("exitCode" -> "0", "image" -> "hello-world", "name" -> "brave_rosalind")),
        Event.Scope.Local,
        ZonedDateTime.parse("2019-03-09T20:19:11.311229300Z")
      ))
  }

  "Decoding JSON into events" should "decode `network-connect` events" in {
    val event = decodeEvent("""{
        | "Type":"network",
        | "Action":"connect",
        | "Actor":{
        |   "ID":"42d2ef1d4ca95d5e6b2c80c1f6dff08f905e1c0a6f94c2ba8bcbd50d9ea13bb9",
        |   "Attributes":{
        |     "container":"af66e275d4b46ed304043c395c73253d4fc2526336cd15a670e9b4f81fb9f750",
        |     "name":"bridge",
        |     "type":"bridge"
        |   }
        | },
        | "scope":"local",
        | "time":1552162750,
        | "timeNano":1552162750745733800}""".stripMargin)

    event should be(
      Event(
        Event.Type.Network,
        Event.Action.Connect,
        Event.Actor(
          "42d2ef1d4ca95d5e6b2c80c1f6dff08f905e1c0a6f94c2ba8bcbd50d9ea13bb9",
          Map("container" -> "af66e275d4b46ed304043c395c73253d4fc2526336cd15a670e9b4f81fb9f750",
              "name" -> "bridge",
              "type" -> "bridge")
        ),
        Event.Scope.Local,
        ZonedDateTime.parse("2019-03-09T20:19:10.745733800Z")
      )
    )
  }

  // -------------------------------------------- Utility methods

  /** Decodes the given string as an [[Event]] or throws an exception if something goes wrong. */
  private def decodeEvent(str: String): Event = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(Event.decoder).fold(throw _, Predef.identity)
  }

}
