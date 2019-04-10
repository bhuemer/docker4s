package org.docker4s.models.images

import org.scalatest.{FlatSpec, Matchers}

class BuildEventTest extends FlatSpec with Matchers {

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

  // -------------------------------------------- Utility methods

  private def decodeBuildEvent(str: String): BuildEvent = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(BuildEvent.decoder).fold(throw _, Predef.identity)
  }

}
