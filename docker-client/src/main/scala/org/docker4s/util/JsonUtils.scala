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
package org.docker4s.util

import io.circe.Json
import io.circe.Json.fromJsonObject

object JsonUtils {

  /**
    * Perform a deep merge of two JSON values.
    *
    * Unlike Circe's default implementation of merging, this will also treat arrays specially and concatenate them.
    */
  def merge(lhs: Json, rhs: Json): Json = {
    // Attempt #1: Merge two objects
    (lhs.asObject, rhs.asObject) match {
      case (Some(a), Some(b)) =>
        fromJsonObject(
          a.toList.foldLeft(b) {
            case (acc, (key, value)) =>
              b(key).fold(acc.add(key, value)) { r =>
                acc.add(key, merge(value, r))
              }
          }
        )

      case _ =>
        // Attempt #2: Merge two arrays
        (lhs.asArray, rhs.asArray) match {
          case (Some(a), Some(b)) => Json.arr(a ++ b: _*)

          // Other values don't get merged and we'll simply assume that the
          // "newer" value (rhs) is supposed to replace the previous one.
          case _ => rhs
        }
    }
  }

}
