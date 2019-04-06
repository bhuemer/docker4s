package org.docker4s.models.containers

import io.circe.Decoder

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
