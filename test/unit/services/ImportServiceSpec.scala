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
import org.mockito.{ArgumentCaptor, ArgumentMatcher}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.customs.imports.connectors.{CustomsDeclarationsConnector, CustomsDeclarationsResponse}
import uk.gov.hmrc.customs.imports.controllers.ErrorResponse
import uk.gov.hmrc.customs.imports.models.SubmissionActionType.SubmissionActionType
import uk.gov.hmrc.customs.imports.models._
import uk.gov.hmrc.customs.imports.repositories.{SubmissionActionRepository, SubmissionNotificationRepository, SubmissionRepository}
import uk.gov.hmrc.customs.imports.services.ImportService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import unit.base.ImportsTestData

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem
import scala.xml.Utility.trim

class ImportServiceSpec extends MockitoSugar with UnitSpec with ScalaFutures with ImportsTestData {

  trait SetUp {
    val mockSubmissionRepo: SubmissionRepository = mock[SubmissionRepository]
    val mockSubmissionActionRepo: SubmissionActionRepository = mock[SubmissionActionRepository]
    val mockSubmissionNotificationRepo: SubmissionNotificationRepository = mock[SubmissionNotificationRepository]
    val mockCustomsDeclarationsConnector: CustomsDeclarationsConnector = mock[CustomsDeclarationsConnector]
    implicit val hc: HeaderCarrier = mock[HeaderCarrier]
    val testObj = new ImportService(mockSubmissionRepo,
      mockSubmissionActionRepo,
      mockSubmissionNotificationRepo,
      mockCustomsDeclarationsConnector)
  }

  "handleDeclarationSubmit" should {
    "save submission Data in repository" in new SetUp() {
      val mockWriteResult: WriteResult = mock[WriteResult]
      val xmlVal: Elem = <iamxml></iamxml>
      val conversationId = randomConversationId

      when(mockCustomsDeclarationsConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(CustomsDeclarationsResponse(conversationId)))
      when(mockSubmissionRepo.insert(any[Submission])(any[ExecutionContext])).thenReturn(Future.successful(mockWriteResult))
      when(mockSubmissionActionRepo.insert(any[SubmissionAction])(any[ExecutionContext])).thenReturn(Future.successful(mockWriteResult))
      when(mockWriteResult.ok).thenReturn(true)

      val result: Option[String] = await(testObj.handleDeclarationSubmit(declarantEoriValue, declarantLrnValue, xmlVal))

      verify(mockCustomsDeclarationsConnector, times(1)).submitImportDeclaration(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext])
      verify(mockSubmissionRepo, times(1)).insert(any[Submission])(any[ExecutionContext])
      verify(mockSubmissionActionRepo, times(1)).insert(any[SubmissionAction])(any[ExecutionContext])
      result shouldBe Some(conversationId)
    }

    "save not call submissionAction Repo if submission insert fails" in new SetUp() {
      val mockSubmissionWriteResult: WriteResult = mock[WriteResult]
      val xmlVal: Elem = <iamxml></iamxml>
      val conversationId = randomConversationId

      when(mockCustomsDeclarationsConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(CustomsDeclarationsResponse(conversationId)))
      when(mockSubmissionRepo.insert(any[Submission])(any[ExecutionContext])).thenReturn(Future.successful(mockSubmissionWriteResult))
      when(mockSubmissionWriteResult.ok).thenReturn(false)

      val result: Option[String] = await(testObj.handleDeclarationSubmit(declarantEoriValue, declarantLrnValue, xmlVal))

      verify(mockCustomsDeclarationsConnector, times(1)).submitImportDeclaration(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext])
      verify(mockSubmissionRepo, times(1)).insert(any[Submission])(any[ExecutionContext])
      verifyZeroInteractions(mockSubmissionActionRepo)
      result shouldBe None
    }
  }

  "handleNotificationReceived" should {

    "save notification Data in repository when submissionAction and Submission exist" in new SetUp() {
      val mockWriteResult: WriteResult = mock[WriteResult]
      when(mockSubmissionRepo.findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext])).thenReturn(Future.successful(Some(submissionNoMrn)))
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
      result should be(true)
    }

    "save notification Data in repository but do not update submission when MRN is set submissionAction and Submission exist" in new SetUp() {
      val mockWriteResult: WriteResult = mock[WriteResult]
      when(mockSubmissionRepo.findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext])).thenReturn(Future.successful(Some(submission)))
      when(mockSubmissionActionRepo.findByConversationId(any[String])).thenReturn(Future.successful(Some(submissionAction)))
      when(mockSubmissionRepo.updateSubmission(any[Submission])).thenReturn(Future.successful(true))
      when(mockWriteResult.ok).thenReturn(true)
      when(mockSubmissionNotificationRepo.insert(any[SubmissionNotification])(any[ExecutionContext])).thenReturn(Future.successful(mockWriteResult))

      val result: Boolean = await(testObj.handleNotificationReceived(conversationId, 1, mrn, exampleAcceptNotification("01")))
      verify(mockSubmissionActionRepo, times(1)).findByConversationId(any[String])
      verify(mockSubmissionRepo, times(1)).findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext])
      verify(mockSubmissionRepo, times(0)).updateSubmission(any[Submission])
      verify(mockSubmissionNotificationRepo, times(1)).insert(any[SubmissionNotification])(any[ExecutionContext])

      result should be(true)
    }

    "return false and do not save notification when submissionAction does not exist" in new SetUp() {
      when(mockSubmissionActionRepo.findByConversationId(any[String])).thenReturn(Future.successful(None))

      val result: Boolean = await(testObj.handleNotificationReceived(conversationId, 1, mrn, exampleAcceptNotification("01")))

      verify(mockSubmissionActionRepo, times(1)).findByConversationId(any[String])
      verifyZeroInteractions(mockSubmissionRepo)
      verifyZeroInteractions(mockSubmissionNotificationRepo)
      result should be(false)
    }

    "return false and do not save notification when submission does not exist" in new SetUp() {
      when(mockSubmissionRepo.findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext])).thenReturn(Future.successful(None))
      when(mockSubmissionActionRepo.findByConversationId(any[String])).thenReturn(Future.successful(Some(submissionAction)))

      val result: Boolean = await(testObj.handleNotificationReceived(conversationId, 1, mrn, exampleAcceptNotification("01")))

      verify(mockSubmissionActionRepo, times(1)).findByConversationId(any[String])
      verify(mockSubmissionRepo, times(1)).findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext])
      verifyZeroInteractions(mockSubmissionNotificationRepo)
      result should be(false)
    }

    "save notification in repository even when update submission fails" in new SetUp() {
      val mockWriteResult: WriteResult = mock[WriteResult]
      when(mockSubmissionRepo.findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext])).thenReturn(Future.successful(Some(submissionNoMrn)))
      when(mockSubmissionActionRepo.findByConversationId(any[String])).thenReturn(Future.successful(Some(submissionAction)))
      when(mockSubmissionRepo.updateSubmission(any[Submission])).thenReturn(Future.successful(false))
      when(mockWriteResult.ok).thenReturn(true)
      when(mockSubmissionNotificationRepo.insert(any[SubmissionNotification])(any[ExecutionContext])).thenReturn(Future.successful(mockWriteResult))

      val result: Boolean = await(testObj.handleNotificationReceived(conversationId, 1, mrn, exampleAcceptNotification("01")))

      verify(mockSubmissionActionRepo, times(1)).findByConversationId(any[String])
      verify(mockSubmissionRepo, times(1)).findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext])
      verify(mockSubmissionNotificationRepo, times(1)).insert(any[SubmissionNotification])(any[ExecutionContext])
      result should be(true)
    }
  }

  "getSubmissions" should {
    "return submissions from submission repository when called with Eori" in new SetUp() {
      val submissions = Seq(Declaration(declarantEoriValue, declarantLrnValue, DateTime.now, Some(mrn), Seq.empty))
      when(mockSubmissionRepo.findByEori(declarantEoriValue)).thenReturn(Future.successful(submissions))
      val result: Seq[Declaration] = await(testObj.getSubmissions(declarantEoriValue))

      result.size should be(1)
    }
  }

  "cancelDeclaration" should {
    val description = "a description"
    val changeReasonCode = 1
    val cancellation = Cancellation(mrn, changeReasonCode, description)
    val mockWriteResult: WriteResult = mock[WriteResult]
    val expectedCancellationXml = trim(<MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
        <wstxns1:Declaration xmlns:wstxns1="urn:wco:datamodel:WCO:DEC-DMS:2">
          <wstxns1:FunctionCode>13</wstxns1:FunctionCode>
          <wstxns1:FunctionalReferenceID>{lrn}</wstxns1:FunctionalReferenceID>
          <wstxns1:ID>{mrn}</wstxns1:ID>
          <wstxns1:TypeCode>INV</wstxns1:TypeCode>
          <wstxns1:AdditionalInformation>
            <wstxns1:StatementDescription>{description}</wstxns1:StatementDescription>
          </wstxns1:AdditionalInformation>
          <wstxns1:Amendment>
            <wstxns1:ChangeReasonCode>{changeReasonCode}</wstxns1:ChangeReasonCode>
          </wstxns1:Amendment>
        </wstxns1:Declaration>
    </MetaData>)

    "return an error when the MRN does not relate to the requesting EORI" in new SetUp {
      when(mockSubmissionRepo.getByEoriAndMrn(eori, mrn)).thenReturn(Future.successful(None))

      val result = await(testObj.cancelDeclaration(eori, lrn, cancellation))

      result shouldBe Left(ErrorResponse.ErrorGenericBadRequest)

      verify(mockCustomsDeclarationsConnector, never()).submitImportDeclaration(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext])
      verify(mockSubmissionActionRepo, never()).insert(any[SubmissionAction])(any[ExecutionContext])
    }

    "return an error when the MRN is for an existing submission for the request EORI but the cancellation request fails" in new SetUp {
      when(mockSubmissionRepo.getByEoriAndMrn(eori, mrn)).thenReturn(Future.successful(Some(submission)))
      when(mockCustomsDeclarationsConnector.submitImportDeclaration(any(), any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Internal Server Error")))

      val result = await(testObj.cancelDeclaration(eori, lrn, cancellation))

      result shouldBe Left(ErrorResponse.ErrorInternalServerError)

      verify(mockCustomsDeclarationsConnector).submitImportDeclaration(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext])
      verify(mockSubmissionActionRepo, never()).insert(any[SubmissionAction])(any[ExecutionContext])
    }

    "return an error when the MRN is for an existing submission for the request EORI but the persistence fails" in new SetUp {
      when(mockSubmissionRepo.getByEoriAndMrn(eori, mrn)).thenReturn(Future.successful(Some(submission)))
      when(mockCustomsDeclarationsConnector.submitImportDeclaration(any(), any())(any(), any()))
        .thenReturn(Future.successful(CustomsDeclarationsResponse(conversationId)))
      when(mockSubmissionActionRepo.insert(any())(any())).thenReturn(mockWriteResult)
      when(mockWriteResult.ok).thenReturn(false)

      val result = await(testObj.cancelDeclaration(eori, lrn, cancellation))

      result shouldBe Left(ErrorResponse.ErrorInternalServerError)

      verify(mockCustomsDeclarationsConnector).submitImportDeclaration(meq(eori), meq(expectedCancellationXml.toString))(any[HeaderCarrier], any[ExecutionContext])
      verify(mockSubmissionActionRepo).insert(any[SubmissionAction])(any[ExecutionContext])
    }

    "persist the submissionAction and return the conversationId when MRN exists and the cancellation request is successful" in new SetUp {
      val expectedSubmissionAction = SubmissionAction(submission.id, conversationId, SubmissionActionType.CANCELLATION)
      when(mockSubmissionRepo.getByEoriAndMrn(eori, mrn)).thenReturn(Future.successful(Some(submission)))
      when(mockCustomsDeclarationsConnector.submitImportDeclaration(any(), any())(any(), any()))
        .thenReturn(Future.successful(CustomsDeclarationsResponse(conversationId)))
      when(mockSubmissionActionRepo.insert(any())(any())).thenReturn(mockWriteResult)
      when(mockWriteResult.ok).thenReturn(true)

      val result = await(testObj.cancelDeclaration(eori, lrn, cancellation))

      result shouldBe Right(conversationId)

      verify(mockCustomsDeclarationsConnector).submitImportDeclaration(meq(eori), meq(expectedCancellationXml.toString))(any[HeaderCarrier], any[ExecutionContext])
      verify(mockSubmissionActionRepo).insert(argThat(IsExpectedSubmissionAction(expectedSubmissionAction)))(any[ExecutionContext])
    }

    case class IsExpectedSubmissionAction(sa: SubmissionAction) extends ArgumentMatcher[SubmissionAction] {
      def matches(request: SubmissionAction): Boolean = request match {
        case SubmissionAction(submissionId, conversationId, actionType, _, _) =>
          submissionId == sa.submissionId && conversationId == sa.conversationId && actionType == sa.actionType
        case _ => false
      }
    }
  }
}
