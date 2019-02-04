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
import uk.gov.hmrc.customs.imports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.customs.imports.models.{Submission, SubmissionAction, SubmissionNotification}
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

  def handleDeclarationSubmit(eori: String, localReferenceNumber: String, xml: NodeSeq)(implicit hc: HeaderCarrier): Future[Boolean] = {
    customsDeclarationsConnector.submitImportDeclaration(eori, xml.toString()).flatMap({ response =>
      Logger.debug(s"conversationId: ${response.conversationId}")
      val submission = Submission(eori, localReferenceNumber, None)
      submissionRepository
        .insert(submission)
        .flatMap(submissionResult => {
          if(submissionResult.ok){
            submissionActionRepository.insert(SubmissionAction(submission.id, response.conversationId))
              .map(submissionActionResult => submissionActionResult.ok)
          }else{
            Future.successful(false)
          }
        })
    })
  }

  def getSubmissions(eori: String) = {
    submissionRepository.findByEori(eori)
  }

  def handleNotificationReceived(conversationId: String,  xml: NodeSeq): Unit = {
    //TODO flow here should be
    //1). parse xml and pull out the function code and MRN
    //2). update the submission with the MRN, if empty
    //3). create notification object and persist

    val functionCode = {xml \\ "FunctionCode"  text}.toInt
    val mrn = xml \\ "ID" text

    submissionActionRepository.findByConversationId(conversationId).map(mayBeSubmissionAction => mayBeSubmissionAction match {
      case Some(submissionAction) => {
        submissionRepository.findById(submissionAction.submissionId)
          .map({
            maybeSubmission => {
              maybeSubmission match {
                case Some(submission) => {
                  Logger.debug(s"Retrieved Submission ${submission.id.stringify}")
                  val updatedSubmission = submission.copy(mrn = Some(mrn))
                  submissionRepository.updateSubmission(updatedSubmission).map(updateResult => {
                    if (!updateResult) {
                      Logger.error("unable to updateSubmission with received MRN ")
                    }
                    Logger.error("about to persist the notification")
                    submissionNotificationRepository.insert(SubmissionNotification(functionCode, conversationId)).map( writeResult =>
                     if(writeResult.ok) {
                       Logger.debug("notification persisted ok")
                     }else{
                       Logger.error("unable to persist notification ")
                     }
                    )
                  }).recover({case ex => Logger.error(s"unable to update submission ${ex.getMessage}", ex)})
                }
                case None => {
                  Logger.error(s"Unable to find submission for conversationID:${conversationId}")
                }
              }
            }
          }).recover({ case ex =>
            Logger.error(s"error getting submission ${ex.getMessage}", ex)
          })
      }
      case None =>  Logger.error(s"Unable to find submissionAction for conversationID:${conversationId}")
    }).recover({case ex => Logger.error(s"error findByConversationId ${ex.getMessage}", ex)})


  }

}
