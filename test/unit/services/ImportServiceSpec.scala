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

import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.customs.imports.connectors.{CustomsDeclarationsConnector, CustomsDeclarationsResponse}
import uk.gov.hmrc.customs.imports.models.{Declaration, Submission, SubmissionAction, SubmissionNotification}
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
    val mockSubmissionNotificationRepo: SubmissionNotificationRepository = mock[SubmissionNotificationRepository]
    //(withSettings().verboseLogging())
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
      val conversationId = randomConversationId

      when(mockCustomsDeclarationsConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext]))
        .thenReturn(Future.successful(CustomsDeclarationsResponse(conversationId)))
      when(mockSubmissionRepo.insert(any[Submission])(any[ExecutionContext])).thenReturn(Future.successful(mockWriteResult))
      when(mockSubmissionActionRepo.insert(any[SubmissionAction])(any[ExecutionContext])).thenReturn(Future.successful(mockWriteResult))
      when(mockWriteResult.ok).thenReturn(true)

      val result: Option[String] = await(testObj.handleDeclarationSubmit(declarantEoriValue, declarantLrnValue,  xmlVal))

      verify(mockCustomsDeclarationsConnector, times(1)).submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext])
      verify(mockSubmissionRepo, times(1)).insert(any[Submission])(any[ExecutionContext])
      verify(mockSubmissionActionRepo, times(1)).insert(any[SubmissionAction])(any[ExecutionContext])
      result shouldBe Some(conversationId)
    }

    "save not call submissionAction Repo if submission insert fails" in new SetUp() {
      val mockSubmissionWriteResult: WriteResult = mock[WriteResult]
      val xmlVal: Elem = <iamxml></iamxml>
      val conversationId = randomConversationId

      when(mockCustomsDeclarationsConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext]))
        .thenReturn(Future.successful(CustomsDeclarationsResponse(conversationId)))
      when(mockSubmissionRepo.insert(any[Submission])(any[ExecutionContext])).thenReturn(Future.successful(mockSubmissionWriteResult))
      when(mockSubmissionWriteResult.ok).thenReturn(false)

      val result: Option[String] = await(testObj.handleDeclarationSubmit(declarantEoriValue, declarantLrnValue,  xmlVal))

      verify(mockCustomsDeclarationsConnector, times(1)).submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext])
      verify(mockSubmissionRepo, times(1)).insert(any[Submission])(any[ExecutionContext])
      verifyZeroInteractions(mockSubmissionActionRepo)
      result shouldBe None
    }

    "save notification Data in repository when submissionAction and Submission exist" in new SetUp() {
      val mockWriteResult: WriteResult = mock[WriteResult]
      when(mockSubmissionRepo.findById(any[BSONObjectID],any[ReadPreference])(any[ExecutionContext])).thenReturn(Future.successful(Some(submissionNoMrn)))
      when(mockSubmissionActionRepo.findByConversationId(any[String])).thenReturn(Future.successful(Some(submissionAction)))
      when(mockSubmissionRepo.updateSubmission(any[Submission])).thenReturn(Future.successful(true))
      when(mockWriteResult.ok).thenReturn(true)
      when(mockSubmissionNotificationRepo.insert(any[SubmissionNotification])(any[ExecutionContext])).thenReturn(Future.successful(mockWriteResult))
      private val submissionCaptor: ArgumentCaptor[Submission] = ArgumentCaptor.forClass(classOf[Submission])
      val result: Boolean = await(testObj.handleNotificationReceived(conversationId, 1, mrn, exampleAcceptNotification("01")))

      verify(mockSubmissionActionRepo, times(1)).findByConversationId(any[String])
      verify(mockSubmissionRepo, times(1)).findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext])
      verify(mockSubmissionRepo, times(1)).updateSubmission(submissionCaptor.capture())
      verify(mockSubmissionNotificationRepo, times(1)).insert(any[SubmissionNotification])(any[ExecutionContext])

      submissionCaptor.getValue.mrn shouldBe Some(mrn)
      result should be (true)

    }


    "save notification Data in repository but do not update submission when MRN is set submissionAction and Submission exist" in new SetUp() {
      val mockWriteResult: WriteResult = mock[WriteResult]
      when(mockSubmissionRepo.findById(any[BSONObjectID],any[ReadPreference])(any[ExecutionContext])).thenReturn(Future.successful(Some(submission)))
      when(mockSubmissionActionRepo.findByConversationId(any[String])).thenReturn(Future.successful(Some(submissionAction)))
      when(mockSubmissionRepo.updateSubmission(any[Submission])).thenReturn(Future.successful(true))
      when(mockWriteResult.ok).thenReturn(true)
      when(mockSubmissionNotificationRepo.insert(any[SubmissionNotification])(any[ExecutionContext])).thenReturn(Future.successful(mockWriteResult))

      val result: Boolean = await(testObj.handleNotificationReceived(conversationId, 1, mrn, exampleAcceptNotification("01")))
      verify(mockSubmissionActionRepo, times(1)).findByConversationId(any[String])
      verify(mockSubmissionRepo, times(1)).findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext])
      verify(mockSubmissionRepo, times(0)).updateSubmission(any[Submission])
      verify(mockSubmissionNotificationRepo, times(1)).insert(any[SubmissionNotification])(any[ExecutionContext])

      result should be (true)

    }

    "return submissions from submission repository when called with Eori" in new SetUp (){
      val submissions = Seq(Declaration(declarantEoriValue, declarantLrnValue, DateTime.now, Some(mrn), Seq.empty))
      when(mockSubmissionRepo.findByEori(declarantEoriValue)).thenReturn(Future.successful(submissions))
      val result: Seq[Declaration] = await(testObj.getSubmissions(declarantEoriValue))

      result.size should be(1)
    }

    "return false and do not save notification when submissionAction does not exist" in new SetUp() {

      when(mockSubmissionActionRepo.findByConversationId(any[String])).thenReturn(Future.successful(None))

      val result: Boolean = await(testObj.handleNotificationReceived(conversationId, 1, mrn, exampleAcceptNotification("01")))

      verify(mockSubmissionActionRepo, times(1)).findByConversationId(any[String])
      verifyZeroInteractions(mockSubmissionRepo)
      verifyZeroInteractions(mockSubmissionNotificationRepo)
      result should be (false)

    }

    "return false and do not save notification when submission does not exist" in new SetUp() {
      when(mockSubmissionRepo.findById(any[BSONObjectID],any[ReadPreference])(any[ExecutionContext])).thenReturn(Future.successful(None))
      when(mockSubmissionActionRepo.findByConversationId(any[String])).thenReturn(Future.successful(Some(submissionAction)))

      val result: Boolean = await(testObj.handleNotificationReceived(conversationId, 1, mrn,  exampleAcceptNotification("01")))

      verify(mockSubmissionActionRepo, times(1)).findByConversationId(any[String])
      verify(mockSubmissionRepo, times(1)).findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext])
      verifyZeroInteractions(mockSubmissionNotificationRepo)
      result should be (false)

    }

    "save notification in repository even when update submission fails" in new SetUp() {
      val mockWriteResult: WriteResult = mock[WriteResult]
      when(mockSubmissionRepo.findById(any[BSONObjectID],any[ReadPreference])(any[ExecutionContext])).thenReturn(Future.successful(Some(submissionNoMrn)))
      when(mockSubmissionActionRepo.findByConversationId(any[String])).thenReturn(Future.successful(Some(submissionAction)))
      when(mockSubmissionRepo.updateSubmission(any[Submission])).thenReturn(Future.successful(false))
      when(mockWriteResult.ok).thenReturn(true)
      when(mockSubmissionNotificationRepo.insert(any[SubmissionNotification])(any[ExecutionContext])).thenReturn(Future.successful(mockWriteResult))

      val result: Boolean = await(testObj.handleNotificationReceived(conversationId, 1, mrn,  exampleAcceptNotification("01")))

      verify(mockSubmissionActionRepo, times(1)).findByConversationId(any[String])
      verify(mockSubmissionRepo, times(1)).findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext])
      verify(mockSubmissionNotificationRepo, times(1)).insert(any[SubmissionNotification])(any[ExecutionContext])
      result should be (true)

    }



  }
}
