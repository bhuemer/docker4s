package org.docker4s.api

import fs2.Stream
import org.docker4s.models.containers.Container
import org.docker4s.models.execs.Exec
import org.docker4s.transport.Parameter
import org.docker4s.transport.Parameter.body

import scala.language.higherKinds

/**
  * @see https://docs.docker.com/engine/api/v1.39/#tag/Exec
  */
trait Execs[F[_]] {

  /**
    * Creates a new exec instance to run a command inside a running container.
    */
  def create(id: Container.Id, parameters: Parameter[Execs.CreateParameter]*): F[Exec.Id]

  def start(id: Exec.Id, detach: Boolean = true): Stream[F, Containers.Log]

  def run(id: Container.Id, cmd: String, args: String*): Stream[F, Containers.Log] =
    run(id, detach = false, cmd, args: _*)

  def run(id: Container.Id, detach: Boolean, cmd: String, args: String*): Stream[F, Containers.Log]

  /**
    * Resize the TTY session used by an exec instance.
    */
  def resize(id: Exec.Id, height: Int, width: Int): F[Unit]

}

object Execs {

  sealed trait CreateParameter

  object CreateParameter {

    /**
      * Attach to `stdin` of the exec command.
      */
    def attachStdin: Parameter[CreateParameter] = body("AttachStdin", true)

    /**
      * Attach to `stdout` of the exec command.
      */
    def attachStdout: Parameter[CreateParameter] = body("AttachStdout", true)

    /**
      * Attach to `stderr` of the exec command.
      */
    def attachStderr: Parameter[CreateParameter] = body("AttachStderr", true)

    /**
      * Specifies the command to run.
      */
    def withCmd(cmd: String): Parameter[CreateParameter] = body("Cmd", Seq(cmd))

    def withCmd(cmd: String, args: String*): Parameter[CreateParameter] = body("Cmd", Seq(cmd) ++ args)

    /**
      * Specifies the working directory for the exec process inside the container.
      */
    def withWorkingDir(path: String): Parameter[CreateParameter] = body("WorkingDir", path)

  }

}
