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
package org.docker4s.models.containers

import java.nio.file.attribute.PosixFilePermission
import java.time.ZonedDateTime

import io.circe.Decoder

/**
  * @param name The name of the file or the directory
  * @param size The size of the file in bytes
  * @param mode The mode of the file (attributes and permissions)
  * @param mtime The time of the last modification
  * @param linkTarget If the file is a symlink, this will contain the path to its target
  */
case class PathStat(name: String, size: Long, mode: PathStat.FileMode, mtime: ZonedDateTime, linkTarget: Option[String])

object PathStat {

  // @formatter:off
  private val MODE_DIR          = 1 << 31 // d: is a directory
  private val MODE_APPEND       = 1 << 30 // a: append-only
  private val MODE_EXCLUSIVE    = 1 << 29 // l: exclusive use
  private val MODE_TEMPORARY    = 1 << 28 // T: Temporary file
  private val MODE_SYMLINK      = 1 << 27 // L: symbolic link
  private val MODE_DEVICE       = 1 << 26 // D: Device file
  private val MODE_NAMED_PIPE   = 1 << 25 // p: named pipe (FIFO)
  private val MODE_SOCKET       = 1 << 24 // S: Unix domain socket
  private val MODE_SETUID       = 1 << 23 // u: setuid
  private val MODE_SETGID       = 1 << 22 // g: setgid
  private val MODE_CHAR_DEVICE  = 1 << 21 // c: Unix character device
  private val MODE_STICKY       = 1 << 20 // t: sticky
  private val MODE_IRREGULAR    = 1 << 19 // ?: non-regular file

  private val PERMISSIONS = Map(
    PosixFilePermission.OWNER_READ      -> (1 << 8),
    PosixFilePermission.OWNER_WRITE     -> (1 << 7),
    PosixFilePermission.OWNER_EXECUTE   -> (1 << 6),

    PosixFilePermission.GROUP_READ      -> (1 << 5),
    PosixFilePermission.GROUP_WRITE     -> (1 << 4),
    PosixFilePermission.GROUP_EXECUTE   -> (1 << 3),

    PosixFilePermission.OTHERS_READ     -> (1 << 2),
    PosixFilePermission.OTHERS_WRITE    -> (1 << 1),
    PosixFilePermission.OTHERS_EXECUTE  -> (1 << 0)
  )
  // @formatter:on

  final case class FileMode(private val mode: Long) extends AnyVal {

    /**
      * Indicates whether this file is a directory.
      */
    def isDirectory: Boolean = (mode & MODE_DIR) != 0

    def isAppendOnly: Boolean = (mode & MODE_APPEND) != 0

    def isExclusive: Boolean = (mode & MODE_EXCLUSIVE) != 0

    /**
      * Indicates whether this file is a temporary file.
      */
    def isTemporary: Boolean = (mode & MODE_TEMPORARY) != 0

    /**
      * Indicates whether this file is a symbolic link.
      */
    def isSymlink: Boolean = (mode & MODE_SYMLINK) != 0

    /**
      * Indicates whether this file is a device file.
      */
    def isDevice: Boolean = (mode & MODE_DEVICE) != 0

    /**
      * Indicates whether this file is a named pipe.
      */
    def isNamedPipe: Boolean = (mode & MODE_NAMED_PIPE) != 0

    /**
      * Indicates whether this file is a Unix domain socket.
      */
    def isSocket: Boolean = (mode & MODE_SOCKET) != 0

    def isSetuid: Boolean = (mode & MODE_SETUID) != 0

    def isSetgid: Boolean = (mode & MODE_SETGID) != 0

    def isCharDevice: Boolean = (mode & MODE_CHAR_DEVICE) != 0

    def isSticky: Boolean = (mode & MODE_STICKY) != 0

    /**
      * Renders this file mode into a string similar to Go's `os.FileMode`.
      */
    def asString: String = {
      // Lookup for the various file type modifiers. Each individual character represents
      // one of the modes enumerated earlier on in this file, in the same order.
      val types = "dalTLDpSugct?"

      val builder = new StringBuilder()
      types.zipWithIndex.foreach({
        case (char, i) =>
          builder.append(if ((mode & (1 << (31 - i))) != 0) {
            char
          } else {
            '-'
          })
      })

      val rwx = "rwxrwxrwx"
      rwx.zipWithIndex.foreach({
        case (char, i) =>
          builder.append(if ((mode & (1 << (8 - i))) != 0) {
            char
          } else {
            '-'
          })
      })

      builder.toString()
    }

    override def toString: String = s"FileMode($asString)"

    /**
      * Returns the POSIX file permissions for this file.
      */
    def permissions: Set[PosixFilePermission] = {
      val permissions = Set.newBuilder[PosixFilePermission]

      PERMISSIONS.foreach({
        case (permission, modifier) =>
          if ((mode & modifier) != 0) {
            permissions += permission
          }
      })

      permissions.result()
    }

  }

  // -------------------------------------------- Circe decoders

  val decoder: Decoder[PathStat] = Decoder.instance({ c =>
    for {
      name <- c.downField("name").as[String].right
      size <- c.downField("size").as[Long].right
      mode <- c.downField("mode").as[Long].right
      mtime <- c.downField("mtime").as[ZonedDateTime].right
      linkTarget <- c.downField("linkTarget").as[Option[String]].right
    } yield PathStat(name, size, FileMode(mode), mtime, linkTarget.filter(_.nonEmpty))
  })

}
