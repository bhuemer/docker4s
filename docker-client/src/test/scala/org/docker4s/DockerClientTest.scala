package org.docker4s

import java.net.URL

import cats.effect._
import io.circe.Json
import org.docker4s.api.Parameter
import org.docker4s.models.containers.PortBinding

import scala.concurrent.ExecutionContext

object DockerClientTest extends IOApp {

  implicit val ec: ExecutionContext = ExecutionContext.global

  override def run(args: List[String]): IO[ExitCode] = {
    DockerClient
      .fromEnvironment[IO]
      .use({ client =>
        main(client).map(_ => ExitCode.Success)
      })
  }

  private def main(client: DockerClient[IO]): IO[Unit] = {
    import org.docker4s.api.Containers.LogParameter._
    import org.docker4s.api.Containers.CreateParameter._
    import org.docker4s.syntax._

    for {
      _ <- client.images.pull(name = "hashicorp/http-echo").compile.drain

      container <- client.containers.create(
        withImage("hashicorp/http-echo"),
        withArgs("-text=Hello from Docker4s"),
        withPortBinding(PortBinding(privatePort = 5678, publicPort = Some(1234)))
      )
      _ <- client.containers.start(container.id)

      _ = {
        val content = scala.io.Source.fromURL(new URL("http://localhost:1234")).mkString
        println("Received: " + content)
      }

      _ <- client.containers.stop(container.id)
    } yield ()
  }

  private val dockerfile =
    """
      |ARG BASE_CONTAINER=jupyter/minimal-notebook
      |FROM $BASE_CONTAINER
      |
      |LABEL maintainer="Bernhard Huemer <bernhard.huemer@gmail.com>"
      |
      |# Set the desired version of almond
      |ENV ALMOND_VERSION="0.1.12"
      |
      |# Set the desired version of SBT
      |ENV SBT_VERSION="1.2.3"
      |
      |# -----------------------------------------------------------------------------
      |# --- Install depenencies (distro packages)
      |# -----------------------------------------------------------------------------
      |
      |USER root
      |
      |# Install software-properties and curl
      |RUN \
      |  apt-get update \
      |  && apt-get install -y curl \
      |  && apt-get install -y openjdk-8-jdk \
      |  && rm -rf /var/lib/apt/lists/*
      |
      |# Define JAVA_HOME environment variable
      |ENV JAVA_HOME /usr/lib/jvm/openjdk-8
      |ENV PATH=${PATH}:${JAVA_HOME}/bin
      |
      |# -----------------------------------------------------------------------------
      |# --- Download and install Almond,  Scala kernel for Jupyter / IPython 3.
      |# --- For details, see https://github.com/almond-sh/almond
      |# -----------------------------------------------------------------------------
      |
      |# Download SBT
      |RUN curl -sL --retry 5 "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz" \
      |  | gunzip \
      |  | tar -x -C "/tmp/" \
      |  && mv "/tmp/sbt" "/opt/sbt-${SBT_VERSION}" \
      |  && chmod +x "/opt/sbt-${SBT_VERSION}/bin/sbt"
      |
      |ENV PATH=${PATH}:/opt/sbt-${SBT_VERSION}/bin/
      |
      |RUN curl -L -o /usr/local/bin/coursier https://git.io/coursier && chmod +x /usr/local/bin/coursier
      |
      |# Switch back to jovyan to avoid accidental container runs as root
      |USER $NB_USER
      |
      |WORKDIR /tmp
      |
      |ENV ALMOND_VERSION=0.1.9
      |
      |ENV SCALA_VERSION=2.11.12
      |
      |RUN coursier bootstrap \
      |    -i user -I user:sh.almond:scala-kernel-api_$SCALA_VERSION:$ALMOND_VERSION \
      |    sh.almond:scala-kernel_$SCALA_VERSION:$ALMOND_VERSION \
      |    -o almond_2_11 && \
      |    chmod +x almond_2_11 && \
      |    ./almond_2_11 --id almond_scala_2_11 --display-name "Scala 2.11 (almond)" --install
      |
      |ENV SCALA_VERSION=2.12.7
      |
      |RUN coursier bootstrap \
      |    -i user -I user:sh.almond:scala-kernel-api_$SCALA_VERSION:$ALMOND_VERSION \
      |    sh.almond:scala-kernel_$SCALA_VERSION:$ALMOND_VERSION \
      |    -o almond_2_12 && \
      |    chmod +x almond_2_12 && \
      |    ./almond_2_12 --id almond_scala_2_12 --display-name "Scala 2.12 (almond)" --install
      |
      |RUN rm /tmp/almond_2_11
      |RUN rm /tmp/almond_2_12
      |
      |WORKDIR /home/$NB_USER/
      |
      |# install the kernel gateway
      |RUN pip install jupyter_kernel_gateway
      |
      |# run kernel gateway on container start, not notebook server
      |EXPOSE 8888
      |CMD ["jupyter", "kernelgateway", "--KernelGatewayApp.ip=0.0.0.0", "--KernelGatewayApp.port=8888"]
    """.stripMargin

}
