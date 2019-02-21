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
import uk.gov.hmrc.customs.imports.models._
import uk.gov.hmrc.customs.imports.repositories.{SubmissionActionRepository, SubmissionNotificationRepository, SubmissionRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TestOnlyImportService @Inject()(submissionRepository: SubmissionRepository,
                                      submissionActionRepository: SubmissionActionRepository,
                                      submissionNotificationRepository: SubmissionNotificationRepository) {

  def deleteByEoriAndLrn(eori: String, lrn: String): Future[Boolean] = {
    for {
      submission        <- submissionRepository.getByEoriAndLrn(eori, lrn)
      childResult       <- deleteSubmissionChildren(submission)
      result            <- deleteSubmission(submission)
    } yield result && childResult
  }

  def deleteCancellationActionsByConversationId(conversationId: String): Future[Boolean] ={
   for {
      actionDeleteResult        <- submissionActionRepository.deleteCancellationActionsByConversationId(conversationId)
      notificationDeleteResult  <- submissionNotificationRepository.deleteByConversationId(conversationId)
    } yield actionDeleteResult && notificationDeleteResult
  }

  private def deleteSubmission(mayBeSubmission: Option[Submission]): Future[Boolean] = {
    mayBeSubmission.fold(Future.successful(true)){ submission => submissionRepository.deleteById(submission.id) }
  }

  private def deleteBySubmissionAction(submissionAction: SubmissionAction): Future[Boolean] = {
    for {
      deleteActionResult        <-  submissionActionRepository.deleteBySubmissionId(submissionAction.submissionId)
      deleteNotificationResult  <-  submissionNotificationRepository.deleteByConversationId(submissionAction.conversationId)
    } yield deleteActionResult && deleteNotificationResult
  }

  private def combineResults(submissionActions: Seq[SubmissionAction]): Future[Boolean] = {
    Future.sequence(submissionActions.map(submissionAction => deleteBySubmissionAction(submissionAction)))
      .map(results => results.forall(b => b))
  }

  private def deleteSubmissionChildren(mayBeSubmission: Option[Submission]): Future[Boolean] = {
    mayBeSubmission.fold(Future.successful(true)){ submission =>
      for {
        submissionActions <- submissionActionRepository.findBySubmissionId(submission.id)
        results           <- combineResults(submissionActions)
      } yield results
    }
  }

}
