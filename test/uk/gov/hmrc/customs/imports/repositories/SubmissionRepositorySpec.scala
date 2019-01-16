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

package uk.gov.hmrc.customs.imports.repositories

import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.customs.imports.base.{CustomsImportsBaseSpec, ImportsTestData}

import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionRepositorySpec extends CustomsImportsBaseSpec with BeforeAndAfterEach with ImportsTestData {

  override protected def afterEach(): Unit = {
    super.afterEach()
    repo.removeAll()
  }

  override lazy val app: Application = GuiceApplicationBuilder().build()
  val repo: SubmissionRepository = component[SubmissionRepository]

  "SubmissionRepository" should {
    "save declaration with EORI and timestamp" in {
      repo.save(submission).futureValue must be(true)

      // we can now display a list of all the declarations belonging to the current user, searching by EORI
      val foundSubmission = repo.findByEori(eori).futureValue
      foundSubmission.length must be(1)
      foundSubmission.head.eori must be(eori)
      foundSubmission.head.conversationId must be(conversationId)
      foundSubmission.head.mrn must be(Some(mrn))

      // a timestamp has been generated representing "creation time" of case class instance
      foundSubmission.head.submittedTimestamp must (be >= before).and(be <= System.currentTimeMillis())

      // we can also retrieve the submission individually by conversation ID
      val submission1 = repo.getByConversationId(conversationId).futureValue.value
      submission1.eori must be(eori)
      submission1.conversationId must be(conversationId)
      submission1.mrn must be(Some(mrn))

      // or we can retrieve it by eori and MRN
      val submission2 = repo.getByEoriAndMrn(eori, mrn).futureValue.value
      submission2.eori must be(eori)
      submission2.conversationId must be(conversationId)
      submission2.mrn must be(Some(mrn))

      // update status test
      val submissionToUpdate = repo.getByConversationId(conversationId).futureValue.value

      val updatedSubmission = submissionToUpdate.copy(status = Some("Accepted"))

      repo.updateSubmission(updatedSubmission).futureValue must be(true)

      val newSubmission = repo.getByConversationId(conversationId).futureValue.value

      newSubmission must be(updatedSubmission)
    }
  }
}
