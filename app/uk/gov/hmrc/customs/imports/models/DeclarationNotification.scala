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

package uk.gov.hmrc.customs.imports.models

import org.joda.time.DateTime
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.wco.dec._

import scala.xml.Elem

case class DeclarationMetadata(
  wcoDataModelVersionCode: Option[String] = None, // max 6 chars
  wcoTypeName: Option[String] = None, // no constraint
  responsibleCountryCode: Option[String] = None, // max 2 chars - ISO 3166-1 alpha2 code
  responsibleAgencyName: Option[String] = None, // max 70 chars
  agencyAssignedCustomizationCode: Option[String] = None, // max 6 chars
  agencyAssignedCustomizationVersionCode: Option[String] = None // max 3 chars
)

object DeclarationMetadata {
  implicit val declarationMetadataFormats: OFormat[DeclarationMetadata] = Json.format[DeclarationMetadata]
}

object DeclarationNotification {
  implicit val measureFormats: OFormat[Measure] = Json.format[Measure]
  implicit val amountFormats: OFormat[Amount] = Json.format[Amount]
  implicit val dateTimeStringFormats: OFormat[DateTimeString] = Json.format[DateTimeString]
  implicit val responseDateTimeElementFormats: OFormat[ResponseDateTimeElement] = Json.format[ResponseDateTimeElement]
  implicit val responsePointerFormats: OFormat[ResponsePointer] = Json.format[ResponsePointer]
  implicit val responseDutyTaxFeePaymentFormats: OFormat[ResponseDutyTaxFeePayment] = Json.format[ResponseDutyTaxFeePayment]
  implicit val responseCommodityDutyTaxFeeFormats: OFormat[ResponseCommodityDutyTaxFee] = Json.format[ResponseCommodityDutyTaxFee]
  implicit val responseCommodityFormats: OFormat[ResponseCommodity] = Json.format[ResponseCommodity]
  implicit val responseGovernmentAgencyGoodsItemFormats: OFormat[ResponseGovernmentAgencyGoodsItem] = Json.format[ResponseGovernmentAgencyGoodsItem]
  implicit val responseGoodsShipmentFormats: OFormat[ResponseGoodsShipment] = Json.format[ResponseGoodsShipment]
  implicit val responseCommunicationFormats: OFormat[ResponseCommunication] = Json.format[ResponseCommunication]
  implicit val responseObligationGuaranteeFormats: OFormat[ResponseObligationGuarantee] = Json.format[ResponseObligationGuarantee]
  implicit val responseStatusFormats: OFormat[ResponseStatus] = Json.format[ResponseStatus]
  implicit val responseAdditionalInformationFormats: OFormat[ResponseAdditionalInformation] = Json.format[ResponseAdditionalInformation]
  implicit val responseAmendmentFormats: OFormat[ResponseAmendment] = Json.format[ResponseAmendment]
  implicit val responseAppealOfficeFormats: OFormat[ResponseAppealOffice] = Json.format[ResponseAppealOffice]
  implicit val responseBankFormats: OFormat[ResponseBank] = Json.format[ResponseBank]
  implicit val responseContactOffice: OFormat[ResponseContactOffice] = Json.format[ResponseContactOffice]
  implicit val responseErrorFormats: OFormat[ResponseError] = Json.format[ResponseError]
  implicit val responsePaymentFormats: OFormat[ResponsePayment] = Json.format[ResponsePayment]
  implicit val responseDutyTaxFee: OFormat[ResponseDutyTaxFee] = Json.format[ResponseDutyTaxFee]
  implicit val responseDeclarationFormats: OFormat[ResponseDeclaration] = Json.format[ResponseDeclaration]
  implicit val responseFormats: OFormat[Response] = Json.format[Response]
  implicit val declarationMetadataFormats: OFormat[DeclarationMetadata] = Json.format[DeclarationMetadata]
  implicit val exportsNotificationFormats: OFormat[DeclarationNotification] = Json.format[DeclarationNotification]

}

case class DeclarationNotification(
  dateTimeReceived: DateTime = DateTime.now(),
  conversationId: String,
  eori: String,
  badgeId: Option[String] = None,
  metadata: DeclarationMetadata,
  response: Seq[Response] = Seq.empty
)

//case class NotificationApiHeaders(
//  accept: String,
//  contentType: String,
//  clientId: String,
//  badgeId: Option[String],
//  conversationId: String,
//  eori: String
//)

