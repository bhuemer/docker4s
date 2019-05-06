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

package org.docker4s.akka
import java.net.URL

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.{ExitCode, IO, IOApp}
import org.docker4s.DockerClient
import org.docker4s.models.containers.PortBinding

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object DockerClientTest extends IOApp {

  implicit val ec: ExecutionContext = ExecutionContext.global

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    AkkaDockerClient
      .fromEnvironment[IO]
      .use({ client =>
        main(client).map({ _ =>
          materializer.shutdown()
          system.terminate()
          ExitCode.Success
        })
      })
  }

  private def main(client: DockerClient[IO]): IO[Unit] = {
    import org.docker4s.api.Containers.CreateParameter._

    for {
      _ <- client.images.pull(name = "hashicorp/http-echo").compile.drain

      container <- client.containers.create(
        withImage("hashicorp/http-echo"),
        withArgs("-text=Hello from Docker4s", "-listen=:8000"),
        withExposedPort(port = 8000),
        withPortBinding(PortBinding(privatePort = 8000, publicPort = Some(1234)))
      )
      _ <- client.containers.start(container.id)

      _ = {
        val content = scala.io.Source.fromURL(new URL("http://localhost:1234")).mkString
        println("Received: " + content)
      }

      _ <- client.containers.stop(container.id, timeout = 1.second)
      _ = println("Removed container")
    } yield ()
  }

}
