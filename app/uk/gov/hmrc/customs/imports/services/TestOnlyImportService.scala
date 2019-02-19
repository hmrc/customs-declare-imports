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
import uk.gov.hmrc.customs.imports.models._
import uk.gov.hmrc.customs.imports.repositories.{SubmissionActionRepository, SubmissionNotificationRepository, SubmissionRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TestOnlyImportService @Inject()(submissionRepository: SubmissionRepository,
                                      submissionActionRepository: SubmissionActionRepository,
                                      submissionNotificationRepository: SubmissionNotificationRepository) {

  def deleteByEoriAndLrn(eori: String, lrn: String): Future[Boolean] = {


    def deleteSubmission(mayBeSubmission: Option[Submission]): Future[Boolean] = {
      mayBeSubmission.fold(Future.successful(true)){ submission =>
        submissionRepository.deleteById(submission.id)
      }
    }

    def deleteSubmissionActions(submissionActions: Seq[SubmissionAction]): Future[Unit] = {
      Future.successful(
        submissionActions.foreach(submissionAction => { submissionActionRepository.deleteBySubmissionId(submissionAction.submissionId)})
      )
    }

    def deleteChildren(mayBeSubmission: Option[Submission]): Future[Boolean] = {
      mayBeSubmission.fold(Future.successful(true)){ submission =>
        submissionActionRepository.getBySubmissionId(submission.id).map(
          submissionActions => {
            submissionActions.foreach(sa => Logger.debug(s"we have submission Action: ${sa.conversationId} ${sa.actionType}"))
            deleteSubmissionActions(submissionActions)
            deleteSubmissionNotifications(submissionActions)
            true
          }
        )
      }

    }

    def deleteSubmissionNotifications(submissionActions: Seq[SubmissionAction]): Future[Unit] = {
      Future.successful(
        submissionActions.foreach({ submissionAction => submissionNotificationRepository.deleteByConversationId(submissionAction.conversationId)})
      )
    }

    for {
      submission        <- submissionRepository.getByEoriAndLrn(eori, lrn)
      _                 <- deleteChildren(submission)
      result            <- deleteSubmission(submission)
    } yield result

  }

}
