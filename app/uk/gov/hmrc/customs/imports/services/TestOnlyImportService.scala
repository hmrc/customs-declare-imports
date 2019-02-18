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
      submissionActions <- submissionActionRepository.getBySubmissionId(submission.head.id)
      _                 <- deleteSubmissionActions(submissionActions)
      _                 <- deleteSubmissionNotifications(submissionActions)
      result            <- submissionRepository.deleteById(submission.head.id)
    } yield result

  }

  private def deleteSubmissionActions(submissionActions: Seq[SubmissionAction]): Future[Unit] = {
    Future.successful(
      submissionActions.foreach(submissionAction => { submissionActionRepository.deleteBySubmissionId(submissionAction.submissionId)})
    )
  }

  private def deleteSubmissionNotifications(submissionActions: Seq[SubmissionAction]): Future[Unit] = {
    Future.successful(
      submissionActions.foreach({ submissionAction => submissionNotificationRepository.deleteByConversationId(submissionAction.conversationId)})
    )
  }

}
