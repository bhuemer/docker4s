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

/**
  * Information about containers that were pruned / removed because they were stopped.
  * @see [[https://docs.docker.com/engine/api/v1.37/#operation/ContainerPrune Docker Engine API]]
  * @param containers Container IDs that were deleted
  * @param spaceReclaimed Disk space reclaimed in bytes
  */
case class ContainersPruned(containers: List[Container.Id], spaceReclaimed: Long)

object ContainersPruned {

  // -------------------------------------------- Circe decoders

  val decoder: Decoder[ContainersPruned] = Decoder.instance({ c =>
    for {
      containers <- c.downField("ContainersDeleted").as[Option[List[String]]].right
      reclaimed <- c.downField("SpaceReclaimed").as[Long].right
    } yield ContainersPruned(containers.getOrElse(List.empty).map(Container.Id), reclaimed)
  })

}
