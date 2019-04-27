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

import java.time.{Instant, ZoneId, ZonedDateTime}

import io.circe.Decoder
import org.docker4s.models.images.Image
import org.docker4s.models.networks.Endpoint

/**
  *
  * @param id The ID of this container
  * @param names The names that this container has been given
  * @param imageName The name of the image used when creating this container
  * @param imageId The ID of the image that this container was created from
  * @param command Command to run when starting the container
  * @param createdAt When the container was created
  * @param ports The ports exposed by this container
  * @param sizeRw The size of files that have been created or changed by this container
  * @param sizeRootFs The total size of all the files in this container
  * @param labels User-defined key/value metadata
  * @param state The state of this container (e.g. `Exited`)
  * @param status Additional human-readable status of this container (e.g. `Exit 0`)
  */
case class ContainerSummary(
    id: Container.Id,
    names: List[String],
    imageName: String,
    imageId: Image.Id,
    command: String,
    createdAt: ZonedDateTime,
    ports: List[PortBinding],
    sizeRw: Option[Long],
    sizeRootFs: Option[Long],
    labels: Map[String, String],
    state: Container.Status,
    status: String,
    networkMode: HostConfig.NetworkMode,
    networks: Map[String, Endpoint.Settings],
    mounts: List[MountPoint])

object ContainerSummary {

  // -------------------------------------------- Circe decoders

  private val networkModeDecoder: Decoder[HostConfig.NetworkMode] = Decoder.instance({ c =>
    c.downField("NetworkMode").as(HostConfig.NetworkMode.decoder)
  })

  private val networksDecoder: Decoder[Map[String, Endpoint.Settings]] = Decoder.instance({ c =>
    implicit val endpointSettingsDecoder: Decoder[Endpoint.Settings] = Endpoint.Settings.decoder
    c.downField("Networks").as[Option[Map[String, Endpoint.Settings]]].map(_.getOrElse(Map.empty))
  })

  val decoder: Decoder[ContainerSummary] = Decoder.instance({ c =>
    implicit val portBindingDecoder: Decoder[PortBinding] = PortBinding.decoder

    for {
      id <- c.downField("Id").as[String].right
      names <- c.downField("Names").as[List[String]].right
      image <- c.downField("Image").as[String].right
      imageId <- c.downField("ImageID").as[String].right
      command <- c.downField("Command").as[String].right
      created <- c.downField("Created").as[Long].right
      ports <- c.downField("Ports").as[Option[List[PortBinding]]].right
      state <- c.downField("State").as(Container.statusDecoder).right
      status <- c.downField("Status").as[String].right
      sizeRw <- c.downField("SizeRw").as[Option[Long]].right
      labels <- c.downField("Labels").as[Option[Map[String, String]]].right
      sizeRootFs <- c.downField("SizeRootFs").as[Option[Long]].right
      networkMode <- c.downField("HostConfig").as(networkModeDecoder).right
      networks <- c.downField("NetworkSettings").as(Decoder.decodeOption(networksDecoder)).right
      mounts <- c.downField("Mounts").as(Decoder.decodeOption(Decoder.decodeList(MountPoint.decoder))).right
    } yield
      ContainerSummary(
        id = Container.Id(id),
        names = names,
        imageName = image,
        imageId = Image.Id(imageId),
        command = command,
        createdAt = Instant.ofEpochSecond(created).atZone(ZoneId.of("Z")),
        ports = ports.getOrElse(List.empty),
        sizeRw = sizeRw,
        sizeRootFs = sizeRootFs,
        labels = labels.getOrElse(Map.empty),
        state = state,
        status = status,
        networkMode = networkMode,
        networks = networks.getOrElse(Map.empty),
        mounts = mounts.getOrElse(List.empty)
      )
  })

}
