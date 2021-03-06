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
package org.docker4s.models.system

import java.time.ZonedDateTime

import io.circe.Decoder

case class Version(
    version: String,
    apiVersion: String,
    minApiVersion: Option[String],
    gitCommit: String,
    goVersion: String,
    os: String,
    arch: String,
    kernelVersion: String,
    buildTime: ZonedDateTime)

object Version {

  // -------------------------------------------- Circe decoders

  val decoder: Decoder[Version] = Decoder.instance({ c =>
    for {
      version <- c.downField("Version").as[String].right
      apiVersion <- c.downField("ApiVersion").as[String].right
      minApiVersion <- c.downField("MinAPIVersion").as[Option[String]].right

      gitCommit <- c.downField("GitCommit").as[String].right
      goVersion <- c.downField("GoVersion").as[String].right

      os <- c.downField("Os").as[String].right
      arch <- c.downField("Arch").as[String].right
      kernelVersion <- c.downField("KernelVersion").as[String].right

      buildTime <- c.downField("BuildTime").as[ZonedDateTime].right
    } yield
      Version(
        version = version,
        apiVersion = apiVersion,
        minApiVersion = minApiVersion,
        gitCommit = gitCommit,
        goVersion = goVersion,
        os = os,
        arch = arch,
        kernelVersion = kernelVersion,
        buildTime = buildTime
      )
  })

}
