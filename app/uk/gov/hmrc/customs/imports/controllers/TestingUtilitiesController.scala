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

package uk.gov.hmrc.customs.imports.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.customs.imports.config.AppConfig
import uk.gov.hmrc.customs.imports.repositories.SubmissionRepository
import uk.gov.hmrc.customs.imports.services.TestOnlyImportService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext

@Singleton
class TestingUtilitiesController @Inject()(appConfig: AppConfig,
                                           submissionRepository: SubmissionRepository,
                                           importService: TestOnlyImportService)(implicit ec: ExecutionContext) extends BaseController {

//TODO delete submission data by eori


  def deleteSubmissionByEoriAndLrn(eori: String, lrn: String): Action[AnyContent] = Action.async {
     implicit req => {
      importService.deleteByEoriAndLrn(eori, lrn).map {
        case true => Ok
        case false => InternalServerError
      }
    }
  }

  def deleteCancelActionByConversationId(conversationId: String): Action[AnyContent] = Action.async {
    implicit req => {
      importService.deleteCancellationActionsByConversationId(conversationId).map {
        case true => Ok
        case false => InternalServerError
      }
    }
  }
}
