/**
  *
  */
package org.docker4s.errors

class DockerException(message: String, val context: String, val status: Option[Int], cause: Throwable)
    extends Exception(s"$message [$context]", cause) {}

object DockerException {}
