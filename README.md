# docker4s

[![CircleCI](https://circleci.com/gh/bhuemer/docker4s.svg?style=shield&circle-token=ef5511de1818d46e6cce2e2d84ee7bbd2b1ff40b)](https://circleci.com/gh/bhuemer/docker4s)

Docker client for Scala backed by http4s, fs2, cats & circe.

## Installation

docker4s is available for Scala 2.12 on Sonatype OSS Snapshots at the following coordinates:

```scala
resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.docker4s" % "docker-client" % "0.9-SNAPSHOT",
  "io.netty" % "netty-transport-native-kqueue" % "4.1.33.Final" classifier "osx-x86_64"
  // Or, if you want to use UNIX domain sockets on `epoll`-based systems like Linux:
  // "io.netty" % "netty-transport-native-epoll" % "4.1.33.Final" classifier "linux-x86_64"
)
```

The second dependency is optional and only necessary when you want to use UNIX domain sockets to communicate with the Docker host (see also [Netty project: Native transports](https://netty.io/wiki/native-transports.html)).

## Usage example

The API is heavily inspired by the Docker client for Python and hopefully mostly self-explanatory. It's far from feature-complete and still in active development, but at the same time most of the crucial operations have been implemented already.

```scala
import cats.effect.{ExitCode, IO, IOApp}
import org.docker4s.DockerClient
import org.docker4s.api.Containers
import org.docker4s.api.Containers.LogParameter._

import scala.concurrent.ExecutionContext

object DockerApp extends IOApp {

  private implicit val ec: ExecutionContext = ExecutionContext.global

  override def run(args: List[String]): IO[ExitCode] = {
    // Uses environment variables to determine the Docker host, e.g. by default on a
    // MacOS X machine it'd be "unix:///var/run/docker.sock" without certificates.
    val clientResource = DockerClient.fromEnvironment[IO]
    clientResource.use({ client =>
      for {
        // Equivalent to `docker container ls`
        containers <- client.containers.list()
        _ = containers.foreach(container => println("Container: " + container))

        // Equivalent to `docker run hello-world`
        _ <- client.images.pull("hello-world").compile.drain
        container <- client.containers.create(image = "hello-world")
        _ <- client.containers.start(container.id)

        // Equivalent to `docker logs ${container.id}`
        _ <- client.containers.logs(container.id, stdout, stderr).evalTap[IO]({
          case Containers.Log(Containers.Stream.StdOut, message) => IO(System.out.println(message))
          case Containers.Log(Containers.Stream.StdErr, message) => IO(System.err.println(message))
        }).compile.drain
      } yield ExitCode.Success
    })
  }

}
```