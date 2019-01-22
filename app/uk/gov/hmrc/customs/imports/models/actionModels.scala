package uk.gov.hmrc.customs.imports.models

import play.api.mvc.{Request, WrappedRequest}

trait HasRequest[A] {
  val request: Request[A]
}

trait HasLocalReferenceNumber {
  val localReferenceNumber: LocalReferenceNumber
}

trait HasEori {
  val eori: Eori
}

case class LocalReferenceNumber(value: String) extends AnyVal

case class Eori(value: String) extends AnyVal


case class ValidatedHeadersRequest[A](localReferenceNumber: LocalReferenceNumber, request: Request[A]) extends WrappedRequest[A](request)
  with HasRequest[A] with HasLocalReferenceNumber

case class AuthorizedRequest[A](localReferenceNumber: LocalReferenceNumber, eori: Eori, request: ValidatedHeadersRequest[A]) extends WrappedRequest[A](request)
  with HasRequest[A] with HasLocalReferenceNumber with HasEori
