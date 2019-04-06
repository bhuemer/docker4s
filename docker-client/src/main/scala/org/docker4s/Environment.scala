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
package org.docker4s

import com.typesafe.scalalogging.LazyLogging

trait Environment {

  def getProperty(name: String): Option[String] = getProperty(name, None)

  def getProperty(name: String, default: => Option[String]): Option[String]

  def isOsX: Boolean = getProperty("os.name").exists(_.toLowerCase.contains("os x"))

  def isLinux: Boolean = getProperty("os.name").exists(_.equalsIgnoreCase("linux"))

}

object Environment {

  def from(options: (String, String)*): Environment = {
    val map = options.toMap

    //noinspection ConvertExpressionToSAM
    new Environment {
      override def getProperty(name: String, default: => Option[String]): Option[String] =
        map.get(name).orElse(default)
    }
  }

  /**
    * Environment implementation that actually looks up variables and properties in production environments.
    */
  object Live extends Environment with LazyLogging {

    override def getProperty(name: String, default: => Option[String]): Option[String] = {
      Option(System.getenv(name))
        .orElse(Option(System.getProperty(name)))
        .orElse(default)
    }

  }

}
