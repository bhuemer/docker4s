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
package org.docker4s.api

import io.circe.Json
import org.http4s.QueryParamEncoder

/**
  * The Docker API typically allows for two different ways of specifying criteria:
  *  - As a query parameter in the URI directly, e.g. `?all=true`, or
  *  - As a field in a JSON object that itself is passed as a query parameter, e.g.
  *     `?filters=filters=%7B%22dangling%22%3A%5B%22true%22%5D%7D`
  *
  * These criterion objects allow APIs to specify the distinction - which parameter is used when.
  */
sealed trait Criterion[T]

object Criterion {

  def query[T, A: QueryParamEncoder](name: String, value: A): Criterion[T] =
    Query(name, QueryParamEncoder[A].encode(value).value)

  def filter[T](name: String, value: String): Criterion[T] = Filter(name, value)

  /**
    * Represents criteria in conditions that are included as query parameters in the URL.
    */
  case class Query[T](name: String, value: String) extends Criterion[T]

  /**
    * Represents criteria in conditions that are included in the encoded JSON object passed in the `filters` parameter.
    */
  case class Filter[T](name: String, value: String) extends Criterion[T]

  def compile(criteria: Seq[Criterion[_]]): Map[String, Seq[String]] = {
    // Collect all the criteria that ought to be used as query parameters directly ..
    val queries = criteria
      .collect({
        case query: Query[_] => query
      })
      .groupBy(_.name)
      .mapValues(_.map(_.value))

    // .. and also collect the ones that ought to be used as part of a JSON encoded object separately.
    val filters = criteria
      .collect({
        case filter: Filter[_] => filter
      })
      .groupBy(_.name)
      .mapValues(_.map(_.value))

    val filtersJson = Json.obj(filters.mapValues(values => Json.fromValues(values.map(Json.fromString))).toSeq: _*)

    queries + ("filters" -> Seq(filtersJson.noSpaces))
  }

  /**
    * Formats the given criteria for logging statements.
    */
  def toDebugString(criteria: Seq[Criterion[_]]): String = {
    val criteriaAsMap = criteria
      .map({
        case Query(name, value)  => (name, value)
        case Filter(name, value) => (name, value)
      })
      .groupBy(_._1)
      .mapValues(_.map(_._2))

    Json
      .obj(
        criteriaAsMap
          .mapValues({ values =>
            Json.arr(values.map(Json.fromString): _*)
          })
          .toSeq: _*)
      .noSpaces
  }

}
