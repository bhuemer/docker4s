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

/**
  * Information about the container that was created as returned by the Docker host.
  * @see [[https://docs.docker.com/engine/api/v1.39/#operation/ContainerCreate Docker Engine API]]
  * @param id The ID of the created container
  * @param warnings Warnings encountered when creating the container
  */
case class ContainerCreated(id: Container.Id, warnings: List[String])

object ContainerCreated {

  // -------------------------------------------- Circe decoders

  val decoder: Decoder[ContainerCreated] = Decoder.instance({ c =>
    for {
      id <- c.downField("Id").as[String].right
      warnings <- c.downField("Warnings").as[Option[List[String]]].right
    } yield {
      ContainerCreated(id = Container.Id(id), warnings = warnings.getOrElse(List.empty))
    }
  })

}
