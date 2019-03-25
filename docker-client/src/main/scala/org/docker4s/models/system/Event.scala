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

import java.time.{Instant, ZoneId, ZonedDateTime}

import io.circe.Decoder

/**
  *
  * @see [[https://docs.docker.com/engine/api/v1.37/#operation/SystemEvents Docker Engine API]]
  */
case class Event(
    `type`: Event.Type,
    action: Event.Action,
    actor: Event.Actor,
    scope: Event.Scope,
    timestamp: ZonedDateTime)

object Event {

  /**
    * The type of object emitting the event.
    */
  sealed abstract class Type private (val name: String)

  object Type {

    case object Container extends Type(name = "container")
    case object Image extends Type(name = "image")
    case object Volume extends Type(name = "volume")
    case object Network extends Type(name = "network")
    case object Daemon extends Type(name = "daemon")
    case object Plugin extends Type(name = "plugin")
    case object Node extends Type(name = "node")
    case object Service extends Type(name = "service")
    case object Secret extends Type(name = "secret")
    case object Config extends Type(name = "config")

    private val all: List[Type] =
      List(Container, Image, Volume, Network, Daemon, Plugin, Node, Service, Secret, Config)

    /**
      * Returns the relevant type for the given string.
      */
    def from(str: String): Option[Type] = all.find(_.name.equalsIgnoreCase(str))

  }

  /**
    * The type of event.
    */
  sealed abstract class Action private (val name: String)

  object Action {

    case object Attach extends Action(name = "attach")
    case object Commit extends Action(name = "commit")
    case object Connect extends Action(name = "connect")
    case object Copy extends Action(name = "copy")
    case object Create extends Action(name = "create")
    case object Delete extends Action(name = "delete")
    case object Destroy extends Action(name = "destroy")
    case object Detach extends Action(name = "detach")
    case object Die extends Action(name = "die")
    case object Disable extends Action(name = "disable")
    case object Disconnect extends Action(name = "disconnect")
    case object Enable extends Action(name = "enable")
    case object ExecCreate extends Action(name = "exec_create")
    case object ExecDetach extends Action(name = "exec_detach")
    case object ExecStart extends Action(name = "exec_start")
    case object Export extends Action(name = "export")
    case object HealthStatus extends Action(name = "health_status")
    case object Import extends Action(name = "import")
    case object Kill extends Action(name = "kill")
    case object Load extends Action(name = "load")
    case object OOM extends Action(name = "oom")
    case object Pause extends Action(name = "pause")
    case object Pull extends Action(name = "pull")
    case object Push extends Action(name = "push")
    case object Reload extends Action(name = "reload")
    case object Remove extends Action(name = "remove")
    case object Rename extends Action(name = "rename")
    case object Resize extends Action(name = "resize")
    case object Restart extends Action(name = "restart")
    case object Save extends Action(name = "save")
    case object Start extends Action(name = "start")
    case object Stop extends Action(name = "top")
    case object Tag extends Action(name = "tag")
    case object Top extends Action(name = "top")
    case object Unmount extends Action(name = "unmount")
    case object Unpause extends Action(name = "unpause")
    case object Untag extends Action(name = "untag")
    case object Update extends Action(name = "update")

    private val all: List[Action] =
      List(
        Attach,
        Commit,
        Connect,
        Copy,
        Create,
        Delete,
        Destroy,
        Detach,
        Die,
        Disconnect,
        Disable,
        Enable,
        ExecCreate,
        ExecDetach,
        ExecStart,
        Export,
        HealthStatus,
        Import,
        Kill,
        Load,
        OOM,
        Pause,
        Pull,
        Push,
        Reload,
        Remove,
        Rename,
        Resize,
        Restart,
        Save,
        Start,
        Stop,
        Tag,
        Top,
        Unmount,
        Unpause,
        Untag,
        Update
      )

    def from(str: String): Option[Action] = all.find(_.name.equalsIgnoreCase(str))

  }

  /**
    * The object emitting the event, incl. its identifier and various key/value attributes depending on its type.
    */
  case class Actor(id: String, attributes: Map[String, String])

  sealed trait Scope

  object Scope {
    case object Local extends Scope
    case object Swarm extends Scope
  }

  // -------------------------------------------- Circe decoders

  private val typeDecoder: Decoder[Type] = Decoder.decodeString.emap({ str =>
    Type.from(str).toRight(s"Cannot decode $str as an event type.")
  })

  private val actionDecoder: Decoder[Action] = Decoder.decodeString.emap({ str =>
    Action.from(str).toRight(s"Cannot decode $str as an event action.")
  })

  private val actorDecoder: Decoder[Actor] = Decoder.instance({ c =>
    for {
      identifier <- c.downField("ID").as[String].right
      attributes <- c.downField("Attributes").as[Map[String, String]].right
    } yield Actor(identifier, attributes)
  })

  private val scopeDecoder: Decoder[Scope] = Decoder.decodeString.emap({
    case "local" => Right(Scope.Local)
    case "swarm" => Right(Scope.Swarm)
    case str     => Left(s"Cannot decode $str as an event scope.")
  })

  val decoder: Decoder[Event] = Decoder.instance({ c =>
    for {
      tpe <- c.downField("Type").as(typeDecoder).right
      action <- c.downField("Action").as(actionDecoder).right
      actor <- c.downField("Actor").as(actorDecoder).right
      scope <- c.downField("scope").as(scopeDecoder).right
      timeInSec <- c.downField("time").as[Long].right
      timeInNano <- c.downField("timeNano").as[Long].right
    } yield
      Event(
        tpe,
        action,
        actor,
        scope, {
          val nanoAdjustment = timeInNano - (timeInSec * 1000000000)
          Instant.ofEpochSecond(timeInSec, nanoAdjustment).atZone(ZoneId.of("Z"))
        }
      )
  })

}
