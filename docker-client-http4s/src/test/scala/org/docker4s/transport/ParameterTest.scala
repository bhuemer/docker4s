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
package org.docker4s.transport

import org.http4s.Query
import org.scalatest.{FlatSpec, Matchers}

class ParameterTest extends FlatSpec with Matchers {

  /**
    * Makes sure that query parameters and JSON encoded filter parameters can be mixed in a single query string.
    */
  "Building criteria" should "encode `filters` as JSON" in {
    renderQueryString(
      Parameter.query("all", "true"),
      Parameter.query("digests", "false"),
      Parameter.filter("dangling", "true"),
      Parameter.filter("before", "image-name")
    ) should be(
      "all=true&digests=false&filters=%7B%22dangling%22%3A%5B%22true%22%5D%2C%22before%22%3A%5B%22image-name%22%5D%7D")
  }

  "Building criteria" should "encode `query arrays` as JSON" in {
    renderQueryString(
      Parameter.query("nocache", false),
      Parameter.queryArr("cachefrom", "image-1"),
      Parameter.queryArr("cachefrom", "image-2")
    ) should be("nocache=false&cachefrom=%5B%22image-1%22%2C%22image-2%22%5D")
  }

  "Building parameters" should "encode `query maps` as JSON" in {
    renderQueryString(
      Parameter.query("nocache", false),
      Parameter.queryMap("buildargs", "FOO", "bar"),
      Parameter.queryMap("buildargs", "BLA", "blubb")
    ) should be("nocache=false&buildargs=%7B%22FOO%22%3A%22bar%22%2C%22BLA%22%3A%22blubb%22%7D")

    renderQueryString(
      Parameter.query("nocache", false),
      Parameter.queryMap("labels", "key1", "value1"),
      Parameter.queryMap("labels", "key2", "value2")
    ) should be("nocache=false&labels=%7B%22key1%22%3A%22value1%22%2C%22key2%22%3A%22value2%22%7D")
  }

  // -------------------------------------------- Utility methods

  private def renderQueryString(criteria: Parameter[_]*): String = {
    Query.fromMap(Parameter.compileQuery(criteria.toSeq)).renderString
  }

}
