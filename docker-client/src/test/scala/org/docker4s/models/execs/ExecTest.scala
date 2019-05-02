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
package org.docker4s.models.execs

import org.docker4s.models.ModelsSpec
import org.docker4s.models.containers.Container

class ExecTest extends ModelsSpec {

  "Decoding JSON into exec details" should "work" in {
    val exec = decode(
      """{
        |  "ID": "9de882757c71f73c5c46a6eb4ab1dd40196465f7dd36bff58a3928986bfd6e33",
        |  "Running": true,
        |  "ExitCode": null,
        |  "ProcessConfig": {
        |    "tty": true,
        |    "entrypoint": "/bin/bash",
        |    "arguments": [],
        |    "privileged": false,
        |    "user": "1000"
        |  },
        |  "OpenStdin": true,
        |  "OpenStderr": true,
        |  "OpenStdout": true,
        |  "CanRemove": false,
        |  "ContainerID": "7158ca16e90e745419cd6da40e1ae2e941a7115ee48f6ebf07826ba06fdff7fc",
        |  "DetachKeys": "",
        |  "Pid":36653
        |}""".stripMargin,
      Exec.decoder
    )
    exec should be(
      Exec(
        id = Exec.Id("9de882757c71f73c5c46a6eb4ab1dd40196465f7dd36bff58a3928986bfd6e33"),
        containerId = Container.Id("7158ca16e90e745419cd6da40e1ae2e941a7115ee48f6ebf07826ba06fdff7fc"),
        running = true,
        exitCode = None,
        processConfig = Exec.ProcessConfig(
          tty = true,
          entryPoint = "/bin/bash",
          args = List.empty,
          privileged = false,
          user = "1000"
        ),
        detachKeys = None,
        canRemove = false,
        openStdin = true,
        openStderr = true,
        openStdout = true,
        pid = 36653
      ))
  }

}
