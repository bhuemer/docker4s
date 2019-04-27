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

import io.circe.{Encoder, Json}
import org.docker4s.util.JsonUtils
import org.http4s.QueryParamEncoder

/**
  * The Docker API typically allows for two different ways of specifying criteria:
  *  - As a query parameter in the URI directly, e.g. `?all=true`, or
  *  - As a field in a JSON object that itself is passed as a query parameter, e.g.
  *     `?filters=filters=%7B%22dangling%22%3A%5B%22true%22%5D%7D`
  *
  * These criterion objects allow APIs to specify the distinction - which parameter is used when.
  */
sealed trait Parameter[T]

object Parameter {

  def query[T, A](name: String, value: A)(implicit encoder: QueryParamEncoder[A]): Parameter[T] =
    Query(name, Seq(encoder.encode(value).value))

  def query[T, A](name: String, value: Option[A])(implicit encoder: QueryParamEncoder[A]): Parameter[T] =
    Query(name, value.map(encoder.encode(_).value).toSeq)

  def query[T, A](name: String, values: Seq[A])(implicit encoder: QueryParamEncoder[A]): Parameter[T] =
    Query(name, values.map({ value =>
      encoder.encode(value).value
    }))

  def queryArr[T, A](name: String, value: A)(implicit encoder: Encoder[A]): Parameter[T] =
    Query.Array(name, Seq(encoder(value)))

  def queryMap[T](name: String, key: String, value: String): Parameter[T] = Query.Map(name, key, value)

  def filter[T](name: String, value: String): Parameter[T] = Filter(name, Seq(value))

  def filter[T](name: String, value: Option[String]): Parameter[T] = Filter(name, value.toSeq)

  def filter[T](name: String, values: Seq[String]): Parameter[T] = Filter(name, values)

  def body[T](value: Json): Parameter[T] = Body(value)

  def body[T, A](name: String, value: A)(implicit encoder: Encoder[A]): Parameter[T] =
    Body(Json.obj(name -> encoder(value)))

  /**
    * Represents a parameter that is included as a query parameter in the URL.
    */
  case class Query[T](name: String, values: Seq[String]) extends Parameter[T]

  object Query {

    /**
      * Represents parameters that are to be encoded as JSON arrays in the URI's query part.
      */
    case class Array[T](name: String, values: Seq[Json]) extends Parameter[T]

    /**
      * Represents parameters that are to be encoded as JSON maps in the URI's query part.
      */
    case class Map[T](name: String, key: String, value: String) extends Parameter[T]

  }

  /**
    * Represents criteria in conditions that are included in the encoded JSON object passed in the `filters` parameter.
    */
  case class Filter[T](name: String, values: Seq[String]) extends Parameter[T]

  /**
    *
    */
  case class Body[T](value: Json) extends Parameter[T]

  def compileQuery(criteria: Seq[Parameter[_]]): Map[String, Seq[String]] = {
    val jsonArrQueries = criteria
      .collect({
        case query: Query.Array[_] => query
      })
      .groupBy(_.name)
      .mapValues({ values =>
        Seq(Json.arr(values.flatMap(_.values): _*).noSpaces)
      })

    val jsonMapQueries = criteria
      .collect({
        case query: Query.Map[_] => query
      })
      .groupBy(_.name)
      .mapValues({ values =>
        Seq(
          Json
            .obj(values
              .map({ value =>
                (value.key, Json.fromString(value.value))
              }): _*)
            .noSpaces)
      })

    // Collect all the criteria that ought to be used as query parameters directly ..
    val queries = criteria
      .collect({
        case query: Query[_] => query
      })
      .groupBy(_.name)
      .mapValues(_.flatMap(_.values))

    // .. and also collect the ones that ought to be used as part of a JSON encoded object separately.
    val filters = criteria
      .collect({
        case filter: Filter[_] => filter
      })
      .groupBy(_.name)
      .mapValues(_.flatMap(_.values))

    if (filters.nonEmpty) {
      val filtersJson = Json.obj(filters.mapValues(values => Json.fromValues(values.map(Json.fromString))).toSeq: _*)
      queries ++ jsonArrQueries ++ jsonMapQueries + ("filters" -> Seq(filtersJson.noSpaces))
    } else {
      queries ++ jsonArrQueries ++ jsonMapQueries
    }
  }

  def compileBody(parameters: Seq[Parameter[_]]): Option[Json] = {
    val bodyParameters = parameters.collect({
      case Body(value) => value
    })
    if (bodyParameters.isEmpty) {
      None
    } else {
      Some(bodyParameters.reduce(JsonUtils.merge))
    }
  }

  /**
    * Formats the given criteria for logging statements.
    */
  def toDebugString(criteria: Seq[Parameter[_]]): String = {
    val criteriaAsMap = criteria
      .map({
        case Query(name, values)         => (name, values)
        case Query.Array(name, values)   => (name, values.map(_.noSpaces))
        case Query.Map(name, key, value) => (name, Seq(s"$key=$value"))
        case Filter(name, values)        => (name, values)
        case Body(value)                 => ("body", Seq(value.noSpaces))
      })
      .groupBy(_._1)
      .mapValues(_.flatMap(_._2))

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
