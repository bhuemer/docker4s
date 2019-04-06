package org.docker4s

import java.nio.file.{Files, Paths}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

// Disabling this for now
@RunWith(classOf[JUnitRunner])
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

    val dockerCertPath = System.getenv("DOCKER_CERT_PATH")
    if (dockerCertPath != null) {
      builder.append(s"Docker cert path: $dockerCertPath").append("\n")
      Files
        .walk(Paths.get(dockerCertPath))
        .forEach({ path =>
          builder.append(s"> $path").append("\n")
        })

      val certificates = DockerCertificates(Paths.get(dockerCertPath))
      builder.append(certificates.toString)
    } else {
      builder.append("Docker cert path is null.").append("\n")
    }

    throw new IllegalStateException(builder.toString())
  }

}
