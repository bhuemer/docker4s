package org.docker4s
import org.scalatest.{FlatSpec, Matchers}

class DockerHostSpec extends FlatSpec with Matchers {

  "Building a docker host" should "detect environment variables" in {
    val builder = new StringBuilder()

    builder.append("Environment variables: \n")
    System
      .getenv()
      .forEach({ (key, value) =>
        builder.append(String.format(" %25s = %s %n", key, value))
      })

    builder.append("System properties: \n")
    System.getProperties.forEach({ (key, value) =>
      builder.append(String.format(" %25s = %s %n", key, value))
    })

    throw new IllegalStateException(builder.toString())
  }

}
