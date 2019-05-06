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
package org.docker4s.models.tasks

import io.circe.Decoder

case class Task(id: Task.Id)

object Task {

  case class Id(value: String)

  sealed abstract class State(val name: String)

  object State {
    case object New extends State(name = "new")
    case object Allocated extends State("allocated")
    case object Pending extends State("pending")
    case object Assigned extends State("assigned")
    case object Accepted extends State("accepted")
    case object Preparing extends State("preparing")
    case object Ready extends State("ready")
    case object Starting extends State("starting")
    case object Running extends State("running")
    case object Complete extends State("complete")
    case object Shutdown extends State("shutdown")
    case object Failed extends State("failed")
    case object Rejected extends State("rejected")

    private val all: List[State] =
      List(
        New,
        Allocated,
        Pending,
        Assigned,
        Accepted,
        Preparing,
        Ready,
        Starting,
        Running,
        Complete,
        Shutdown,
        Failed,
        Rejected
      )

    /**
      * Returns the relevant state for the given string.
      */
    def from(str: String): Option[State] = all.find(_.name.equalsIgnoreCase(str))

  }

  // -------------------------------------------- Circe decoders

  private val stateDecoder: Decoder[State] = Decoder.decodeString.emap({ str =>
    State.from(str).toRight(s"Cannot decode $str as a task state.")
  })

}
