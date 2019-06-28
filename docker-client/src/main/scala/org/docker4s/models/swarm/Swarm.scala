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
package org.docker4s.models.swarm

import java.time.ZonedDateTime

/**
  * {
  *   "ID":"u8phfv36zkitb014dpzajoqx1",
  *   "Version":{
  *     "Index":10
  *   },
  *   "CreatedAt":"2019-06-28T15:36:32.4826562Z",
  *   "UpdatedAt":"2019-06-28T15:36:33.0065493Z",
  *   "Spec":{
  *     "Name":"default",
  *     "Labels":{},
  *     "Orchestration":{"TaskHistoryRetentionLimit":5},
  *     "Raft":{"SnapshotInterval":10000,"KeepOldSnapshots":0,"LogEntriesForSlowFollowers":500,"ElectionTick":10,"HeartbeatTick":1},
  *     "Dispatcher":{"HeartbeatPeriod":5000000000},
  *     "CAConfig":{"NodeCertExpiry":7776000000000000},
  *     "TaskDefaults":{},
  *     "EncryptionConfig":{"AutoLockManagers":false}
  *   },
  *   "TLSInfo":{
  *     "TrustRoot":"-----BEGIN CERTIFICATE-----\nMIIBajCCARCgAwIBAgIUC8ODj6wjed3vsxobmr5RUlWI5JQwCgYIKoZIzj0EAwIw\nEzERMA8GA1UEAxMIc3dhcm0tY2EwHhcNMTkwNjI4MTUzMjAwWhcNMzkwNjIzMTUz\nMjAwWjATMREwDwYDVQQDEwhzd2FybS1jYTBZMBMGByqGSM49AgEGCCqGSM49AwEH\nA0IABNQ0s1vG6xAf/SkM7BnABMxDIUk3PlKFHzwc35N+s/svCwQPD0L56fm1P40W\ndMxHkdTzXLChDcSwabvOd7vN4Y+jQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNVHRMB\nAf8EBTADAQH/MB0GA1UdDgQWBBTb0LHKkHM9gHhqXjv2rWC+SxdHnzAKBggqhkjO\nPQQDAgNIADBFAiAVVu9W3XJzMJMV9+6gF8uHLUBoghqj9ZF69USQATlNkAIhAIH5\nKT+dbXA6oI34d9+uoJqdeXsJCzn5h3xTF755l0KD\n-----END CERTIFICATE-----\n",
  *     "CertIssuerSubject":"MBMxETAPBgNVBAMTCHN3YXJtLWNh",
  *     "CertIssuerPublicKey":"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1DSzW8brEB/9KQzsGcAEzEMhSTc+UoUfPBzfk36z+y8LBA8PQvnp+bU/jRZ0zEeR1PNcsKENxLBpu853u83hjw=="
  *   },
  *   "RootRotationInProgress":false,
  *   "DefaultAddrPool":["10.0.0.0/8"],
  *   "SubnetSize":24,
  *   "JoinTokens":{
  *     "Worker":"SWMTKN-1-3sqnbrp9psllu2vl2z249rbswrn9caotewbik9scouqk67xerw-9fjg0a57lleyeo6nf9luqbn3f",
  *     "Manager":"SWMTKN-1-3sqnbrp9psllu2vl2z249rbswrn9caotewbik9scouqk67xerw-e7rwbll85fl0r7r4pfrnmuh0w"
  *   }
  * }
  */
case class Swarm(id: Swarm.Id, createdAt: ZonedDateTime, updatedAt: ZonedDateTime) {}

object Swarm {

  /** Typed identifier for Docker swarms. */
  case class Id(id: String)

}
