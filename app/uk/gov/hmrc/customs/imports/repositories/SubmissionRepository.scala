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
import play.api.Logger
import play.api.libs.json.JsString
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.customs.imports.models.Submission
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats
import uk.gov.hmrc.mongo.{AtomicUpdate, ReactiveRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionRepository @Inject()(implicit mc: ReactiveMongoComponent, ec: ExecutionContext)
    extends ReactiveRepository[Submission, BSONObjectID](
      "submissions",
      mc.mongoConnector.db,
      Submission.formats,
      objectIdFormats
    ) with AtomicUpdate[Submission] {

  override def indexes: Seq[Index] = Seq(
    Index(Seq("eori" -> IndexType.Ascending), name = Some("eoriIdx")),
    Index(Seq("localReferenceNumber" -> IndexType.Ascending), name = Some("lrnIdx"))
  )

  def findByEori(eori: String): Future[Seq[Submission]] = find("eori" -> JsString(eori))

  def getByEoriAndMrn(eori: String, mrn: String): Future[Option[Submission]] =
    find("eori" -> JsString(eori), "mrn" -> JsString(mrn)).map(_.headOption)

  override def isInsertion(newRecordId: BSONObjectID, oldRecord: Submission): Boolean = newRecordId.equals(oldRecord.id)

  def save(submission: Submission): Future[Boolean] = insert(submission).map { res =>
    if (!res.ok)
    // $COVERAGE-OFF$Trivial
      Logger.error("Error during inserting submission result " + res.writeErrors.mkString("--"))
    // $COVERAGE-ON$
    res.ok
  }

  def updateSubmission(submission: Submission): Future[Boolean] = {
    val finder = BSONDocument("_id" -> submission.id, "mrn" -> submission.mrn)

    val modifier = BSONDocument(
      "$set" ->
        BSONDocument("mrn" -> submission.mrn)
    )
    atomicUpdate(finder, modifier).map(res => res.get.writeResult.ok)
  }

}
