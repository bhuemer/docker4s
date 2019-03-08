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
package org.docker4s

import cats.effect.{ConcurrentEffect, Sync}
import io.circe.Decoder
import org.docker4s.models.Info
import org.http4s.{EntityDecoder, Header, Method, Request, Uri}
import org.http4s.client.Client

import scala.language.higherKinds

private[docker4s] class Http4sDockerClient[F[_]: ConcurrentEffect](private val client: Client[F], private val uri: Uri)
    extends DockerClient[F] {

  private val infoDecoder: Decoder[Info] = Decoder.instance({ c =>
    for {
      id <- c.downField("ID").as[String].right

      // @formatter:off
      containers        <- c.downField("Containers").as[Int].right
      containersRunning <- c.downField("ContainersRunning").as[Int].right
      containersPaused  <- c.downField("ContainersPaused").as[Int].right
      containersStopped <- c.downField("ContainersStopped").as[Int].right
      // @formatter:on

      cpuCfsPeriod <- c.downField("CpuCfsPeriod").as[Option[Boolean]].right
      cpuCfsQuota <- c.downField("CpuCfsQuota").as[Option[Boolean]].right

      images <- c.downField("Images").as[Int].right
      osType <- c.downField("OSType").as[String].right
      architecture <- c.downField("Architecture").as[String].right

      httpProxy <- c.downField("HttpProxy").as[Option[String]].right
      httpsProxy <- c.downField("HttpsProxy").as[Option[String]].right
      noProxy <- c.downField("NoProxy").as[Option[String]].right
    } yield
      Info(
        id = id,
        containers = containers,
        containersRunning = containersRunning,
        containersPaused = containersPaused,
        containersStopped = containersStopped,
        cpuCfsPeriod = cpuCfsPeriod,
        cpuCfsQuota = cpuCfsQuota,
        images = images,
        osType = osType,
        architecture = architecture,
        httpProxy = httpProxy,
        httpsProxy = httpsProxy,
        noProxy = noProxy
      )
  })

  /**
    * Returns system-wide information. Identical to the `docker info` command.
    */
  override def info: F[Info] = {
    client.fetchAs[Info](
      Request[F]()
        .withMethod(Method.GET)
        .withHeaders(Header("Host", uri.host.map(_.value).getOrElse("localhost")))
        .withUri(uri.withPath("/info")))(accumulatingJsonOf(infoDecoder))
  }

  // Partially applied version of `accumulatingJsonOf` that doesn't require evidence for the effect type any more.
  private def accumulatingJsonOf[T](decoder: Decoder[T]): EntityDecoder[F, T] =
    org.http4s.circe.accumulatingJsonOf(implicitly[Sync[F]], decoder)

}
