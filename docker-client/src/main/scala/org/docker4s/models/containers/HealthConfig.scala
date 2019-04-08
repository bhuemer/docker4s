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

import io.circe.{Decoder, Encoder, Json}

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}

/**
  *
  * @param test Command to run to check health
  * @param interval Time between running the check
  * @param timeout Maximum time to allow one check to run before considering it to have hung
  * @param retries Consecutive failures needed to report unhealthy
  * @param startPeriod Start period for the container to initialize before counting retries towards unstable
  */
case class HealthConfig(
    test: HealthConfig.Test,
    interval: Option[FiniteDuration] = None,
    timeout: Option[FiniteDuration] = None,
    retries: Option[Int] = None,
    startPeriod: Option[FiniteDuration] = None) {

  def withInterval(interval: FiniteDuration): HealthConfig = copy(interval = Some(interval))

  def withTimeout(timeout: FiniteDuration): HealthConfig = copy(timeout = Some(timeout))

  def withRetries(retries: Int): HealthConfig = copy(retries = Some(retries))

  def withStartPeriod(startPeriod: FiniteDuration): HealthConfig = copy(startPeriod = Some(startPeriod))

}

object HealthConfig {

  def inherited: HealthConfig = HealthConfig(test = Test.Inherited)

  def disabled: HealthConfig = HealthConfig(test = Test.Disabled)

  def cmd(args: String*): HealthConfig = HealthConfig(test = Test.Cmd(args.toList))
  def cmd(args: List[String]): HealthConfig = HealthConfig(test = Test.Cmd(args))

  def cmdShell(command: String): HealthConfig = HealthConfig(test = Test.CmdShell(command))

  sealed trait Test

  /**
    * For more information on how health checks work, see https://docs.docker.com/engine/reference/builder/#healthcheck
    */
  object Test {

    /**
      * Inherits health-check from image or parent image - equivalent to `[]`.
      */
    case object Inherited extends Test

    /**
      * Disables any default health-check set by the image - equivalent to `["NONE"]`.
      */
    case object Disabled extends Test

    case class Cmd(args: List[String]) extends Test

    case class CmdShell(command: String) extends Test

  }

  // -------------------------------------------- Circe encoders and decoders

  private val testEncoder: Encoder[HealthConfig.Test] = Encoder.instance({
    case Test.Inherited         => Json.arr()
    case Test.Disabled          => Json.arr(Json.fromString("NONE"))
    case Test.Cmd(args)         => Json.arr(("CMD" :: args).map(Json.fromString): _*)
    case Test.CmdShell(command) => Json.arr(Json.fromString("CMD-SHELL"), Json.fromString(command))
  })

  private val testDecoder: Decoder[HealthConfig.Test] = Decoder
    .decodeList(Decoder.decodeString)
    .emap({
      case List()                     => Right(Test.Inherited)
      case List("NONE")               => Right(Test.Disabled)
      case "CMD" :: args              => Right(Test.Cmd(args))
      case List("CMD-SHELL", command) => Right(Test.CmdShell(command))
      case otherwise                  => Left(s"Cannot decode $otherwise as a healthcheck.")
    })

  // Durations for this object are encoded as nanoseconds. It should be 0 or at least 1000000 (1 ms).
  private val durationEncoder: Encoder[Option[FiniteDuration]] = Encoder.instance({ duration =>
    Json.fromLong(duration.map(_.toNanos).map(ns => Math.max(ns, 1000000)).getOrElse(0L))
  })

  private val durationDecoder: Decoder[Option[FiniteDuration]] =
    Decoder
      .decodeOption(Decoder.decodeLong)
      .map({
        _.map({ durationInNanos =>
          FiniteDuration(durationInNanos, NANOSECONDS)
        })
      })

  implicit val encoder: Encoder[HealthConfig] = Encoder.instance({ config =>
    Json.obj(
      "Test" -> testEncoder(config.test),
      "Interval" -> durationEncoder(config.interval),
      "Timeout" -> durationEncoder(config.timeout),
      "Retries" -> Json.fromInt(config.retries.getOrElse(0)),
      "StartPeriod" -> durationEncoder(config.startPeriod)
    )
  })

  val decoder: Decoder[HealthConfig] = Decoder.instance({ c =>
    for {
      test <- c.downField("Test").as(testDecoder).right
      interval <- c.downField("Interval").as(durationDecoder).right
      timeout <- c.downField("Timeout").as(durationDecoder).right
      retries <- c.downField("Retries").as[Option[Int]].right
      startPeriod <- c.downField("StartPeriod").as(durationDecoder).right
    } yield HealthConfig(test, interval, timeout, retries, startPeriod)
  })

}
