package us.oyanglul.dhall

import org.dhallj.ast.{Application, FieldAccess, RecordLiteral, UnionType}
import org.dhallj.codec.Decoder.Result
import org.dhallj.codec.{Decoder, DecodingFailure}
import org.dhallj.core.Expr
import shapeless.labelled.{FieldType, field}
import shapeless._
import cats.syntax.either._

object generic {
  implicit val decodeHNil: Decoder[HNil] = new Decoder[HNil] {
    def decode(expr: Expr): Result[HNil] = HNil.asRight
    def isValidType(typeExpr: Expr): Boolean = true
    def isExactType(typeExpr: Expr): Boolean = true
  }

  implicit val decodeCNil: Decoder[CNil] = new Decoder[CNil] {
    def decode(expr: Expr): Result[CNil] =
      Left(new DecodingFailure("Inconceivable! Coproduct never be CNil", expr))
    def isValidType(typeExpr: Expr): Boolean = true
    def isExactType(typeExpr: Expr): Boolean = true
  }

  implicit def decodeGeneric[A, ARepr](implicit
      gen: LabelledGeneric.Aux[A, ARepr],
      decoder: Lazy[Decoder[ARepr]]
  ): Decoder[A] =
    new Decoder[A] {
      def decode(expr: Expr): Result[A] =
        decoder.value.decode(expr).map(gen.from)
      def isValidType(typeExpr: Expr): Boolean = true
      def isExactType(typeExpr: Expr): Boolean = true
    }

  implicit def decodeCoproduct[K <: Symbol, V, T <: Coproduct](implicit
      vDecoder: Lazy[Decoder[V]],
      tailDecoder: Decoder[T],
      kWitness: Witness.Aux[K]
  ): Decoder[FieldType[K, V] :+: T] =
    new Decoder[FieldType[K, V] :+: T] {
      val key = kWitness.value.name
      def decode(expr: Expr): Result[FieldType[K, V] :+: T] = {
        val decoderEitherLeftOrRight = (arg: Expr, typ: String) =>
          if (key == typ) {
            vDecoder.value.decode(arg).map { hValue => Inl(field[K](hValue)) }
          } else {
            tailDecoder.decode(expr).map { Inr(_) }
          }
        expr.normalize match {
          case Application(FieldAccess(UnionType(_), typ), arg) =>
            decoderEitherLeftOrRight(arg, typ)
          case FieldAccess(UnionType(_), typ) =>
            decoderEitherLeftOrRight(expr, typ)
          case other =>
            Left(new DecodingFailure(s"expr $other is not a union", other))
        }
      }

      def isValidType(typeExpr: Expr): Boolean = true
      def isExactType(typeExpr: Expr): Boolean = true
    }
  implicit def decodeProduct[K <: Symbol, V, T <: HList](implicit
      vDecoder: Lazy[Decoder[V]],
      tailDecoder: Decoder[T],
      kWitness: Witness.Aux[K]
  ): Decoder[FieldType[K, V] :: T] =
    new Decoder[FieldType[K, V] :: T] {
      val key = kWitness.value.name
      def decode(expr: Expr): Result[FieldType[K, V] :: T] =
        expr.normalize match {
          case RecordLiteral(fields) =>
            for {
              valueExpr <-
                fields
                  .get(key)
                  .toRight(
                    new DecodingFailure(s"missing key ${key} in record", expr)
                  )
              hValue <- vDecoder.value.decode(valueExpr)
              tail <- tailDecoder.decode(expr)
            } yield field[K](hValue) :: tail
          case other =>
            Left(new DecodingFailure("expr is not a record", other))
        }
      def isValidType(typeExpr: Expr): Boolean = true
      def isExactType(typeExpr: Expr): Boolean = true
    }
}
