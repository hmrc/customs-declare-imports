/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package integration.repositories

import java.util.UUID

import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsString
import uk.gov.hmrc.customs.imports.models.SubmissionAction
import uk.gov.hmrc.customs.imports.repositories.SubmissionActionRepository
import unit.base.{CustomsImportsBaseSpec, ImportsTestData}

import scala.concurrent.Await
import scala.concurrent.duration._

class SubmissionActionRepositorySpec extends CustomsImportsBaseSpec with BeforeAndAfterEach with ImportsTestData {

  override protected def afterEach(): Unit = {
    super.afterEach()
    Await.result(repo.removeAll(), 1 second)
  }

  override lazy val app: Application = GuiceApplicationBuilder().build()

  val repo: SubmissionActionRepository = component[SubmissionActionRepository]

  "SubmissionActionRepository" should {
    "save submissionAction then return true" in {
      val actionToPersist = submissionAction
      await(repo.insert(actionToPersist)).ok shouldBe true

      val foundAction: Seq[SubmissionAction] = await(repo.find("conversationId" -> JsString(conversationId)))

      foundAction.size shouldBe 1
      foundAction(0).submissionId shouldBe actionToPersist.submissionId
      foundAction(0).conversationId shouldBe conversationId
      foundAction(0).dateTimeSent should (be >= before).and(be <= System.currentTimeMillis())
    }

    "find by conversationId should return the relevant submissionAction" in{
      val actionToPersist = submissionAction
      await(repo.insert(actionToPersist)).ok shouldBe true

       val  foundAction : SubmissionAction = await(repo.findByConversationId(conversationId)).get

      foundAction.submissionId shouldBe actionToPersist.submissionId
      foundAction.conversationId shouldBe conversationId
      foundAction.dateTimeSent should (be >= before).and(be <= System.currentTimeMillis())
    }

    "find by submissionId should return the relevant submissionAction" in{
      val actionToPersist = submissionAction
      await(repo.insert(actionToPersist)).ok shouldBe true

      val  foundActions : Seq[SubmissionAction] = await(repo.getBySubmissionId(actionToPersist.submissionId))

      foundActions.size shouldBe 1
      foundActions.head.submissionId shouldBe actionToPersist.submissionId
      foundActions.head.conversationId shouldBe conversationId
      foundActions.head.dateTimeSent should (be >= before).and(be <= System.currentTimeMillis())
    }

    "find by submissionId should return 2 actions when 2 are persisted" in{
      val actionToPersist = submissionAction
      await(repo.insert(actionToPersist)).ok shouldBe true
      await(repo.insert(cancelationAction)).ok shouldBe true

      val  foundActions : Seq[SubmissionAction] = await(repo.getBySubmissionId(actionToPersist.submissionId))

      foundActions.size shouldBe 2

    }

    "remove by submissionId should remove the relevant submissionAction" in{
      val actionToPersist = submissionAction
      await(repo.insert(actionToPersist)).ok shouldBe true

      val  foundAction : SubmissionAction = await(repo.findByConversationId(conversationId)).get

      foundAction.submissionId shouldBe actionToPersist.submissionId
      foundAction.conversationId shouldBe conversationId
      foundAction.dateTimeSent should (be >= before).and(be <= System.currentTimeMillis())

      await(repo.deleteBySubmissionId(actionToPersist.submissionId))

      val  foundAfterDeleteAction : Option[SubmissionAction] = await(repo.findByConversationId(conversationId))

      foundAfterDeleteAction shouldBe None
    }

    "find by conversationId should return None when there is no match" in{
      val actionToPersist = submissionAction
      await(repo.insert(actionToPersist)).ok shouldBe true

      val  foundAction : Option[SubmissionAction] = await(repo.findByConversationId(UUID.randomUUID().toString))

      foundAction shouldBe None
    }
  }

}
