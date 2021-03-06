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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsString
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.customs.imports.models.SubmissionNotification
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionNotificationRepository @Inject()(mc: ReactiveMongoComponent)(implicit ec: ExecutionContext)
    extends ReactiveRepository[SubmissionNotification, BSONObjectID](
      "submissionsNotifications",
      mc.mongoConnector.db,
      SubmissionNotification.formats,
      objectIdFormats
    ) {


  def deleteByConversationId(conversationId: String): Future[Boolean] = {
    remove("conversationId" -> JsString(conversationId)).map(_.ok)
  }

  override def indexes: Seq[Index] = Seq(
    Index(Seq("functionCode" -> IndexType.Ascending), name = Some("functionCodeIdx")),
    Index(Seq("conversationId" -> IndexType.Ascending),  name = Some("conversationIdIdx"))
  )

}
