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
import play.api.http.Status._
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.customs.imports.connectors.{CustomsDeclarationsConnector, CustomsDeclarationsResponse}
import uk.gov.hmrc.customs.imports.models.Submission
import uk.gov.hmrc.customs.imports.repositories.SubmissionRepository
import uk.gov.hmrc.customs.imports.services.ImportService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import unit.base.ImportsTestData

import scala.concurrent.{ExecutionContext, Future}

class ImportServiceSpec extends MockitoSugar with UnitSpec with ScalaFutures with ImportsTestData{

  trait SetUp {
    val mockSubmissionRepo = mock[SubmissionRepository]
    val mockCustomsDeclarationsConnector = mock[CustomsDeclarationsConnector]
    implicit val hc = mock[HeaderCarrier]
    val testObj = new ImportService(mockSubmissionRepo, mockCustomsDeclarationsConnector)
  }

  "ImportService" should {
    "save submission Data in repository" in new SetUp() {
      val mockWriteResult = mock[WriteResult]

      when(mockCustomsDeclarationsConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext]))
        .thenReturn(Future.successful(CustomsDeclarationsResponse(randomConversationId)))
      when(mockSubmissionRepo.save(any[Submission])).thenReturn(Future.successful(mockWriteResult))
      when(mockWriteResult.ok).thenReturn(true)

      val xmlVal = <iamxml></iamxml>
      val result = testObj.handleDeclarationSubmit(declarantEoriValue, declarantLrnValue,  xmlVal).futureValue

      verify(mockCustomsDeclarationsConnector, times(1)).submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext])
      verify(mockSubmissionRepo, times(1)).save(any[Submission])

      status(result) should be(OK)
    }
  }
}
