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
package org.docker4s.models.containers

import java.time.ZonedDateTime

import io.circe.Decoder
import org.docker4s.models.execs.Exec
import org.docker4s.models.images.Image

/**
  *
  * @param id The ID of the container
  * @param createdAt The time the container was created
  * @param state The state of the container
  * @param imageId The container's image
  * @param execIds IDs of exec instances that are running in the container.
  * @param networkSettings
  */
final case class Container(
    id: Container.Id,
    createdAt: ZonedDateTime,
    state: Container.State,
    imageId: Image.Id,
    execIds: List[Exec.Id],
    networkSettings: NetworkSettings)

object Container {

  case class Id(value: String)

  sealed abstract class Status(val name: String)

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

  /**
    * Contains information about the container's running state.
    */
  case class State(
      status: Status,
      running: Boolean,
      paused: Boolean,
      restarting: Boolean,
      oomKilled: Boolean,
      dead: Boolean,
      pid: Int,
      exitCode: Option[Int],
      error: Option[String],
      startedAt: ZonedDateTime,
      finishedAt: Option[ZonedDateTime],
      health: Option[State.HealthState])

  object State {

    sealed abstract class HealthStatus(val name: String)

    object HealthStatus {

      /** Starting indicates that the container is not yet ready. */
      case object Starting extends HealthStatus("starting")

      /** Healthy indicates that the container is running correctly. */
      case object Healthy extends HealthStatus("healthy")

      /** Unhealthy indicates that the container has a problem. */
      case object Unhealthy extends HealthStatus("unhealthy")

      private val all: List[HealthStatus] = List(Starting, Healthy, Unhealthy)

      /**
        * Returns the relevant health status for the given string,
        */
      def from(str: String): Option[HealthStatus] = all.find(_.name.equalsIgnoreCase(str))

    }

    /**
      * Contains information about the container's health-check results.
      * @param status Health status of the container
      * @param failingStreak The number of consecutive failures
      * @param logs The last few results from the health checks (oldest first)
      */
    final case class HealthState(status: HealthStatus, failingStreak: Int, logs: List[HealthState.Log])

    object HealthState {

      final case class Log(start: ZonedDateTime, end: ZonedDateTime, exitCode: Int, output: Option[String])

    }

  }

  // -------------------------------------------- Circe decoders

  private[containers] val statusDecoder: Decoder[Status] = Decoder.decodeString.emap({ str =>
    Status.from(str).toRight(s"Cannot decode '$str' as a container status.")
  })

  private val stateDecoder: Decoder[State] = {
    val healthStatusDecoder: Decoder[State.HealthStatus] = Decoder.decodeString.emap({ str =>
      State.HealthStatus.from(str).toRight(s"Cannot decode '$str' as a health status.")
    })

    val healthStatusLogDecoder: Decoder[State.HealthState.Log] = Decoder.instance({ c =>
      for {
        start <- c.downField("Start").as[ZonedDateTime].right
        end <- c.downField("End").as[ZonedDateTime].right
        exitCode <- c.downField("ExitCode").as[Int].right
        output <- c.downField("Output").as[Option[String]].right
      } yield State.HealthState.Log(start, end, exitCode, output)
    })

    val healthStateDecoder: Decoder[State.HealthState] = Decoder.instance({ c =>
      for {
        status <- c.downField("Status").as(healthStatusDecoder).right
        failingStreak <- c.downField("FailingStreak").as[Int].right
        logs <- c
          .downField("Log")
          .as(
            Decoder.decodeOption(
              Decoder.decodeList(healthStatusLogDecoder)
            )
          )
          .right
      } yield State.HealthState(status, failingStreak, logs.getOrElse(List.empty))
    })

    Decoder.instance({ c =>
      for {
        status <- c.downField("Status").as(statusDecoder).right
        running <- c.downField("Running").as[Boolean].right
        paused <- c.downField("Paused").as[Boolean].right
        restarting <- c.downField("Restarting").as[Boolean].right
        oomKilled <- c.downField("OOMKilled").as[Boolean].right
        dead <- c.downField("Dead").as[Boolean].right
        pid <- c.downField("Pid").as[Int].right
        exitCode <- c.downField("ExitCode").as[Option[Int]].right
        error <- c.downField("Error").as[Option[String]].right
        startedAt <- c.downField("StartedAt").as[ZonedDateTime].right
        finishedAt <- c.downField("FinishedAt").as[ZonedDateTime].right
        health <- c.downField("Health").as(Decoder.decodeOption(healthStateDecoder)).right
      } yield
        State(
          status,
          running = running,
          paused = paused,
          restarting = restarting,
          oomKilled = oomKilled,
          dead = dead,
          pid = pid,
          exitCode = exitCode,
          error = error.filter(_.nonEmpty),
          startedAt = startedAt,
          // Containers that are still running have a finished date of '0001-01-01T00:00:00Z'.
          finishedAt = Some(finishedAt).filter(_.getYear >= 2000),
          health = health
        )
    })
  }

  val decoder: Decoder[Container] = Decoder.instance({ c =>
    for {
      id <- c.downField("Id").as[String].right
      createdAt <- c.downField("Created").as[ZonedDateTime].right
      state <- c.downField("State").as(stateDecoder).right
      imageId <- c.downField("Image").as[String].right
      execIds <- c.downField("ExecIDs").as[Option[List[String]]].right
      networkSettings <- c.downField("NetworkSettings").as(NetworkSettings.decoder).right
    } yield
      Container(
        id = Container.Id(id),
        createdAt = createdAt,
        state = state,
        imageId = Image.Id(imageId),
        execIds = execIds.getOrElse(List.empty).map(Exec.Id),
        networkSettings = networkSettings
      )
  })

}
