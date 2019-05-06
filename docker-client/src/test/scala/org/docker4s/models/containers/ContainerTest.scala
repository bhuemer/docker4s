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

class ContainerTest extends ModelsSpec {

  "Decoding JSON into a container" should "work" in {
    val container = decodeContainer(
      """{
      |    "Id": "3a73d078dfe897882443cb09785b9ac896a989cc4b8c721c61e08596ae642b4a",
      |    "Created": "2019-05-06T15:06:10.3206247Z",
      |    "Path": "tini",
      |    "Args": [
      |        "-g",
      |        "--",
      |        "start-notebook.sh"
      |    ],
      |    "State": {
      |        "Status": "running",
      |        "Running": true,
      |        "Paused": false,
      |        "Restarting": false,
      |        "OOMKilled": false,
      |        "Dead": false,
      |        "Pid": 3931,
      |        "ExitCode": 0,
      |        "Error": "",
      |        "StartedAt": "2019-05-06T15:06:10.8446029Z",
      |        "FinishedAt": "0001-01-01T00:00:00Z"
      |    },
      |    "Image": "sha256:c277215e785e96520dd2e7717b692c128e782f187edcb1c32feaf209b533a530",
      |    "ResolvConfPath": "/var/lib/docker/containers/3a73d078dfe897882443cb09785b9ac896a989cc4b8c721c61e08596ae642b4a/resolv.conf",
      |    "HostnamePath": "/var/lib/docker/containers/3a73d078dfe897882443cb09785b9ac896a989cc4b8c721c61e08596ae642b4a/hostname",
      |    "HostsPath": "/var/lib/docker/containers/3a73d078dfe897882443cb09785b9ac896a989cc4b8c721c61e08596ae642b4a/hosts",
      |    "LogPath": "/var/lib/docker/containers/3a73d078dfe897882443cb09785b9ac896a989cc4b8c721c61e08596ae642b4a/3a73d078dfe897882443cb09785b9ac896a989cc4b8c721c61e08596ae642b4a-json.log",
      |    "Name": "/dazzling_wiles",
      |    "RestartCount": 0,
      |    "Driver": "overlay2",
      |    "Platform": "linux",
      |    "MountLabel": "",
      |    "ProcessLabel": "",
      |    "AppArmorProfile": "",
      |    "ExecIDs": null,
      |    "HostConfig": {
      |        "Binds": null,
      |        "ContainerIDFile": "",
      |        "LogConfig": {
      |            "Type": "json-file",
      |            "Config": {}
      |        },
      |        "NetworkMode": "default",
      |        "PortBindings": {},
      |        "RestartPolicy": {
      |            "Name": "no",
      |            "MaximumRetryCount": 0
      |        },
      |        "AutoRemove": false,
      |        "VolumeDriver": "",
      |        "VolumesFrom": null,
      |        "CapAdd": null,
      |        "CapDrop": null,
      |        "Dns": [],
      |        "DnsOptions": [],
      |        "DnsSearch": [],
      |        "ExtraHosts": null,
      |        "GroupAdd": null,
      |        "IpcMode": "shareable",
      |        "Cgroup": "",
      |        "Links": null,
      |        "OomScoreAdj": 0,
      |        "PidMode": "",
      |        "Privileged": false,
      |        "PublishAllPorts": false,
      |        "ReadonlyRootfs": false,
      |        "SecurityOpt": null,
      |        "UTSMode": "",
      |        "UsernsMode": "",
      |        "ShmSize": 67108864,
      |        "Runtime": "runc",
      |        "ConsoleSize": [
      |            0,
      |            0
      |        ],
      |        "Isolation": "",
      |        "CpuShares": 0,
      |        "Memory": 0,
      |        "NanoCpus": 0,
      |        "CgroupParent": "",
      |        "BlkioWeight": 0,
      |        "BlkioWeightDevice": [],
      |        "BlkioDeviceReadBps": null,
      |        "BlkioDeviceWriteBps": null,
      |        "BlkioDeviceReadIOps": null,
      |        "BlkioDeviceWriteIOps": null,
      |        "CpuPeriod": 0,
      |        "CpuQuota": 0,
      |        "CpuRealtimePeriod": 0,
      |        "CpuRealtimeRuntime": 0,
      |        "CpusetCpus": "",
      |        "CpusetMems": "",
      |        "Devices": [],
      |        "DeviceCgroupRules": null,
      |        "DiskQuota": 0,
      |        "KernelMemory": 0,
      |        "MemoryReservation": 0,
      |        "MemorySwap": 0,
      |        "MemorySwappiness": null,
      |        "OomKillDisable": false,
      |        "PidsLimit": 0,
      |        "Ulimits": null,
      |        "CpuCount": 0,
      |        "CpuPercent": 0,
      |        "IOMaximumIOps": 0,
      |        "IOMaximumBandwidth": 0,
      |        "MaskedPaths": [
      |            "/proc/asound",
      |            "/proc/acpi",
      |            "/proc/kcore",
      |            "/proc/keys",
      |            "/proc/latency_stats",
      |            "/proc/timer_list",
      |            "/proc/timer_stats",
      |            "/proc/sched_debug",
      |            "/proc/scsi",
      |            "/sys/firmware"
      |        ],
      |        "ReadonlyPaths": [
      |            "/proc/bus",
      |            "/proc/fs",
      |            "/proc/irq",
      |            "/proc/sys",
      |            "/proc/sysrq-trigger"
      |        ]
      |    },
      |    "GraphDriver": {
      |        "Data": {
      |            "LowerDir": "/var/lib/docker/overlay2/835d104a817adb408d6244c8fc860c7152a78e1e41ea4db5b666f4575004a5d1-init/diff:/var/lib/docker/overlay2/a0c07c7e0e2e1a7ffc00520b9a9c0b475250f430f1400e12dfb044df321cb1af/diff:/var/lib/docker/overlay2/dcb737829da445e8367b89f786bfb407a63f36bb34deba4ab4ed65e1f08515d6/diff:/var/lib/docker/overlay2/6177047047ef429ccf2ab95e68c7dcb584af31bae8e0d0c4dfcc557c58a5031d/diff:/var/lib/docker/overlay2/f30c116318756929bb9ea3eabe4177452e216e812c3232d97b47181ce9510a8b/diff:/var/lib/docker/overlay2/b13741d5bb9b4054917a609a2853b9eb3a3de60fd62c96fdaff876cb49d0e96b/diff:/var/lib/docker/overlay2/2eb90336f3cc1110594a535380dc33b3d6d3ab5ff81eeb9026884b2d149c38b3/diff:/var/lib/docker/overlay2/8120f7386f42c8a2f1b775b9cf798380baf2ad305824e9172ef1e0e7837211e4/diff:/var/lib/docker/overlay2/38c09f9b6479b504559bf9508c575863f7b52816a452ab122ed5fc188e76a9d5/diff:/var/lib/docker/overlay2/eb32cd2a6cc759c44fd9a6d5aa04702d12f91e778254bc58954ef59a80733c0e/diff:/var/lib/docker/overlay2/761ee5ea71ca54a384f3726ffcad2dde697b6e0cb661f7d8184e9df6d04f6f7a/diff:/var/lib/docker/overlay2/19220d7279aad0d85a4332d119ea1bd6421f39fee49fabccddffe63825c9c595/diff:/var/lib/docker/overlay2/a5d4bf6998c06c9c9dbce0e0e4749c6cbddbff258909f06fb43bc861794c7d21/diff:/var/lib/docker/overlay2/97d836c8b39a53788a751103e56f53275ed362504bba744094985e78c91e997f/diff:/var/lib/docker/overlay2/fa171fbce1bef0a4445d8388b3945107b6d61b806a5ffee112198029bfadbb7d/diff:/var/lib/docker/overlay2/0b670b55cf41195a5b746f492c5522e68386be654978d2e15af9ff7b05d3b2b5/diff:/var/lib/docker/overlay2/c43f7794ee5ff11f016b0e511e85207971c424aa8b1fb96734ab02444ba77ea4/diff:/var/lib/docker/overlay2/a1205e27bac140f4d79de13def2ab51e59235f5152589029e297e913290acc6d/diff:/var/lib/docker/overlay2/ec0b1070023a8a91f3f3b71243e36b528840ef9863860f4f4957b7b3db9fdf7e/diff:/var/lib/docker/overlay2/ec412bf231360acf18e5103176d3d3d5b82fee4e32f6a2c4a28445cdd0dcd5b6/diff:/var/lib/docker/overlay2/97366efdf955709ba6412d48d6bf4ccdd7d5edabdbc5aa698eda4cb7e959fcf9/diff:/var/lib/docker/overlay2/8c9266fdc4828e485e935d76c9a23ea370657cb613418e1adf68018c5c148593/diff",
      |            "MergedDir": "/var/lib/docker/overlay2/835d104a817adb408d6244c8fc860c7152a78e1e41ea4db5b666f4575004a5d1/merged",
      |            "UpperDir": "/var/lib/docker/overlay2/835d104a817adb408d6244c8fc860c7152a78e1e41ea4db5b666f4575004a5d1/diff",
      |            "WorkDir": "/var/lib/docker/overlay2/835d104a817adb408d6244c8fc860c7152a78e1e41ea4db5b666f4575004a5d1/work"
      |        },
      |        "Name": "overlay2"
      |    },
      |    "Mounts": [],
      |    "Config": {
      |        "Hostname": "3a73d078dfe8",
      |        "Domainname": "",
      |        "User": "1000",
      |        "AttachStdin": false,
      |        "AttachStdout": true,
      |        "AttachStderr": true,
      |        "ExposedPorts": {
      |            "8888/tcp": {}
      |        },
      |        "Tty": false,
      |        "OpenStdin": false,
      |        "StdinOnce": false,
      |        "Env": [
      |            "PATH=/opt/conda/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
      |            "DEBIAN_FRONTEND=noninteractive",
      |            "CONDA_DIR=/opt/conda",
      |            "SHELL=/bin/bash",
      |            "NB_USER=jovyan",
      |            "NB_UID=1000",
      |            "NB_GID=100",
      |            "LC_ALL=en_US.UTF-8",
      |            "LANG=en_US.UTF-8",
      |            "LANGUAGE=en_US.UTF-8",
      |            "HOME=/home/jovyan",
      |            "MINICONDA_VERSION=4.5.12",
      |            "CONDA_VERSION=4.6.7"
      |        ],
      |        "Cmd": [
      |            "start-notebook.sh"
      |        ],
      |        "ArgsEscaped": true,
      |        "Image": "jupyterhub/singleuser:latest",
      |        "Volumes": null,
      |        "WorkingDir": "/home/jovyan",
      |        "Entrypoint": [
      |            "tini",
      |            "-g",
      |            "--"
      |        ],
      |        "OnBuild": null,
      |        "Labels": {
      |            "maintainer": "Jupyter Project <jupyter@googlegroups.com>"
      |        }
      |    },
      |    "NetworkSettings": {
      |        "Bridge": "",
      |        "SandboxID": "0104b31b0ec019a39de9efdfc6f71b2a876a1b441dba942e0fcafd70106f8a6a",
      |        "HairpinMode": false,
      |        "LinkLocalIPv6Address": "",
      |        "LinkLocalIPv6PrefixLen": 0,
      |        "Ports": {
      |            "8888/tcp": null
      |        },
      |        "SandboxKey": "/var/run/docker/netns/0104b31b0ec0",
      |        "SecondaryIPAddresses": null,
      |        "SecondaryIPv6Addresses": null,
      |        "EndpointID": "e8cba948f78adfe193c19ee1d9e7ec492d01c722fff5bb8e4d6b9a5dc3561df8",
      |        "Gateway": "172.17.0.1",
      |        "GlobalIPv6Address": "",
      |        "GlobalIPv6PrefixLen": 0,
      |        "IPAddress": "172.17.0.5",
      |        "IPPrefixLen": 16,
      |        "IPv6Gateway": "",
      |        "MacAddress": "02:42:ac:11:00:05",
      |        "Networks": {
      |            "bridge": {
      |                "IPAMConfig": null,
      |                "Links": null,
      |                "Aliases": null,
      |                "NetworkID": "5f4231b1b650acb2a247518e274deba52ca0875689a19e04826b228cac655691",
      |                "EndpointID": "e8cba948f78adfe193c19ee1d9e7ec492d01c722fff5bb8e4d6b9a5dc3561df8",
      |                "Gateway": "172.17.0.1",
      |                "IPAddress": "172.17.0.5",
      |                "IPPrefixLen": 16,
      |                "IPv6Gateway": "",
      |                "GlobalIPv6Address": "",
      |                "GlobalIPv6PrefixLen": 0,
      |                "MacAddress": "02:42:ac:11:00:05",
      |                "DriverOpts": null
      |            }
      |        }
      |    }
      |}""")
    container.state should be(
      Container.State(
        status = Container.Status.Running,
        running = true,
        paused = false,
        restarting = false,
        oomKilled = false,
        dead = false,
        pid = 3931,
        exitCode = Some(0),
        error = None,
        startedAt = ZonedDateTime.parse("2019-05-06T15:06:10.8446029Z"),
        finishedAt = None,
        health = None
      ))
    println(container)
  }

  "Decoding JSON into a container" should "work for elasticsearch containers" in {
    val container = decodeContainer(
      """{
      |    "Id": "0112e5d6ac0445947b3de98303043f124ffc0bfa9939717fd56974418a449223",
      |    "Created": "2019-05-06T20:59:09.9140417Z",
      |    "Path": "/docker-entrypoint.sh",
      |    "Args": [
      |        "elasticsearch"
      |    ],
      |    "State": {
      |        "Status": "running",
      |        "Running": true,
      |        "Paused": false,
      |        "Restarting": false,
      |        "OOMKilled": false,
      |        "Dead": false,
      |        "Pid": 6578,
      |        "ExitCode": 0,
      |        "Error": "",
      |        "StartedAt": "2019-05-06T20:59:10.6833899Z",
      |        "FinishedAt": "0001-01-01T00:00:00Z",
      |        "Health": {
      |            "Status": "healthy",
      |            "FailingStreak": 0,
      |            "Log": [
      |                {
      |                    "Start": "2019-05-06T21:00:09.2987441Z",
      |                    "End": "2019-05-06T21:00:09.4147615Z",
      |                    "ExitCode": 0,
      |                    "Output": "{\"cluster_name\":\"elasticsearch\",\"status\":\"green\",\"timed_out\":false,\"number_of_nodes\":1,\"number_of_data_nodes\":1,\"active_primary_shards\":0,\"active_shards\":0,\"relocating_shards\":0,\"initializing_shards\":0,\"unassigned_shards\":0,\"delayed_unassigned_shards\":0,\"number_of_pending_tasks\":0,\"number_of_in_flight_fetch\":0,\"task_max_waiting_in_queue_millis\":0,\"active_shards_percent_as_number\":100.0}"
      |                },
      |                {
      |                    "Start": "2019-05-06T21:00:14.4615154Z",
      |                    "End": "2019-05-06T21:00:14.5751695Z",
      |                    "ExitCode": 0,
      |                    "Output": "{\"cluster_name\":\"elasticsearch\",\"status\":\"green\",\"timed_out\":false,\"number_of_nodes\":1,\"number_of_data_nodes\":1,\"active_primary_shards\":0,\"active_shards\":0,\"relocating_shards\":0,\"initializing_shards\":0,\"unassigned_shards\":0,\"delayed_unassigned_shards\":0,\"number_of_pending_tasks\":0,\"number_of_in_flight_fetch\":0,\"task_max_waiting_in_queue_millis\":0,\"active_shards_percent_as_number\":100.0}"
      |                }
      |            ]
      |        }
      |    },
      |    "Image": "sha256:d1ac13423d3cf7664e2b158049330c713f777473815b0ffe03d4103412505f0c",
      |    "ResolvConfPath": "/var/lib/docker/containers/0112e5d6ac0445947b3de98303043f124ffc0bfa9939717fd56974418a449223/resolv.conf",
      |    "HostnamePath": "/var/lib/docker/containers/0112e5d6ac0445947b3de98303043f124ffc0bfa9939717fd56974418a449223/hostname",
      |    "HostsPath": "/var/lib/docker/containers/0112e5d6ac0445947b3de98303043f124ffc0bfa9939717fd56974418a449223/hosts",
      |    "LogPath": "/var/lib/docker/containers/0112e5d6ac0445947b3de98303043f124ffc0bfa9939717fd56974418a449223/0112e5d6ac0445947b3de98303043f124ffc0bfa9939717fd56974418a449223-json.log",
      |    "Name": "/elasticsearch",
      |    "RestartCount": 0,
      |    "Driver": "overlay2",
      |    "Platform": "linux",
      |    "MountLabel": "",
      |    "ProcessLabel": "",
      |    "AppArmorProfile": "",
      |    "ExecIDs": null,
      |    "HostConfig": {
      |        "Binds": null,
      |        "ContainerIDFile": "",
      |        "LogConfig": {
      |            "Type": "json-file",
      |            "Config": {}
      |        },
      |        "NetworkMode": "default",
      |        "PortBindings": {},
      |        "RestartPolicy": {
      |            "Name": "no",
      |            "MaximumRetryCount": 0
      |        },
      |        "AutoRemove": true,
      |        "VolumeDriver": "",
      |        "VolumesFrom": null,
      |        "CapAdd": null,
      |        "CapDrop": null,
      |        "Dns": [],
      |        "DnsOptions": [],
      |        "DnsSearch": [],
      |        "ExtraHosts": null,
      |        "GroupAdd": null,
      |        "IpcMode": "shareable",
      |        "Cgroup": "",
      |        "Links": null,
      |        "OomScoreAdj": 0,
      |        "PidMode": "",
      |        "Privileged": false,
      |        "PublishAllPorts": false,
      |        "ReadonlyRootfs": false,
      |        "SecurityOpt": null,
      |        "UTSMode": "",
      |        "UsernsMode": "",
      |        "ShmSize": 67108864,
      |        "Runtime": "runc",
      |        "ConsoleSize": [
      |            0,
      |            0
      |        ],
      |        "Isolation": "",
      |        "CpuShares": 0,
      |        "Memory": 0,
      |        "NanoCpus": 0,
      |        "CgroupParent": "",
      |        "BlkioWeight": 0,
      |        "BlkioWeightDevice": [],
      |        "BlkioDeviceReadBps": null,
      |        "BlkioDeviceWriteBps": null,
      |        "BlkioDeviceReadIOps": null,
      |        "BlkioDeviceWriteIOps": null,
      |        "CpuPeriod": 0,
      |        "CpuQuota": 0,
      |        "CpuRealtimePeriod": 0,
      |        "CpuRealtimeRuntime": 0,
      |        "CpusetCpus": "",
      |        "CpusetMems": "",
      |        "Devices": [],
      |        "DeviceCgroupRules": null,
      |        "DiskQuota": 0,
      |        "KernelMemory": 0,
      |        "MemoryReservation": 0,
      |        "MemorySwap": 0,
      |        "MemorySwappiness": null,
      |        "OomKillDisable": false,
      |        "PidsLimit": 0,
      |        "Ulimits": null,
      |        "CpuCount": 0,
      |        "CpuPercent": 0,
      |        "IOMaximumIOps": 0,
      |        "IOMaximumBandwidth": 0,
      |        "MaskedPaths": [
      |            "/proc/asound",
      |            "/proc/acpi",
      |            "/proc/kcore",
      |            "/proc/keys",
      |            "/proc/latency_stats",
      |            "/proc/timer_list",
      |            "/proc/timer_stats",
      |            "/proc/sched_debug",
      |            "/proc/scsi",
      |            "/sys/firmware"
      |        ],
      |        "ReadonlyPaths": [
      |            "/proc/bus",
      |            "/proc/fs",
      |            "/proc/irq",
      |            "/proc/sys",
      |            "/proc/sysrq-trigger"
      |        ]
      |    },
      |    "GraphDriver": {
      |        "Data": {
      |            "LowerDir": "/var/lib/docker/overlay2/92f90b8a35259835c6f713d2f3e509260dbaacffa667d2f8f8ddee7be38d6d0a-init/diff:/var/lib/docker/overlay2/002ff3e56cb07f6ddd321d18f814fd3fa5bc8b03d0cec97304c56feb5c6ab9e2/diff:/var/lib/docker/overlay2/dd2817a755d20eb67056c8b55f2f94debf8ab4a82d4f7e5e5ec802a8ea07026f/diff:/var/lib/docker/overlay2/336948abcccdacadcea9e8bc5e186c3c83fdd405c891496c9916f8ec1106a23c/diff:/var/lib/docker/overlay2/018a944bb52770200bb76131083305a6555e626cc28c4dcc96d8163e01d07176/diff:/var/lib/docker/overlay2/ce6cc2d6ca7030e9e8f2238ba5786ba5d89ca13f6ae0a9f81adb18787c90b119/diff:/var/lib/docker/overlay2/66f6b9e26ecc2c4679e153916f4ded11274119bdf00b46003e93bde3965649c0/diff:/var/lib/docker/overlay2/1a1cb2a1a6d20b279f4f8e30ec7e41b73a3f74fa9d29fb7344ae8465afad2f03/diff:/var/lib/docker/overlay2/a7367f2921d22b83f702d679e746c9819696560ea1e410e9a42b66670b8b4071/diff:/var/lib/docker/overlay2/0a5b2e9befc15b7d70f67ddb925abda9dcaaab746d6bc68292746ae4ba67c4a9/diff:/var/lib/docker/overlay2/e7e221158a5dc116f20e3ab9f17b177ab0fac0fc7eb48964306282c61d4a3555/diff:/var/lib/docker/overlay2/7aeacc2270f37b2e5cbe4217cbf3acb55ba4ea9d96f3fc5c37a00381e2ce7d06/diff:/var/lib/docker/overlay2/4c76905e2aaf472cd7d27769bca69f98bc116856ce299076add14e41eec04bfa/diff:/var/lib/docker/overlay2/8905635a73be821ea30c376618f39411574542dbd026cb1ad4ff25f77aab82f1/diff:/var/lib/docker/overlay2/eaedf8d577aa3ff478b73b21b90c1ea902b542a12910a3e89e228bbcd69a2f99/diff:/var/lib/docker/overlay2/38421d09a0a1654059f6390e2c7931bd74336d8c40696a936cb09851d15262be/diff",
      |            "MergedDir": "/var/lib/docker/overlay2/92f90b8a35259835c6f713d2f3e509260dbaacffa667d2f8f8ddee7be38d6d0a/merged",
      |            "UpperDir": "/var/lib/docker/overlay2/92f90b8a35259835c6f713d2f3e509260dbaacffa667d2f8f8ddee7be38d6d0a/diff",
      |            "WorkDir": "/var/lib/docker/overlay2/92f90b8a35259835c6f713d2f3e509260dbaacffa667d2f8f8ddee7be38d6d0a/work"
      |        },
      |        "Name": "overlay2"
      |    },
      |    "Mounts": [
      |        {
      |            "Type": "volume",
      |            "Name": "ab51a4264f3228e7aff3d9877d1986ea9ebdcadcc990805bf331ffdd022f81df",
      |            "Source": "/var/lib/docker/volumes/ab51a4264f3228e7aff3d9877d1986ea9ebdcadcc990805bf331ffdd022f81df/_data",
      |            "Destination": "/usr/share/elasticsearch/data",
      |            "Driver": "local",
      |            "Mode": "",
      |            "RW": true,
      |            "Propagation": ""
      |        }
      |    ],
      |    "Config": {
      |        "Hostname": "0112e5d6ac04",
      |        "Domainname": "",
      |        "User": "",
      |        "AttachStdin": false,
      |        "AttachStdout": false,
      |        "AttachStderr": false,
      |        "ExposedPorts": {
      |            "9200/tcp": {},
      |            "9300/tcp": {}
      |        },
      |        "Tty": false,
      |        "OpenStdin": false,
      |        "StdinOnce": false,
      |        "Env": [
      |            "PATH=/usr/share/elasticsearch/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
      |            "LANG=C.UTF-8",
      |            "JAVA_HOME=/docker-java-home/jre",
      |            "JAVA_VERSION=8u141",
      |            "JAVA_DEBIAN_VERSION=8u141-b15-1~deb9u1",
      |            "CA_CERTIFICATES_JAVA_VERSION=20170531+nmu1",
      |            "GOSU_VERSION=1.10",
      |            "ELASTICSEARCH_VERSION=5.5.2",
      |            "ELASTICSEARCH_DEB_VERSION=5.5.2"
      |        ],
      |        "Cmd": [
      |            "elasticsearch"
      |        ],
      |        "Healthcheck": {
      |            "Test": [
      |                "CMD-SHELL",
      |                "curl --silent --fail localhost:9200/_cluster/health || exit 1"
      |            ],
      |            "Interval": 5000000000,
      |            "Timeout": 2000000000,
      |            "Retries": 12
      |        },
      |        "ArgsEscaped": true,
      |        "Image": "elasticsearch:5.5",
      |        "Volumes": {
      |            "/usr/share/elasticsearch/data": {}
      |        },
      |        "WorkingDir": "/usr/share/elasticsearch",
      |        "Entrypoint": [
      |            "/docker-entrypoint.sh"
      |        ],
      |        "OnBuild": null,
      |        "Labels": {}
      |    },
      |    "NetworkSettings": {
      |        "Bridge": "",
      |        "SandboxID": "44debc5cace8ded87426bb4d9c415996709f3e7ef022e6c7584587b8a4a28835",
      |        "HairpinMode": false,
      |        "LinkLocalIPv6Address": "",
      |        "LinkLocalIPv6PrefixLen": 0,
      |        "Ports": {
      |            "9200/tcp": null,
      |            "9300/tcp": null
      |        },
      |        "SandboxKey": "/var/run/docker/netns/44debc5cace8",
      |        "SecondaryIPAddresses": null,
      |        "SecondaryIPv6Addresses": null,
      |        "EndpointID": "a703204a506023b540ebc8d4684a2cdd0420dbe6f75ff9d08a142a46c327e708",
      |        "Gateway": "172.17.0.1",
      |        "GlobalIPv6Address": "",
      |        "GlobalIPv6PrefixLen": 0,
      |        "IPAddress": "172.17.0.2",
      |        "IPPrefixLen": 16,
      |        "IPv6Gateway": "",
      |        "MacAddress": "02:42:ac:11:00:02",
      |        "Networks": {
      |            "bridge": {
      |                "IPAMConfig": null,
      |                "Links": null,
      |                "Aliases": null,
      |                "NetworkID": "5f4231b1b650acb2a247518e274deba52ca0875689a19e04826b228cac655691",
      |                "EndpointID": "a703204a506023b540ebc8d4684a2cdd0420dbe6f75ff9d08a142a46c327e708",
      |                "Gateway": "172.17.0.1",
      |                "IPAddress": "172.17.0.2",
      |                "IPPrefixLen": 16,
      |                "IPv6Gateway": "",
      |                "GlobalIPv6Address": "",
      |                "GlobalIPv6PrefixLen": 0,
      |                "MacAddress": "02:42:ac:11:00:02",
      |                "DriverOpts": null
      |            }
      |        }
      |    }
      |}""".stripMargin)
    container.state should be(
      Container.State(
        status = Container.Status.Running,
        running = true,
        paused = false,
        restarting = false,
        oomKilled = false,
        dead = false,
        pid = 6578,
        exitCode = Some(0),
        error = None,
        startedAt = ZonedDateTime.parse("2019-05-06T20:59:10.683389900Z"),
        finishedAt = None,
        health = Some(Container.State.HealthState(
          status = Container.State.HealthStatus.Healthy,
          failingStreak = 0,
          logs = List(
            Container.State.HealthState.Log(
              start = ZonedDateTime.parse("2019-05-06T21:00:09.298744100Z"),
              end = ZonedDateTime.parse("2019-05-06T21:00:09.414761500Z"),
              exitCode = 0,
              output = Some(
                """{"cluster_name":"elasticsearch","status":"green","timed_out":false,"number_of_nodes":1,"number_of_data_nodes":1,"active_primary_shards":0,"active_shards":0,"relocating_shards":0,"initializing_shards":0,"unassigned_shards":0,"delayed_unassigned_shards":0,"number_of_pending_tasks":0,"number_of_in_flight_fetch":0,"task_max_waiting_in_queue_millis":0,"active_shards_percent_as_number":100.0}""")
            ),
            Container.State.HealthState.Log(
              start = ZonedDateTime.parse("2019-05-06T21:00:14.461515400Z"),
              end = ZonedDateTime.parse("2019-05-06T21:00:14.575169500Z"),
              exitCode = 0,
              output = Some(
                """{"cluster_name":"elasticsearch","status":"green","timed_out":false,"number_of_nodes":1,"number_of_data_nodes":1,"active_primary_shards":0,"active_shards":0,"relocating_shards":0,"initializing_shards":0,"unassigned_shards":0,"delayed_unassigned_shards":0,"number_of_pending_tasks":0,"number_of_in_flight_fetch":0,"task_max_waiting_in_queue_millis":0,"active_shards_percent_as_number":100.0}""")
            )
          )
        ))
      ))
    println(container.state)
    println(container)
  }

  private def decodeContainer(str: String): Container = decode(str, Container.decoder)

}
