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

package uk.gov.hmrc.customs.imports.repositories

import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.customs.imports.base.{CustomsImportsBaseSpec, ImportsTestData}

import scala.concurrent.ExecutionContext.Implicits.global

class NotificationsRepositorySpec extends CustomsImportsBaseSpec with BeforeAndAfterEach with ImportsTestData {

  override protected def afterEach(): Unit = {
    super.afterEach()
    repo.removeAll()
  }

  override lazy val app: Application = GuiceApplicationBuilder().build()

  val repo: NotificationsRepository = component[NotificationsRepository]

  "NotificationsRepository" should {
    "save notification with eori, conversationId and timestamp" in {
      repo.save(notification).futureValue must be(true)

      // we can now display a list of all the declarations belonging to the current user, searching by EORI
      val foundDeclarationNotification = repo.findByEori(eori).futureValue
      foundDeclarationNotification.length must be(1)
      foundDeclarationNotification.head.eori must be(eori)
      foundDeclarationNotification.head.conversationId must be(conversationId)

      foundDeclarationNotification.head.dateTimeReceived must be(now)

      // we can also retrieve the submission individually by conversation Id
      val declarationNotification1 = repo.getByConversationId(conversationId).futureValue.value
      declarationNotification1.eori must be(eori)
      declarationNotification1.conversationId must be(conversationId)

      // or we can retrieve it by eori and conversationId
      val declarationNotification2 = repo.getByEoriAndConversationId(eori, conversationId).futureValue.value
      declarationNotification2.eori must be(eori)
      declarationNotification2.conversationId must be(conversationId)
    }
  }
}
