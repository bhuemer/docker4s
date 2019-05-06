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

import io.circe.Decoder
import org.docker4s.models.ModelsSpec

class HostConfigTest extends ModelsSpec {
//
//  "" should "" in {
//    decode(
//      """{
//             |    "Binds":null,
//             |    "ContainerIDFile":"",
//             |    "LogConfig":{
//             |      "Type":"json-file",
//             |      "Config":{
//             |
//             |      }
//             |    },
//             |    "NetworkMode":"default",
//             |    "PortBindings":{
//             |      "8888/tcp":[
//             |        {
//             |          "HostIp":"",
//             |          "HostPort":""
//             |        }
//             |      ]
//             |    },
//             |    "RestartPolicy":{
//             |      "Name":"",
//             |      "MaximumRetryCount":0
//             |    },
//             |    "AutoRemove":false,
//             |    "VolumeDriver":"",
//             |    "VolumesFrom":null,
//             |    "CapAdd":null,
//             |    "CapDrop":null,
//             |    "Dns":null,
//             |    "DnsOptions":null,
//             |    "DnsSearch":null,
//             |    "ExtraHosts":null,
//             |    "GroupAdd":null,
//             |    "IpcMode":"shareable",
//             |    "Cgroup":"",
//             |    "Links":null,
//             |    "OomScoreAdj":0,
//             |    "PidMode":"",
//             |    "Privileged":false,
//             |    "PublishAllPorts":false,
//             |    "ReadonlyRootfs":false,
//             |    "SecurityOpt":null,
//             |    "UTSMode":"",
//             |    "UsernsMode":"",
//             |    "ShmSize":67108864,
//             |    "Runtime":"runc",
//             |    "ConsoleSize":[
//             |      0,
//             |      0
//             |    ],
//             |    "Isolation":"",
//             |    "CpuShares":0,
//             |    "Memory":0,
//             |    "NanoCpus":0,
//             |    "CgroupParent":"",
//             |    "BlkioWeight":0,
//             |    "BlkioWeightDevice":null,
//             |    "BlkioDeviceReadBps":null,
//             |    "BlkioDeviceWriteBps":null,
//             |    "BlkioDeviceReadIOps":null,
//             |    "BlkioDeviceWriteIOps":null,
//             |    "CpuPeriod":0,
//             |    "CpuQuota":0,
//             |    "CpuRealtimePeriod":0,
//             |    "CpuRealtimeRuntime":0,
//             |    "CpusetCpus":"",
//             |    "CpusetMems":"",
//             |    "Devices":null,
//             |    "DeviceCgroupRules":null,
//             |    "DiskQuota":0,
//             |    "KernelMemory":0,
//             |    "MemoryReservation":0,
//             |    "MemorySwap":0,
//             |    "MemorySwappiness":null,
//             |    "OomKillDisable":false,
//             |    "PidsLimit":0,
//             |    "Ulimits":null,
//             |    "CpuCount":0,
//             |    "CpuPercent":0,
//             |    "IOMaximumIOps":0,
//             |    "IOMaximumBandwidth":0,
//             |    "MaskedPaths":[
//             |      "/proc/asound",
//             |      "/proc/acpi",
//             |      "/proc/kcore",
//             |      "/proc/keys",
//             |      "/proc/latency_stats",
//             |      "/proc/timer_list",
//             |      "/proc/timer_stats",
//             |      "/proc/sched_debug",
//             |      "/proc/scsi",
//             |      "/sys/firmware"
//             |    ],
//             |    "ReadonlyPaths":[
//             |      "/proc/bus",
//             |      "/proc/fs",
//             |      "/proc/irq",
//             |      "/proc/sys",
//             |      "/proc/sysrq-trigger"
//             |    ]
//             |  }""",
//      Decoder.decodeString
//    )
//  }

}
