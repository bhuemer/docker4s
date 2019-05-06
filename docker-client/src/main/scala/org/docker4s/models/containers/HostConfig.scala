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

import io.circe.Decoder
import org.docker4s.models.containers.Container.{Id => ContainerId}

/**
  *
  * @param binds List of volume bindings for this container
  * @param logConfig Configuration of the logs for this container
  * @param networkMode Network mode to use for this container
  * @param portBindings The mappings of container ports to host ports.
  */
case class HostConfig(
    binds: List[String],
    logConfig: HostConfig.LogConfig,
    networkMode: HostConfig.NetworkMode,
    portBindings: List[PortBinding])

object HostConfig {

  /**
    * Represents the logging configuration of the container.
    */
  case class LogConfig(`type`: String, config: Map[String, String])

  sealed trait NetworkMode

  object NetworkMode {

    case object None extends NetworkMode
    case object Host extends NetworkMode
    case object Bridge extends NetworkMode
    case object Default extends NetworkMode
    case class Container(id: ContainerId) extends NetworkMode
    case class Custom(name: String) extends NetworkMode

    // ------------------------------------------ Circe decoders

    val decoder: Decoder[NetworkMode] = Decoder.decodeString.map({
      case "none" => None
      case "host" => Host
      // In Windows this network is given the name NAT but still the bridge network stack
      case "nat"     => Bridge
      case "bridge"  => Bridge
      case "default" => Default
      case container if container.startsWith("container:") =>
        Container(ContainerId(container.substring("container:".length)))
      // Any other value is taken as a custom network's name to which the container should connect to
      case otherwise => Custom(otherwise)
    })

  }

}
