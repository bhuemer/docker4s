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

import java.time.ZonedDateTime

import org.docker4s.models.ModelsSpec
import org.docker4s.models.images.Image
import org.docker4s.models.networks.{Endpoint, Network}

class ContainerSummaryTest extends ModelsSpec {

  "Decoding JSON into container summaries" should "successfully decode hello-world containers" in {
    val containerSummary =
      decodeContainerSummary("""{
      |  "Command": "/hello",
      |  "Created": 1552349820,
      |  "HostConfig": {
      |    "NetworkMode": "default"
      |  },
      |  "Id": "191bdb92cc525e317d8efd897e109aeb6439688fa477f057309e76387b4fb43a",
      |  "Image": "hello-world",
      |  "ImageID": "sha256:fce289e99eb9bca977dae136fbe2a82b6b7d4c372474c9235adc1741675f587e",
      |  "Labels": {},
      |  "Mounts": [],
      |  "Names": [
      |    "/fervent_chatelet"
      |  ],
      |  "NetworkSettings": {
      |    "Networks": {
      |      "bridge": {
      |        "Aliases": null,
      |        "DriverOpts": null,
      |        "EndpointID": "",
      |        "Gateway": "",
      |        "GlobalIPv6Address": "",
      |        "GlobalIPv6PrefixLen": 0,
      |        "IPAMConfig": null,
      |        "IPAddress": "",
      |        "IPPrefixLen": 0,
      |        "IPv6Gateway": "",
      |        "Links": null,
      |        "MacAddress": "",
      |        "NetworkID": "42d2ef1d4ca95d5e6b2c80c1f6dff08f905e1c0a6f94c2ba8bcbd50d9ea13bb9"
      |      }
      |    }
      |  },
      |  "Ports": [],
      |  "State": "exited",
      |  "Status": "Exited (0) 6 days ago"
      |}""".stripMargin)

    containerSummary should be(
      ContainerSummary(
        id = Container.Id("191bdb92cc525e317d8efd897e109aeb6439688fa477f057309e76387b4fb43a"),
        names = List("/fervent_chatelet"),
        imageName = "hello-world",
        imageId = Image.Id("sha256:fce289e99eb9bca977dae136fbe2a82b6b7d4c372474c9235adc1741675f587e"),
        command = "/hello",
        createdAt = ZonedDateTime.parse("2019-03-12T00:17Z"),
        ports = List.empty,
        sizeRw = None,
        sizeRootFs = None,
        labels = Map.empty,
        state = Container.Status.Exited,
        status = "Exited (0) 6 days ago",
        networkMode = HostConfig.NetworkMode.Default,
        networks = Map("bridge" -> Endpoint.Settings(
          ipamConfig = None,
          links = List.empty,
          aliases = List.empty,
          networkId = Network.Id("42d2ef1d4ca95d5e6b2c80c1f6dff08f905e1c0a6f94c2ba8bcbd50d9ea13bb9"),
          endpointId = Endpoint.Id(""),
          gateway = "",
          ipAddress = "",
          ipPrefixLen = 0,
          ipv6Gateway = "",
          globalIpv6Address = "",
          globalIpv6PrefixLen = 0,
          macAddress = "",
          driverOpts = Map.empty
        )),
        mounts = List.empty
      ))
  }

  "Decoding JSON into container summaries" should "successfully decode exposed ports" in {
    val containerSummary =
      decodeContainerSummary("""
      |{
      |  "Id" : "12a6a4da97dd4c43db609ecdd22ad6dffc95cf2fe9a8a9fe2b84038eba093053",
      |  "Names" : [
      |    "/sad_wescoff"
      |  ],
      |  "Image" : "jupyter/base-notebook",
      |  "ImageID" : "sha256:0fe41535dcac2fb8bee08dbb26895aa645ba38109c162b390e5bb45948f8ac35",
      |  "Command" : "tini -g -- start-notebook.sh",
      |  "Created" : 1553442354,
      |  "Ports" : [
      |    {
      |      "IP" : "0.0.0.0",
      |      "PrivatePort" : 8888,
      |      "PublicPort" : 8888,
      |      "Type" : "tcp"
      |    }
      |  ],
      |  "Labels" : {
      |    "maintainer" : "Jupyter Project <jupyter@googlegroups.com>"
      |  },
      |  "State" : "running",
      |  "Status" : "Up 6 seconds",
      |  "HostConfig" : {
      |    "NetworkMode" : "default"
      |  },
      |  "NetworkSettings" : {
      |    "Networks" : {
      |      "bridge" : {
      |        "IPAMConfig" : null,
      |        "Links" : null,
      |        "Aliases" : null,
      |        "NetworkID" : "8e692c7e924e9ae7351992f2e427c586d5a853ced52e8a50da412fd6399dd5d2",
      |        "EndpointID" : "0b4cc8afb7a3c17bc93fda25c5349f5a84af1346cb1c2a1449a89733c29bda8a",
      |        "Gateway" : "172.17.0.1",
      |        "IPAddress" : "172.17.0.2",
      |        "IPPrefixLen" : 16,
      |        "IPv6Gateway" : "",
      |        "GlobalIPv6Address" : "",
      |        "GlobalIPv6PrefixLen" : 0,
      |        "MacAddress" : "02:42:ac:11:00:02",
      |        "DriverOpts" : null
      |      }
      |    }
      |  },
      |  "Mounts" : [
      |  ]
      |}""".stripMargin)

    containerSummary should be(
      ContainerSummary(
        id = Container.Id("12a6a4da97dd4c43db609ecdd22ad6dffc95cf2fe9a8a9fe2b84038eba093053"),
        names = List("/sad_wescoff"),
        imageName = "jupyter/base-notebook",
        imageId = Image.Id("sha256:0fe41535dcac2fb8bee08dbb26895aa645ba38109c162b390e5bb45948f8ac35"),
        command = "tini -g -- start-notebook.sh",
        createdAt = ZonedDateTime.parse("2019-03-24T15:45:54Z"),
        ports = List(
          PortBinding(Some("0.0.0.0"), 8888, Some(8888), PortBinding.Type.TCP)
        ),
        sizeRw = None,
        sizeRootFs = None,
        labels = Map(
          "maintainer" -> "Jupyter Project <jupyter@googlegroups.com>"
        ),
        state = Container.Status.Running,
        status = "Up 6 seconds",
        networkMode = HostConfig.NetworkMode.Default,
        networks = Map(
          "bridge" -> Endpoint.Settings(
            ipamConfig = None,
            links = List.empty,
            aliases = List.empty,
            networkId = Network.Id("8e692c7e924e9ae7351992f2e427c586d5a853ced52e8a50da412fd6399dd5d2"),
            endpointId = Endpoint.Id("0b4cc8afb7a3c17bc93fda25c5349f5a84af1346cb1c2a1449a89733c29bda8a"),
            gateway = "172.17.0.1",
            ipAddress = "172.17.0.2",
            ipPrefixLen = 16,
            ipv6Gateway = "",
            globalIpv6Address = "",
            globalIpv6PrefixLen = 0,
            macAddress = "02:42:ac:11:00:02",
            driverOpts = Map.empty
          )
        ),
        mounts = List.empty
      )
    )
  }

  private def decodeContainerSummary(str: String): ContainerSummary = decode(str, ContainerSummary.decoder)

}
