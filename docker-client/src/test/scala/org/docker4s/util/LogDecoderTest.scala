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
package org.docker4s.util

import java.nio.charset.StandardCharsets

import fs2.{Chunk, Stream}
import org.docker4s.api.Containers
import org.docker4s.api.Containers.Log
import org.docker4s.api.Containers.Stream.{StdErr, StdOut}
import org.scalatest.{FlatSpec, Matchers}

class LogDecoderTest extends FlatSpec with Matchers {

  /** Makes sure that we can decode log messages even if the header frame (these initial 8 bytes) aren't included. */
  "Decoding log streams" should "cope with missing headers" in {
    val stream =
      Stream.emits(Seq("Hello from", "some fake", "Docker log").mkString("\n").getBytes(StandardCharsets.UTF_8))
    val result = stream.through(LogDecoder.decode).compile.toList
    result should be(
      List(
        Containers.Log(Containers.Stream.StdOut, "Hello from"),
        Containers.Log(Containers.Stream.StdOut, "some fake"),
        Containers.Log(Containers.Stream.StdOut, "Docker log")
      ))
  }

  /**
    * Decodes the logs from an actual log stream generated via `docker run -p 8888:8888 jupyter/base-notebook`.
    */
  "Decoding log streams" should "cope with multiplexed streams " in {
    // @formatter:off
    val stream = Stream.emits(Array[Byte](
      1, 0, 0, 0, 0, 0, 0, 93, 50, 48, 49, 57, 45, 48, 51, 45, 50, 52, 84, 49, 52, 58, 48, 51, 58, 52, 55, 46, 56, 55, 51, 49, 53, 55, 48, 48, 48, 90, 32, 67, 111, 110, 116, 97, 105, 110, 101, 114, 32, 109, 117, 115, 116, 32, 98, 101, 32, 114, 117, 110, 32, 119, 105, 116, 104, 32, 103, 114, 111, 117, 112, 32, 34, 114, 111, 111, 116, 34, 32, 116, 111, 32, 117, 112, 100, 97, 116, 101, 32, 112, 97, 115, 115, 119, 100, 32, 102, 105, 108, 101, 10,
      1, 0, 0, 0, 0, 0, 0, 71, 50, 48, 49, 57, 45, 48, 51, 45, 50, 52, 84, 49, 52, 58, 48, 51, 58, 52, 55, 46, 56, 55, 51, 50, 48, 53, 51, 48, 48, 90, 32, 69, 120, 101, 99, 117, 116, 105, 110, 103, 32, 116, 104, 101, 32, 99, 111, 109, 109, 97, 110, 100, 58, 32, 106, 117, 112, 121, 116, 101, 114, 32, 110, 111, 116, 101, 98, 111, 111, 107, 10,
      2, 0, 0, 0, 0, 0, 0, -90, 50, 48, 49, 57, 45, 48, 51, 45, 50, 52, 84, 49, 52, 58, 48, 51, 58, 52, 57, 46, 48, 54, 55, 54, 55, 52, 50, 48, 48, 90, 32, 91, 73, 32, 49, 52, 58, 48, 51, 58, 52, 57, 46, 48, 54, 54, 32, 78, 111, 116, 101, 98, 111, 111, 107, 65, 112, 112, 93, 32, 87, 114, 105, 116, 105, 110, 103, 32, 110, 111, 116, 101, 98, 111, 111, 107, 32, 115, 101, 114, 118, 101, 114, 32, 99, 111, 111, 107, 105, 101, 32, 115, 101, 99, 114, 101, 116, 32, 116, 111, 32, 47, 104, 111, 109, 101, 47, 106, 111, 118, 121, 97, 110, 47, 46, 108, 111, 99, 97, 108, 47, 115, 104, 97, 114, 101, 47, 106, 117, 112, 121, 116, 101, 114, 47, 114, 117, 110, 116, 105, 109, 101, 47, 110, 111, 116, 101, 98, 111, 111, 107, 95, 99, 111, 111, 107, 105, 101, 95, 115, 101, 99, 114, 101, 116, 10,
      2, 0, 0, 0, 0, 0, 0, -113, 50, 48, 49, 57, 45, 48, 51, 45, 50, 52, 84, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 52, 51, 57, 51, 54, 48, 48, 90, 32, 91, 73, 32, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 51, 32, 78, 111, 116, 101, 98, 111, 111, 107, 65, 112, 112, 93, 32, 74, 117, 112, 121, 116, 101, 114, 76, 97, 98, 32, 101, 120, 116, 101, 110, 115, 105, 111, 110, 32, 108, 111, 97, 100, 101, 100, 32, 102, 114, 111, 109, 32, 47, 111, 112, 116, 47, 99, 111, 110, 100, 97, 47, 108, 105, 98, 47, 112, 121, 116, 104, 111, 110, 51, 46, 55, 47, 115, 105, 116, 101, 45, 112, 97, 99, 107, 97, 103, 101, 115, 47, 106, 117, 112, 121, 116, 101, 114, 108, 97, 98, 10,
      2, 0, 0, 0, 0, 0, 0, 125, 50, 48, 49, 57, 45, 48, 51, 45, 50, 52, 84, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 52, 54, 57, 54, 55, 48, 48, 90, 32, 91, 73, 32, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 52, 32, 78, 111, 116, 101, 98, 111, 111, 107, 65, 112, 112, 93, 32, 74, 117, 112, 121, 116, 101, 114, 76, 97, 98, 32, 97, 112, 112, 108, 105, 99, 97, 116, 105, 111, 110, 32, 100, 105, 114, 101, 99, 116, 111, 114, 121, 32, 105, 115, 32, 47, 111, 112, 116, 47, 99, 111, 110, 100, 97, 47, 115, 104, 97, 114, 101, 47, 106, 117, 112, 121, 116, 101, 114, 47, 108, 97, 98, 10,
      2, 0, 0, 0, 0, 0, 0, 113, 50, 48, 49, 57, 45, 48, 51, 45, 50, 52, 84, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 56, 50, 50, 53, 56, 48, 48, 90, 32, 91, 73, 32, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 55, 32, 78, 111, 116, 101, 98, 111, 111, 107, 65, 112, 112, 93, 32, 83, 101, 114, 118, 105, 110, 103, 32, 110, 111, 116, 101, 98, 111, 111, 107, 115, 32, 102, 114, 111, 109, 32, 108, 111, 99, 97, 108, 32, 100, 105, 114, 101, 99, 116, 111, 114, 121, 58, 32, 47, 104, 111, 109, 101, 47, 106, 111, 118, 121, 97, 110, 10,
      2, 0, 0, 0, 0, 0, 0, 96, 50, 48, 49, 57, 45, 48, 51, 45, 50, 52, 84, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 56, 52, 55, 53, 50, 48, 48, 90, 32, 91, 73, 32, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 56, 32, 78, 111, 116, 101, 98, 111, 111, 107, 65, 112, 112, 93, 32, 84, 104, 101, 32, 74, 117, 112, 121, 116, 101, 114, 32, 78, 111, 116, 101, 98, 111, 111, 107, 32, 105, 115, 32, 114, 117, 110, 110, 105, 110, 103, 32, 97, 116, 58, 10,
      2, 0, 0, 0, 0, 0, 0, -100, 50, 48, 49, 57, 45, 48, 51, 45, 50, 52, 84, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 56, 54, 54, 55, 55, 48, 48, 90, 32, 91, 73, 32, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 56, 32, 78, 111, 116, 101, 98, 111, 111, 107, 65, 112, 112, 93, 32, 104, 116, 116, 112, 58, 47, 47, 40, 49, 101, 98, 97, 48, 56, 53, 55, 100, 55, 101, 48, 32, 111, 114, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 41, 58, 56, 56, 56, 56, 47, 63, 116, 111, 107, 101, 110, 61, 52, 98, 48, 98, 99, 51, 52, 52, 101, 101, 100, 55, 97, 49, 98, 53, 53, 56, 99, 100, 54, 102, 55, 98, 97, 51, 101, 52, 97, 97, 51, 98, 53, 53, 98, 55, 101, 52, 53, 55, 51, 55, 53, 53, 48, 100, 48, 98, 10,
      2, 0, 0, 0, 0, 0, 0, -106, 50, 48, 49, 57, 45, 48, 51, 45, 50, 52, 84, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 56, 57, 52, 51, 57, 48, 48, 90, 32, 91, 73, 32, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 56, 32, 78, 111, 116, 101, 98, 111, 111, 107, 65, 112, 112, 93, 32, 85, 115, 101, 32, 67, 111, 110, 116, 114, 111, 108, 45, 67, 32, 116, 111, 32, 115, 116, 111, 112, 32, 116, 104, 105, 115, 32, 115, 101, 114, 118, 101, 114, 32, 97, 110, 100, 32, 115, 104, 117, 116, 32, 100, 111, 119, 110, 32, 97, 108, 108, 32, 107, 101, 114, 110, 101, 108, 115, 32, 40, 116, 119, 105, 99, 101, 32, 116, 111, 32, 115, 107, 105, 112, 32, 99, 111, 110, 102, 105, 114, 109, 97, 116, 105, 111, 110, 41, 46, 10, 
      2, 0, 0, 0, 0, 0, 0, 61, 50, 48, 49, 57, 45, 48, 51, 45, 50, 52, 84, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 57, 54, 55, 54, 49, 48, 48, 90, 32, 91, 67, 32, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 57, 32, 78, 111, 116, 101, 98, 111, 111, 107, 65, 112, 112, 93, 32, 10,
      2, 0, 0, 0, 0, 0, 0, 36, 50, 48, 49, 57, 45, 48, 51, 45, 50, 52, 84, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 57, 54, 57, 56, 53, 48, 48, 90, 32, 32, 32, 32, 32, 10,
      2, 0, 0, 0, 0, 0, 0, 110, 50, 48, 49, 57, 45, 48, 51, 45, 50, 52, 84, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 57, 55, 49, 54, 56, 48, 48, 90, 32, 32, 32, 32, 32, 67, 111, 112, 121, 47, 112, 97, 115, 116, 101, 32, 116, 104, 105, 115, 32, 85, 82, 76, 32, 105, 110, 116, 111, 32, 121, 111, 117, 114, 32, 98, 114, 111, 119, 115, 101, 114, 32, 119, 104, 101, 110, 32, 121, 111, 117, 32, 99, 111, 110, 110, 101, 99, 116, 32, 102, 111, 114, 32, 116, 104, 101, 32, 102, 105, 114, 115, 116, 32, 116, 105, 109, 101, 44, 10,
      2, 0, 0, 0, 0, 0, 0, 58, 50, 48, 49, 57, 45, 48, 51, 45, 50, 52, 84, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 57, 55, 51, 53, 48, 48, 48, 90, 32, 32, 32, 32, 32, 116, 111, 32, 108, 111, 103, 105, 110, 32, 119, 105, 116, 104, 32, 97, 32, 116, 111, 107, 101, 110, 58, 10,
      2, 0, 0, 0, 0, 0, 0, -121, 50, 48, 49, 57, 45, 48, 51, 45, 50, 52, 84, 49, 52, 58, 48, 51, 58, 52, 57, 46, 55, 50, 57, 55, 53, 51, 49, 48, 48, 90, 32, 32, 32, 32, 32, 32, 32, 32, 32, 104, 116, 116, 112, 58, 47, 47, 40, 49, 101, 98, 97, 48, 56, 53, 55, 100, 55, 101, 48, 32, 111, 114, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 41, 58, 56, 56, 56, 56, 47, 63, 116, 111, 107, 101, 110, 61, 52, 98, 48, 98, 99, 51, 52, 52, 101, 101, 100, 55, 97, 49, 98, 53, 53, 56, 99, 100, 54, 102, 55, 98, 97, 51, 101, 52, 97, 97, 51, 98, 53, 53, 98, 55, 101, 52, 53, 55, 51, 55, 53, 53, 48, 100, 48, 98, 10
    ))
    
    val result = stream.through(LogDecoder.decode).compile.toList
    result should be(List(
      Log(StdOut, "2019-03-24T14:03:47.873157000Z Container must be run with group \"root\" to update passwd file"), 
      Log(StdOut, "2019-03-24T14:03:47.873205300Z Executing the command: jupyter notebook"), 
      Log(StdErr, "2019-03-24T14:03:49.067674200Z [I 14:03:49.066 NotebookApp] Writing notebook server cookie secret to /home/jovyan/.local/share/jupyter/runtime/notebook_cookie_secret"), 
      Log(StdErr, "2019-03-24T14:03:49.724393600Z [I 14:03:49.723 NotebookApp] JupyterLab extension loaded from /opt/conda/lib/python3.7/site-packages/jupyterlab"), 
      Log(StdErr, "2019-03-24T14:03:49.724696700Z [I 14:03:49.724 NotebookApp] JupyterLab application directory is /opt/conda/share/jupyter/lab"), 
      Log(StdErr, "2019-03-24T14:03:49.728225800Z [I 14:03:49.727 NotebookApp] Serving notebooks from local directory: /home/jovyan"), 
      Log(StdErr, "2019-03-24T14:03:49.728475200Z [I 14:03:49.728 NotebookApp] The Jupyter Notebook is running at:"), 
      Log(StdErr, "2019-03-24T14:03:49.728667700Z [I 14:03:49.728 NotebookApp] http://(1eba0857d7e0 or 127.0.0.1):8888/?token=4b0bc344eed7a1b558cd6f7ba3e4aa3b55b7e45737550d0b"), 
      Log(StdErr, "2019-03-24T14:03:49.728943900Z [I 14:03:49.728 NotebookApp] Use Control-C to stop this server and shut down all kernels (twice to skip confirmation)."), 
      Log(StdErr, "2019-03-24T14:03:49.729676100Z [C 14:03:49.729 NotebookApp]"),
      Log(StdErr, "2019-03-24T14:03:49.729698500Z"), 
      Log(StdErr, "2019-03-24T14:03:49.729716800Z     Copy/paste this URL into your browser when you connect for the first time,"), 
      Log(StdErr, "2019-03-24T14:03:49.729735000Z     to login with a token:"), 
      Log(StdErr, "2019-03-24T14:03:49.729753100Z         http://(1eba0857d7e0 or 127.0.0.1):8888/?token=4b0bc344eed7a1b558cd6f7ba3e4aa3b55b7e45737550d0b")
    ))
    // @formatter:on
  }

  "Decoding log headers" should "manage to decode the frame size correctly" in {
    def evaluate(bytes: Array[Byte]) = LogDecoder.decodeHeader(Chunk.bytes(bytes))

    evaluate(Array(1, 0, 0, 0, 0, 0, 0, 69)) should be(Some((Containers.Stream.StdOut, 69)))
    evaluate(Array(1, 0, 0, 0, 0, 0, 0, 40)) should be(Some((Containers.Stream.StdOut, 40)))
    evaluate(Array(2, 0, 0, 0, 0, 0, 0, -121)) should be(Some((Containers.Stream.StdErr, 135)))
    evaluate(Array(2, 0, 0, 0, 0, 0, 4, -23)) should be(Some((Containers.Stream.StdErr, 1257)))
  }

}
