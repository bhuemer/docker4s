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
package org.docker4s.models.execs

import io.circe.Decoder

import org.docker4s.models.containers.Container

case class Exec(
    id: Exec.Id,
    containerId: Container.Id,
    running: Boolean,
    exitCode: Option[Int],
    processConfig: Exec.ProcessConfig,
    detachKeys: Option[String],
    canRemove: Boolean,
    openStdin: Boolean,
    openStderr: Boolean,
    openStdout: Boolean,
    pid: Int)

object Exec {

  case class Id(value: String)

  case class ProcessConfig(tty: Boolean, entryPoint: String, args: List[String], privileged: Boolean, user: String)

  // -------------------------------------------- Circe encoders and decoders

  private val processConfigDecoder: Decoder[ProcessConfig] = Decoder.instance({ c =>
    for {
      tty <- c.downField("tty").as[Boolean].right
      entryPoint <- c.downField("entrypoint").as[String].right
      arguments <- c.downField("arguments").as[Option[List[String]]].right
      privileged <- c.downField("privileged").as[Boolean].right
      user <- c.downField("user").as[String].right
    } yield ProcessConfig(tty, entryPoint, arguments.getOrElse(List.empty), privileged, user)
  })

  val decoder: Decoder[Exec] = Decoder.instance({ c =>
    for {
      id <- c.downField("ID").as[String].right
      running <- c.downField("Running").as[Boolean].right
      exitCode <- c.downField("ExitCode").as[Option[Int]].right
      processConfig <- c.downField("ProcessConfig").as(processConfigDecoder).right
      openStdin <- c.downField("OpenStdin").as[Boolean].right
      openStderr <- c.downField("OpenStderr").as[Boolean].right
      openStdout <- c.downField("OpenStdout").as[Boolean].right
      canRemove <- c.downField("CanRemove").as[Boolean].right
      containerId <- c.downField("ContainerID").as[String].right
      detachKeys <- c.downField("DetachKeys").as[Option[String]].right
      pid <- c.downField("Pid").as[Int].right
    } yield
      Exec(
        id = Exec.Id(id),
        containerId = Container.Id(containerId),
        running = running,
        exitCode = exitCode,
        processConfig = processConfig,
        openStdin = openStdin,
        openStderr = openStderr,
        openStdout = openStdout,
        canRemove = canRemove,
        detachKeys = detachKeys.filter(_.nonEmpty),
        pid = pid
      )
  })

}
