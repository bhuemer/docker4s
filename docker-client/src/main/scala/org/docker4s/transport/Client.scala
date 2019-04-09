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

import cats.effect.Effect
import fs2.Stream
import io.circe.Decoder
import org.docker4s.api.Criterion
import org.http4s.{EntityEncoder, QueryParamEncoder, QueryParamKeyLike, Status, Uri}

import scala.language.higherKinds

trait Client[F[_]] {

  def get(path: String): Client.RequestBuilder[F]

  def head(path: String): Client.RequestBuilder[F]

  def post(path: String): Client.RequestBuilder[F]

  def delete(path: String): Client.RequestBuilder[F]

}

object Client {

  trait RequestBuilder[F[_]] {

    def body[T](entity: T)(implicit encoder: EntityEncoder[F, T]): RequestBuilder[F]

    def queryParam[T: QueryParamEncoder, K: QueryParamKeyLike](key: K, value: T): RequestBuilder[F]

    def queryParam[T: QueryParamEncoder, K: QueryParamKeyLike](key: K, value: Option[T]): RequestBuilder[F]

    def queryParam[T: QueryParamEncoder, K: QueryParamKeyLike](key: K, values: Seq[T]): RequestBuilder[F]

    /**
      */
    def criteria(criteria: Seq[Criterion[_]]): RequestBuilder[F]

    def on(status: Status): StatusHandler[F]

    def execute: F[Unit]

    def expect[A](decoder: Decoder[A]): F[A]

    def expectMany[A](decoder: Decoder[A]): F[List[A]] = expect(Decoder.decodeList(decoder))

    def stream: Stream[F, Byte]

    def stream[A](decoder: Decoder[A]): Stream[F, A]

  }

  trait StatusHandler[F[_]] {
    def raise(handler: (String, String) => Exception): RequestBuilder[F]
  }

  /**
    *
    */
  def from[F[_]: Effect](client: org.http4s.client.Client[F], uri: Uri): Client[F] = new Http4sClient[F](client, uri)

}
