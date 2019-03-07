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
package org.docker4s.models

import io.circe.Decoder

case class Info(
    id: String,
    containers: Int,
    containersRunning: Int,
    containersPaused: Int,
    containersStopped: Int,
    images: Int,
    osType: String,
    architecture: String)

object Info {

  val decoder: Decoder[Info] = Decoder.instance({ c =>
    for {
      id <- c.downField("ID").as[String].right

      // @formatter:off
      containers        <- c.downField("Containers").as[Int].right
      containersRunning <- c.downField("ContainersRunning").as[Int].right
      containersPaused  <- c.downField("ContainersPaused").as[Int].right
      containersStopped <- c.downField("ContainersStopped").as[Int].right
      // @formatter:on

      images <- c.downField("Images").as[Int].right
      osType <- c.downField("OSType").as[String].right
      architecture <- c.downField("Architecture").as[String].right
    } yield Info(id, containers, containersRunning, containersPaused, containersStopped, images, osType, architecture)
  })

}
