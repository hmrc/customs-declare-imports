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

package uk.gov.hmrc.customs.imports.services

import javax.inject.{Inject, Singleton}
import play.api.Logger
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.customs.imports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.customs.imports.models.{Declaration, Submission, SubmissionAction, SubmissionNotification}
import uk.gov.hmrc.customs.imports.repositories.{SubmissionActionRepository, SubmissionNotificationRepository, SubmissionRepository}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class ImportService @Inject()(submissionRepository: SubmissionRepository,
                              submissionActionRepository: SubmissionActionRepository,
                              submissionNotificationRepository: SubmissionNotificationRepository,
                              customsDeclarationsConnector: CustomsDeclarationsConnector) {

  def handleDeclarationSubmit(eori: String, localReferenceNumber: String, xml: NodeSeq)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    customsDeclarationsConnector.submitImportDeclaration(eori, xml.toString()).flatMap({ response =>
      val conversationId = response.conversationId
      Logger.debug(s"conversationId: ${conversationId}")
      val submission = Submission(eori, localReferenceNumber, None)
      submissionRepository
        .insert(submission)
        .flatMap(submissionResult => {
          if (submissionResult.ok) {
            submissionActionRepository.insert(SubmissionAction(submission.id, conversationId))
              .map(submissionActionResult => Some(conversationId))
          } else {
            Future.successful(None)
          }
        })
    })
  }

  def getSubmissions(eori: String): Future[Seq[Declaration]] = {
    submissionRepository.findByEori(eori)
  }



  def handleNotificationReceived(conversationId: String, functionCode: Int, mrn: String, xml: NodeSeq): Future[Boolean] = {
    for {
      submissionId <- findSubmissionIdByConversationId(conversationId)
      submission <- findSubmissionBySubmissionId(submissionId)
      updateResult <- updateSubmissionWithMrn(mrn, submission)
      persistNotificationResult <- handlePersistNotification(functionCode, conversationId, submission, updateResult)
    } yield persistNotificationResult

  }

  private def findSubmissionBySubmissionId(mayBeSubmissionid : Option[BSONObjectID]): Future[Option[Submission]] = {
    mayBeSubmissionid match {
      case Some(submissionId: BSONObjectID) => {
        submissionRepository.findById(submissionId)
      }
      case None => Future.successful(None)
    }
  }


  private def updateSubmissionWithMrn(mrn: String, submission: Option[Submission]): Future[Boolean] ={
    submission.fold(Future.successful(false)){ submission =>
      submission.mrn match {
        case None => {
          Logger.info(s"updating submission with mrn: $mrn")
          submissionRepository.updateSubmission(submission.copy(mrn = Some(mrn)))
        }
        case Some(existingMrn) => {
          Logger.info (s"mrn: $existingMrn is populated on submission so not updating with $mrn")
          Future.successful (false)
        }
      }
    }
  }

  private def findSubmissionIdByConversationId(conversationId: String): Future[Option[BSONObjectID]] ={
    submissionActionRepository.findByConversationId(conversationId)
      .map(submissionActionOption => {
            submissionActionOption match {
              case Some(submissionAction) => Some(submissionAction.submissionId)
              case None => {
                Logger.error(s"unable to find submission for conversationId: ${conversationId}")
                None
              }
            }
        })
  }


  private def handlePersistNotification(functionCode: Int,
                                        conversationId: String,
                                        submission: Option[Submission],
                                        updateResult: Boolean): Future[Boolean] = {
    if (!updateResult) {
      Logger.error(s"unable to updateSubmission with received MRN conversationId:$conversationId")
    }
    submission match {
      case Some(_) =>  submissionNotificationRepository.insert(SubmissionNotification(functionCode, conversationId))
        .map(writeResult => writeResult.ok)
      case None => Future.successful(false)
    }

  }

}
