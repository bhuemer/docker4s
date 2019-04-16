package org.docker4s

import cats.effect._
import fs2.Stream
import org.docker4s.api.Containers
import org.docker4s.models.images.Image
import org.docker4s.util.Compression

object DockerClientTest {

  def main(args: Array[String]): Unit = {
    import cats.effect.{ContextShift, Timer}
    import scala.concurrent.ExecutionContext.global

    implicit val cs: ContextShift[IO] = IO.contextShift(global)
    implicit val timer: Timer[IO] = IO.timer(global)
    val cf: ConcurrentEffect[IO] = implicitly[ConcurrentEffect[IO]]

    DockerClient
      .fromEnvironment(Environment.Live)(cf, global)
      .use({ client =>
        main(client)
      })
      .unsafeRunSync()
    println()
  }

  private def main(client: DockerClient[IO])(implicit cs: ContextShift[IO], timer: Timer[IO]): IO[Unit] = {
    import org.docker4s.api.Containers.LogParameter._
    import org.docker4s.api.Containers.CreateParameter._
    import org.docker4s.syntax._

    for {
      _ <- client.images.pull(name = "busybox").compile.drain

      built <- client.images
        .build(
          Stream
            .emit(Compression.TarEntry(
              "Dockerfile",
              """
              |FROM busybox:latest
              |CMD ["sh", "-c", "while true; do echo -n 'This is a test ';date ; sleep 5; done"]
            """.stripMargin.getBytes
            ))
            .through(Compression.tar())
            .through(Compression.gzip()))
        .result

      // Create a container from the newly built image and run it
      container <- client.containers.create(withImage(built.imageId.get))
      _ <- client.containers.start(container.id)

      // Follow all the logs from the container (stderr not actually used in this example ..)
      _ <- client.containers
        .logs(container.id, stdout, stderr, follow)
        .evalTap[IO]({
          case Containers.Log(Containers.Stream.StdOut, message) => IO(System.out.println(message))
          case Containers.Log(Containers.Stream.StdErr, message) => IO(System.err.println(message))
        })
        .compile
        .drain
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
