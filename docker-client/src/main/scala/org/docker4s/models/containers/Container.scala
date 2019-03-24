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
package org.docker4s.models.containers

import io.circe.Decoder

case class Container()

object Container {

  case class Id(value: String)

  sealed abstract class Status(private val name: String)

  object Status {
    case object Created extends Status("created")
    case object Running extends Status("running")
    case object Paused extends Status("paused")
    case object Restarting extends Status("restarting")
    case object Removing extends Status("removing")
    case object Exited extends Status("exited")
    case object Dead extends Status("dead")

    private val all: List[Status] =
      List(Created, Running, Paused, Restarting, Removing, Exited, Dead)

    /**
      * Returns the relevant status for the given string.
      */
    def from(str: String): Option[Status] = all.find(_.name.equalsIgnoreCase(str))

  }

  // -------------------------------------------- Circe decoders

  private[containers] val statusDecoder: Decoder[Status] = Decoder.decodeString.emap({ str =>
    Status.from(str).toRight(s"Cannot decode $str as a container status.")
  })

}
