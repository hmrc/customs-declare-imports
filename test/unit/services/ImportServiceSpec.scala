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
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.customs.imports.connectors.{CustomsDeclarationsConnector, CustomsDeclarationsResponse}
import uk.gov.hmrc.customs.imports.models.{Submission, SubmissionAction, SubmissionNotification}
import uk.gov.hmrc.customs.imports.repositories.{SubmissionActionRepository, SubmissionNotificationRepository, SubmissionRepository}
import uk.gov.hmrc.customs.imports.services.ImportService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import unit.base.ImportsTestData

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class ImportServiceSpec extends MockitoSugar with UnitSpec with ScalaFutures with ImportsTestData {

  trait SetUp {
    val mockSubmissionRepo: SubmissionRepository = mock[SubmissionRepository]
    val mockSubmissionActionRepo: SubmissionActionRepository = mock[SubmissionActionRepository]
    val mockSubmissionNotificationRepo: SubmissionNotificationRepository = mock[SubmissionNotificationRepository](withSettings().verboseLogging())
    val mockCustomsDeclarationsConnector: CustomsDeclarationsConnector = mock[CustomsDeclarationsConnector]
    implicit val hc: HeaderCarrier = mock[HeaderCarrier]
    val testObj = new ImportService(mockSubmissionRepo,
                                    mockSubmissionActionRepo,
                                    mockSubmissionNotificationRepo,
                                    mockCustomsDeclarationsConnector)
  }

  "ImportService" should {
    "save submission Data in repository" in new SetUp() {
      val mockWriteResult: WriteResult = mock[WriteResult]
      val xmlVal: Elem = <iamxml></iamxml>

      when(mockCustomsDeclarationsConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext]))
        .thenReturn(Future.successful(CustomsDeclarationsResponse(randomConversationId)))
      when(mockSubmissionRepo.insert(any[Submission])(any[ExecutionContext])).thenReturn(Future.successful(mockWriteResult))
      when(mockSubmissionActionRepo.insert(any[SubmissionAction])(any[ExecutionContext])).thenReturn(Future.successful(mockWriteResult))
      when(mockWriteResult.ok).thenReturn(true)

      val result: Boolean = await(testObj.handleDeclarationSubmit(declarantEoriValue, declarantLrnValue,  xmlVal))

      verify(mockCustomsDeclarationsConnector, times(1)).submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext])
      verify(mockSubmissionRepo, times(1)).insert(any[Submission])(any[ExecutionContext])
      verify(mockSubmissionActionRepo, times(1)).insert(any[SubmissionAction])(any[ExecutionContext])
      result should be(true)
    }

    "save notification Data in repository when submissionAction and Submission exist" in new SetUp() {
      val mockWriteResult: WriteResult = mock[WriteResult]
      when(mockSubmissionRepo.findById(any[BSONObjectID],any[ReadPreference])(any[ExecutionContext])).thenReturn(Future.successful(Some(submissionNoMrn)))
      when(mockSubmissionActionRepo.findByConversationId(any[String])).thenReturn(Future.successful(Some(submissionAction)))
      when(mockSubmissionRepo.updateSubmission(any[Submission])).thenReturn(Future.successful(true))
      when(mockWriteResult.ok).thenReturn(true)
      when(mockSubmissionNotificationRepo.insert(any[SubmissionNotification])(any[ExecutionContext])).thenReturn(Future.successful(mockWriteResult))

      val result: Boolean = await(testObj.handleNotificationReceived(conversationId, exampleAcceptNotification))

      verify(mockSubmissionActionRepo, times(1)).findByConversationId(any[String])
      verify(mockSubmissionRepo, times(1)).findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext])
      verify(mockSubmissionNotificationRepo, times(1)).insert(any[SubmissionNotification])(any[ExecutionContext])
      result should be (true)

    }


  }
}
