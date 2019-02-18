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
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.{FailoverStrategy, ReadPreference}
import reactivemongo.api.commands.Command
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.JSONSerializationPack
import uk.gov.hmrc.customs.imports.models.{Declaration, Submission}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats
import reactivemongo.play.json._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionRepository @Inject()(implicit mc: ReactiveMongoComponent, ec: ExecutionContext)
  extends ReactiveRepository[Submission, BSONObjectID](
    "submissions",
    mc.mongoConnector.db,
    Submission.formats,
    objectIdFormats
  ) {


  def deleteById(id: BSONObjectID) : Future[Boolean] = {
    removeById(id).map(_.ok)
  }

  def getByEoriAndLrn(eori: String, localReferenceNumber: String):  Future[Option[Submission]] =
  find("eori" -> JsString(eori), "localReferenceNumber" -> JsString(localReferenceNumber)).map(_.headOption)

  override def indexes: Seq[Index] = Seq(
    Index(Seq("eori" -> IndexType.Ascending), name = Some("eoriIdx")),
    Index(Seq("localReferenceNumber" -> IndexType.Ascending), name = Some("lrnIdx"))
  )

  def findByEori(eori: String): Future[Seq[Declaration]] = {
    val commandDoc = Json.obj(
      "aggregate" -> collectionName,
      "pipeline" -> List(
        Json.obj("$match" -> Json.obj("eori" -> eori)),
        Json.obj(
          "$lookup" -> Json.obj(
            "from" -> "submissionActions",
            "localField" -> "_id",
            "foreignField" -> "submissionId",
            "as" -> "actions")),
        Json.obj(
          "$unwind" -> Json.obj(
            "path" -> "$actions",
            "preserveNullAndEmptyArrays" -> true)),
        Json.obj(
          "$lookup" -> Json.obj(
            "from" -> "submissionsNotifications",
            "localField" -> "actions.conversationId",
            "foreignField" -> "conversationId",
            "as" -> "actions.notifications")),
        Json.obj("$sort" -> Json.obj("submittedDateTime" -> -1)),
        Json.obj(
          "$group" -> Json.obj(
            "_id" -> "$_id",
            "eori" -> Json.obj("$first" -> "$eori"),
            "localReferenceNumber" -> Json.obj("$first" -> "$localReferenceNumber"),
            "mrn" -> Json.obj("$first" -> "$mrn"),
            "submittedDateTime" -> Json.obj("$first" -> "$submittedDateTime"),
            "actions" -> Json.obj("$push" -> "$actions"))),
        Json.obj(
          "$project" -> Json.obj(
            "_id" -> 1,
            "eori" -> 1,
            "localReferenceNumber" -> 1,
            "mrn" -> 1,
            "submittedDateTime" -> 1,
            "actions" -> Json.obj(
              "$filter" -> Json.obj("input" -> "$actions", "as" -> "a", "cond" -> Json.obj("$ifNull" -> Json.arr("$$a._id", false))))))),
    "allowDiskUse" -> true)

    val runner = Command.run(JSONSerializationPack, FailoverStrategy())
    runner.apply(collection.db, runner.rawCommand(commandDoc)).one[JsObject](ReadPreference.Primary).flatMap { json =>
      (json \ "result").validate[Seq[Declaration]] match {
        case JsSuccess(result, _) => Future.successful(result)
        case JsError(errors) =>
          Future.failed(new RuntimeException((json \ "errmsg").asOpt[String].getOrElse(errors.mkString(","))))
      }
    }
  }

  def getByEoriAndMrn(eori: String, mrn: String): Future[Option[Submission]] =
    find("eori" -> JsString(eori), "mrn" -> JsString(mrn)).map(_.headOption)

  def updateSubmission(submission: Submission) = {
    val finder = Json.obj("eori" -> submission.eori, "localReferenceNumber" -> submission.localReferenceNumber)

    val modifier = Json.obj(
      "$set" ->
        Json.obj("mrn" -> submission.mrn)
    )
    findAndUpdate(finder, modifier, upsert = true)
      .map(_.lastError.exists(_.updatedExisting))
  }


}
