package org.docker4s

import java.nio.file.{Path, Paths}

sealed trait DockerHost

object DockerHost {

  def fromEnvironment: DockerHost = {
    // TODO: Implement this properly
    DockerHost.Unix(Paths.get("/var/run/docker.sock"), None)
  }

  /**
    * Connects via UNIX domain sockets to the given docker host.
    */
  case class Unix(socketPath: Path, certificatePath: Option[Path]) extends DockerHost

  case class Npipe()

  /**
    * Connects via TCP to the given docker host.
    */
  case class Tcp(host: String, port: Int, certificatePath: Option[Path]) extends DockerHost
}
