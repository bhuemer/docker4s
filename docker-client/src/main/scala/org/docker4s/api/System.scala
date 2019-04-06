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

import java.time.ZonedDateTime

import fs2.Stream
import org.docker4s.api.Criterion.{filter, query}
import org.docker4s.models.system.{Event, Info, Version}

import scala.language.higherKinds

/**
  * Docker client methods related to the system endpoint, i.e. similar to `docker system ..` commands.
  * @tparam F the effect type for evaluations, e.g. `IO`
  */
trait System[F[_]] {

  /**
    * Returns system-wide information. Similar to the `docker info` or `docker system info` command.
    */
  def info: F[Info]

  /**
    * Streams real-time events from the server. Similar to the `docker system events` command.
    */
  def events(criteria: Criterion[System.EventsCriterion]*): Stream[F, Event]

  /**
    * Returns version information from the server. Similar to the `docker version` command.
    */
  def version: F[Version]

}

object System {

  sealed trait EventsCriterion

  object EventsCriterion {

    /**
      * Show events created since this timestamp, then stream new events.
      */
    def since(timestamp: ZonedDateTime): Criterion[EventsCriterion] = query("since", timestamp.toInstant.getEpochSecond)

    /**
      * Show events created until this timestamp, then stop streaming.
      */
    def until(timestamp: ZonedDateTime): Criterion[EventsCriterion] = query("until", timestamp.toInstant.getEpochSecond)

    /**
      * Show events related to the given action. For example, `action(Event.Action.Pull)` for pull events.
      */
    def action(action: Event.Action): Criterion[EventsCriterion] = filter("event", action.name)

    /**
      * Show events related to the given config name or ID.
      */
    def config(name: String): Criterion[EventsCriterion] = filter("config", name)

    /**
      * Show events related to the given container name or ID.
      */
    def container(name: String): Criterion[EventsCriterion] = filter("container", name)

    /**
      * Show events related to the given daemon name or ID.
      */
    def daemon(name: String): Criterion[EventsCriterion] = filter("daemon", name)

    /**
      * Show events related to the given image name or ID.
      */
    def image(name: String): Criterion[EventsCriterion] = filter("image", name)

    /**
      * Show events related to the given network name or ID.
      */
    def network(name: String): Criterion[EventsCriterion] = filter("network", name)

    /**
      * Show events related to the given node name or ID.
      */
    def node(name: String): Criterion[EventsCriterion] = filter("node", name)

    /**
      * Show events related to the given plugin name or ID.
      */
    def plugin(name: String): Criterion[EventsCriterion] = filter("plugin", name)

    /**
      * Show events for scope `local` or `swarm`.
      */
    def scope(scope: Event.Scope): Criterion[EventsCriterion] = scope match {
      case Event.Scope.Local => filter("scope", "local")
      case Event.Scope.Swarm => filter("scope", "swarm")
    }

    /**
      * Show events related to the given secret name or ID.
      */
    def secret(name: String): Criterion[EventsCriterion] = filter("secret", name)

    /**
      * Show events related to the given service name or ID.
      */
    def service(name: String): Criterion[EventsCriterion] = filter("service", name)

    /**
      * Show events for objects of the given type.
      */
    def `type`(`type`: Event.Type): Criterion[EventsCriterion] = filter("type", `type`.name)

    /**
      * Show events related to the given volume name.
      */
    def volume(name: String): Criterion[EventsCriterion] = filter("volume", name)

  }

}
