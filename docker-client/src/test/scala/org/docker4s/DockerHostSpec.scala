package org.docker4s
import org.scalatest.{FlatSpec, Matchers}

class DockerHostSpec extends FlatSpec with Matchers {

  "Building a docker host" should "detect environment variables" in {
    println("Environment variables: ")
    System
      .getenv()
      .forEach({ (key, value) =>
        System.out.printf(" %25s = %s %n", key, value)
      })

    println("System properties: ")
    System.getProperties.forEach({ (key, value) =>
      System.out.printf(" %25s = %s %n", key, value)
    })
  }

}
