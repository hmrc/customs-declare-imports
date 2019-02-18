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

package unit.services

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.imports.repositories.{SubmissionActionRepository, SubmissionNotificationRepository, SubmissionRepository}
import uk.gov.hmrc.customs.imports.services.TestOnlyImportService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import unit.base.ImportsTestData

import scala.concurrent.Future

class TestOnlyImportServiceSpec extends MockitoSugar with UnitSpec with ScalaFutures with ImportsTestData {

  trait SetUp {
    val mockSubmissionRepo: SubmissionRepository = mock[SubmissionRepository]
    val mockSubmissionActionRepo: SubmissionActionRepository = mock[SubmissionActionRepository]
    val mockSubmissionNotificationRepo: SubmissionNotificationRepository = mock[SubmissionNotificationRepository]
    implicit val hc: HeaderCarrier = mock[HeaderCarrier]
    val testObj = new TestOnlyImportService(mockSubmissionRepo,
      mockSubmissionActionRepo,
      mockSubmissionNotificationRepo)
  }


  "TestOnlyImportService" should {
    "call submission, submissionAction and submissionNotification repository delete actions " in new SetUp() {

      when(mockSubmissionRepo.getByEoriAndLrn(declarantEoriValue, declarantLrnValue)).thenReturn(Future.successful(Some(submission)))
      when(mockSubmissionActionRepo.getBySubmissionId(submission.id)).thenReturn(Future.successful(Seq(submissionAction)))
      when(mockSubmissionRepo.deleteById(any())).thenReturn(Future.successful(true))

      val result = await(testObj.deleteByEoriAndLrn(declarantEoriValue, declarantLrnValue))

      result shouldBe true

      verify(mockSubmissionRepo, times(1)).getByEoriAndLrn(declarantEoriValue, declarantLrnValue)
      verify(mockSubmissionRepo, times(1)).deleteById(submission.id)
      verify(mockSubmissionActionRepo, times(1)).getBySubmissionId(submission.id)
      verify(mockSubmissionActionRepo, times(1)).deleteBySubmissionId(submission.id)
      verify(mockSubmissionNotificationRepo, times(1)).deleteByConversationId(submissionAction.conversationId)
    }
  }

}
