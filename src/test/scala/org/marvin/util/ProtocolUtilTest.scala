/*
 * Copyright [2017] [B2W Digital]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.marvin.util

import org.marvin.model.{EngineActionMetadata, EngineMetadata}
import org.marvin.testutil.MetadataMock
import org.scalatest.{Matchers, WordSpec}

class ProtocolUtilTest extends WordSpec with Matchers {

  val protocolUtil = new ProtocolUtil()

  "generateProtocol" should {

    "generate a protocol with valid format" in {
      val protocol = protocolUtil.generateProtocol("test")
      protocol should startWith("test_")

      val protocolWithoutPrefix = protocol.replace("test_", "")
      val uuidRegex = """[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}""".r

      uuidRegex.findFirstIn(protocolWithoutPrefix) shouldNot be(Option.empty)
    }
  }

  "splitProtocol" should {

    "split a protocol message with one protocol only" in {
      val metadata = MetadataMock.simpleMockedMetadata()

      val protocolStr = protocolUtil.generateProtocol("acquisitor")
      val protocols = protocolUtil.splitProtocol(protocolStr, metadata)

      assert(protocols.contains("initial_dataset"))
      protocols.get("initial_dataset").mkString should be(protocolStr)
    }

    "split a protocol message with multiple protocols" in {
      val metadata = MetadataMock.simpleMockedMetadata()

      val aProtocol = protocolUtil.generateProtocol("acquisitor")
      val tProtocol = protocolUtil.generateProtocol("tpreparator")
      val protocols = protocolUtil.splitProtocol(aProtocol + "," + tProtocol, metadata)

      assert(protocols.contains("initial_dataset"))
      protocols.get("initial_dataset").mkString should be(aProtocol)

      assert(protocols.contains("dataset"))
      protocols.get("dataset").mkString should be(tProtocol)
    }

    "split a protocol message with pipeline protocol" in {

      val metadata =
        EngineMetadata(
          name = "test",
          actions = List[EngineActionMetadata](
            new EngineActionMetadata(name="predictor", actionType="online", port=777, host="localhost", artifactsToPersist=List(), artifactsToLoad=List("model")),
            new EngineActionMetadata(name="acquisitor", actionType="batch", port=778, host="localhost", artifactsToPersist=List("initial_dataset"), artifactsToLoad=List()),
            new EngineActionMetadata(name="tpreparator", actionType="batch", port=779, host="localhost", artifactsToPersist=List("dataset"), artifactsToLoad=List("initial_dataset"))
          ),
          artifactsRemotePath = "",
          artifactManagerType = "HDFS",
          s3BucketName = "marvin-artifact-bucket",
          batchActionTimeout = 100,
          engineType = "python",
          hdfsHost = "",
          healthCheckTimeout = 100,
          onlineActionTimeout = 100,
          pipelineActions = List("acquisitor", "tpreparator"),
          reloadStateTimeout = Some(500),
          reloadTimeout = 100,
          version = "1"
        )

      val pProtocol = protocolUtil.generateProtocol("pipeline")
      val protocols = protocolUtil.splitProtocol(pProtocol, metadata)

      protocols.keys.size should be(2)
      protocols.keys.foreach(key => protocols.get(key).mkString should be(pProtocol))
    }
  }
}
