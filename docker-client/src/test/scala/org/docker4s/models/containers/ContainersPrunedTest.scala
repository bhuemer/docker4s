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

import org.docker4s.models.ModelsSpec

/**
  * Contains test cases related to parsing [[ContainersPruned]] objects from JSON response bodies.
  */
class ContainersPrunedTest extends ModelsSpec {

  "Decoding JSON into containers pruned" should "work" in {
    val containersPruned =
      decodeContainersPruned("""{
        |  "ContainersDeleted": [
        |    "6104d001c7ac0eb59538747edd8197c4f56da5c7f4a399023edd85fb88fffb58",
        |    "b088c99e83f6370491e298963e23d7c0ba14637ccf482330703bc58a4a3247dc",
        |    "fc4545ec82606c93070e69c5bc758430324f0490fc1742bdaa06b958b34d1768"
        |  ],
        |  "SpaceReclaimed": 166468
        |}""".stripMargin)
    containersPruned should be(
      ContainersPruned(
        containers = List(
          Container.Id("6104d001c7ac0eb59538747edd8197c4f56da5c7f4a399023edd85fb88fffb58"),
          Container.Id("b088c99e83f6370491e298963e23d7c0ba14637ccf482330703bc58a4a3247dc"),
          Container.Id("fc4545ec82606c93070e69c5bc758430324f0490fc1742bdaa06b958b34d1768")
        ),
        spaceReclaimed = 166468L
      )
    )
  }

  "Decoding JSON into containers pruned" should "cope with empty lists" in {
    decodeContainersPruned("""{
        |  "ContainersDeleted": null,
        |  "SpaceReclaimed": 0
        |}
     """.stripMargin) should be(ContainersPruned(containers = List.empty, spaceReclaimed = 0L))

    decodeContainersPruned("""{
        |  "ContainersDeleted": [],
        |  "SpaceReclaimed": 0
        |}
     """.stripMargin) should be(ContainersPruned(containers = List.empty, spaceReclaimed = 0L))
  }

  private def decodeContainersPruned(str: String): ContainersPruned = decode(str, ContainersPruned.decoder)

}
