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
package org.docker4s.models.plugins

import org.scalatest.{FlatSpec, Matchers}

class PluginsTest extends FlatSpec with Matchers {

  "Decoding JSON into plugins" should "decode the `vieux/sshfs` plugin" ignore {
    val plugin =
      decodePlugin("""{
      |  "Config": {
      |    "Args": {
      |      "Description": "",
      |      "Name": "",
      |      "Settable": null,
      |      "Value": null
      |    },
      |    "Description": "sshFS plugin for Docker",
      |    "DockerVersion": "18.05.0-ce-rc1",
      |    "Documentation": "https://docs.docker.com/engine/extend/plugins/",
      |    "Entrypoint": [
      |      "/docker-volume-sshfs"
      |    ],
      |    "Env": [
      |      {
      |        "Description": "",
      |        "Name": "DEBUG",
      |        "Settable": ["value"],
      |        "Value": "0"
      |      }
      |    ],
      |    "Interface": {
      |      "Socket": "sshfs.sock",
      |      "Types": [ "docker.volumedriver/1.0" ]
      |    },
      |    "IpcHost": false,
      |    "Linux": {
      |      "AllowAllDevices": false,
      |      "Capabilities": [ "CAP_SYS_ADMIN" ],
      |      "Devices": [
      |        {
      |          "Description": "",
      |          "Name": "",
      |          "Path": "/dev/fuse",
      |          "Settable": null
      |        }
      |      ]
      |    },
      |    "Mounts": [
      |      {
      |        "Description": "",
      |        "Destination": "/mnt/state",
      |        "Name": "state",
      |        "Options": ["rbind"],
      |        "Settable": ["source"],
      |        "Source": "/var/lib/docker/plugins/",
      |        "Type": "bind"
      |      },
      |      {
      |        "Description": "",
      |        "Destination": "/root/.ssh",
      |        "Name": "sshkey",
      |        "Options": ["rbind"],
      |        "Settable": ["source"],
      |        "Source": "",
      |        "Type": "bind"
      |      }
      |    ],
      |    "Network": { "Type": "host" },
      |    "PidHost": false,
      |    "PropagatedMount": "/mnt/volumes",
      |    "User": {},
      |    "WorkDir": "",
      |    "rootfs": {
      |      "diff_ids": ["sha256:ce2b7a99c5db05cfe263bcd3640f2c1ce7c6f4619339633d44e65a8168ec3587"],
      |      "type":"layers"
      |    }
      |  },
      |  "Enabled": true,
      |  "Id": "33728ef391b4c49fde34b16e5e2704de83e5b91786acb3ce0425ba6a120fb107",
      |  "Name": "vieux/sshfs:latest",
      |  "PluginReference": "docker.io/vieux/sshfs:latest",
      |  "Settings": {
      |    "Args": [],
      |    "Devices": [
      |      {
      |        "Description": "",
      |        "Name": "",
      |        "Path": "/dev/fuse",
      |        "Settable": null
      |      }
      |    ],
      |    "Env": ["DEBUG=0"],
      |    "Mounts": [
      |      {
      |        "Description": "",
      |        "Destination": "/mnt/state",
      |        "Name": "state",
      |        "Options": ["rbind"],
      |        "Settable": ["source"],
      |        "Source": "/var/lib/docker/plugins/",
      |        "Type": "bind"
      |      },
      |      {
      |        "Description": "",
      |        "Destination": "/root/.ssh",
      |        "Name": "sshkey",
      |        "Options": ["rbind"],
      |        "Settable": ["source"],
      |        "Source":"",
      |        "Type":"bind"
      |      }
      |    ]
      |  }
      |}""".stripMargin)

  }

  // -------------------------------------------- Utility methods

  private def decodePlugin(str: String): Plugin = {
    val json = io.circe.parser.parse(str).fold(throw _, Predef.identity)
    json.as(Plugin.decoder).fold(throw _, Predef.identity)
  }

}
