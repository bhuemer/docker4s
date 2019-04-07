package org.docker4s

import cats.effect.Effect
import org.docker4s.transport.MockClient
import org.http4s.{Request, Response}

import scala.language.higherKinds

object MockDockerClient {

  def apply[F[_]: Effect](f: Request[F] => Response[F]): DockerClient[F] = {
    new DefaultDockerClient[F](MockClient(f))
  }

}
