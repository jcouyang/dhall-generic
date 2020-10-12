package us.oyanglul.dhall

import org.dhallj.codec._
import org.dhallj.core.Expr
import org.dhallj.ast._
import org.dhallj.codec.Decoder._

object generic {
  implicit def decodeCoproduct[A: Decoder]: Decoder[List[A]] = new Decoder[List[A]] {
    def decode(expr: Expr): Result[List[A]] = expr.normalize match {
      case RecordLiteral(fields) => ???
      case other => Left(new DecodingFailure("Record", other))
    }

    def isValidType(typeExpr: Expr): Boolean = typeExpr match {
      case Application(Expr.Constants.LIST, elementType) => Decoder[A].isValidType(elementType)
      case _                                             => false
    }

    def isExactType(typeExpr: Expr): Boolean = typeExpr match {
      case Application(Expr.Constants.LIST, elementType) => Decoder[A].isExactType(elementType)
      case _                                             => false
    }
  }
}
