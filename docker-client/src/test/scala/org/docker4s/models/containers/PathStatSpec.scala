/*
 * Copyright (c) 2019 Bernhard Huemer
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
package org.docker4s.models.containers

import java.nio.file.attribute.PosixFilePermission
import java.time.ZonedDateTime

import org.scalatest.{FlatSpec, Matchers}

class PathStatSpec extends FlatSpec with Matchers {

  /**
    * {{{
    * $ ls -all
    * -rw-r--r-- 1 jovyan users    6 Apr 27 08:41 hello.txt
    * }}}
    */
  "Decoding JSON into path stats" should "work for simple files" in {
    val pathStat = decodePathStat("""{
        |  "name":"hello.txt",
        |  "size":6,
        |  "mode":420,
        |  "mtime":"2019-04-27T08:41:04.389457Z",
        |  "linkTarget":""
        |}""".stripMargin)
    pathStat should be(
      PathStat(
        name = "hello.txt",
        size = 6,
        mode = PathStat.FileMode(420L),
        mtime = ZonedDateTime.parse("2019-04-27T08:41:04.389457Z"),
        linkTarget = None
      ))

    // Note that the encoded permissions integer is in the decimal system, but we're
    // typically used to the octal representation. For example, 420 dec == 644 oct.
    pathStat.mode.permissions should be(
      Set(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.GROUP_READ,
        PosixFilePermission.OTHERS_READ
      ))
    pathStat.mode.asString should be("-------------rw-r--r--")
  }

  /**
    * {{{
    * $ ls -all
    * ...
    * drwsrwsr-x 1 jovyan users 4096 Apr 25 17:25 jovyan
    * }}}
    */
  "Decoding JSON into path stats" should "work for directories" in {
    val pathStat = decodePathStat("""{
        |  "name":"jovyan",
        |  "size":4096,
        |  "mode":2160067069,
        |  "mtime":"2019-04-25T17:25:02.034063Z",
        |  "linkTarget":""
        |}""".stripMargin)
    pathStat should be(
      PathStat(
        name = "jovyan",
        size = 4096,
        mode = PathStat.FileMode(2160067069L),
        mtime = ZonedDateTime.parse("2019-04-25T17:25:02.034063Z"),
        linkTarget = None
      )
    )

    pathStat.mode.isDirectory should be(true)
    pathStat.mode.isSetuid should be(true)
    pathStat.mode.isSetgid should be(true)
    pathStat.mode.permissions should be(
      Set(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.OWNER_EXECUTE,
        PosixFilePermission.GROUP_READ,
        PosixFilePermission.GROUP_WRITE,
        PosixFilePermission.GROUP_EXECUTE,
        PosixFilePermission.OTHERS_READ,
        PosixFilePermission.OTHERS_EXECUTE
      )
    )
    pathStat.mode.asString should be("d-------ug---rwxrwxr-x")
  }

  /**
    * {{{
    * $ ls -all
    * ...
    * lrwxrwxrwx 1 jovyan users    9 Apr 27 10:33 link.txt -> hello.txt
    * }}}
    */
  "Decoding JSON into path stats" should "work for symlinks" in {
    val pathStat = decodePathStat("""{
        |  "name": "link.txt",
        |  "size": 9,
        |  "mode": 134218239,
        |  "mtime": "2019-04-27T10:33:39.898194Z",
        |  "linkTarget": "/home/jovyan/hello.txt"
        |}""".stripMargin)
    pathStat should be(
      PathStat(
        name = "link.txt",
        size = 9,
        mode = PathStat.FileMode(134218239),
        mtime = ZonedDateTime.parse("2019-04-27T10:33:39.898194Z"),
        linkTarget = Some("/home/jovyan/hello.txt")
      )
    )

    pathStat.mode.isSymlink should be(true)
    pathStat.mode.permissions should be(
      Set(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.OWNER_EXECUTE,
        PosixFilePermission.GROUP_READ,
        PosixFilePermission.GROUP_WRITE,
        PosixFilePermission.GROUP_EXECUTE,
        PosixFilePermission.OTHERS_READ,
        PosixFilePermission.OTHERS_WRITE,
        PosixFilePermission.OTHERS_EXECUTE
      )
    )
    pathStat.mode.asString should be("----L--------rwxrwxrwx")
  }

  /**
    * {{{
    * $ ls -all
    * ...
    * crw-rw-rw- 1 root root 1, 9 Apr 24 17:18 urandom
    * }}}
    */
  "Decoding JSON into path stats" should "work for device files" in {
    val pathStat = decodePathStat("""{
        |  "name":"urandom",
        |  "size":0,
        |  "mode":69206454,
        |  "mtime":"2018-04-26T21:16:13Z",
        |  "linkTarget":""
        |}""".stripMargin)
    pathStat should be(
      PathStat(
        name = "urandom",
        size = 0,
        mode = PathStat.FileMode(69206454L),
        mtime = ZonedDateTime.parse("2018-04-26T21:16:13Z"),
        linkTarget = None
      )
    )

    pathStat.mode.isDevice should be(true)
    pathStat.mode.isCharDevice should be(true)
    pathStat.mode.asString should be("-----D----c--rw-rw-rw-")
  }

  // -------------------------------------------- Utility methods

  private def decodePathStat(str: String): PathStat = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(PathStat.decoder).fold(throw _, Predef.identity)
  }

}
