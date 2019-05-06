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
package org.docker4s.models.images

import org.docker4s.models.ModelsSpec

class BuildEventTest extends ModelsSpec {

  "Decoding JSON into build events" should "decode pull events" in {
    val buildEvent =
      decodeBuildEvent("""{ "status": "Download complete", "progressDetail": {}, "id": "4afad9c4aba6" }""")
    buildEvent should be(BuildEvent.Pull(PullEvent.Layer.Downloaded("4afad9c4aba6")))
  }

  "Decoding JSON into build events" should "decode `aux` events" in {
    val buildEvent =
      decodeBuildEvent("""{"aux":{"ID":"sha256:4ba672ac8fec77ce68c1ca27ab28b29fc3e734ecc1fcdfa52d235a742598b32c"}}""")
    buildEvent should be(
      BuildEvent.Built(Image.Id("sha256:4ba672ac8fec77ce68c1ca27ab28b29fc3e734ecc1fcdfa52d235a742598b32c")))
  }

  "Decoding JSON into build events" should "decode `stream` events" in {
    val buildEvent =
      decodeBuildEvent("""{"stream":"Step 3/3 : CMD [\"cat\", \"README.md\"]"}""")
    buildEvent should be(BuildEvent.Stream("Step 3/3 : CMD [\"cat\", \"README.md\"]"))
  }

  private def decodeBuildEvent(str: String): BuildEvent = decode(str, BuildEvent.decoder)

}
