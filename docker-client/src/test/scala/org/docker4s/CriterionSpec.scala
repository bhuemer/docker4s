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

import org.http4s.Query
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class CriterionSpec extends FlatSpec with Matchers {

  /**
    * Makes sure that query parameters and JSON encoded filter parameters can be mixed in a single query string.
    */
  "Building criteria" should "encode `filters` as JSON" in {
    renderQueryString(
      Criterion.Query("all", "true"),
      Criterion.Query("digests", "false"),
      Criterion.Filter("dangling", "true"),
      Criterion.Filter("before", "image-name")
    ) should be(
      "all=true&digests=false&filters=%7B%22dangling%22%3A%5B%22true%22%5D%2C%22before%22%3A%5B%22image-name%22%5D%7D")
  }

  // -------------------------------------------- Utility methods

  private def renderQueryString(criteria: Criterion[_]*): String = {
    val parameters = Criterion.build(criteria.toSeq)
    Query.fromMap(parameters).renderString
  }

}
