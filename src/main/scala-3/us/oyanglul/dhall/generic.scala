package us.oyanglul.dhall

import org.dhallj.ast.{Application, FieldAccess, RecordLiteral, UnionType}
import org.dhallj.codec.Decoder.Result
import org.dhallj.codec.{Decoder, DecodingFailure}
import org.dhallj.core.Expr
import scala.deriving._
import scala.compiletime.{erasedValue, summonInline,constValueTuple}

object generic {
  private inline def summonAll[T <: Tuple]: List[Decoder[_]] =
   inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) => summonInline[Decoder[t]] :: summonAll[ts]

  given decodeHNil: Decoder[EmptyTuple] with {
    def decode(expr: Expr): Result[EmptyTuple] = Right(EmptyTuple)
    // visitor pattern is not use in scala
    def isValidType(typeExpr: Expr): Boolean = true
    def isExactType(typeExpr: Expr): Boolean = true
  }

  inline def fieldNames[T](p: Mirror.ProductOf[T]) =
    constValueTuple[p.MirroredElemLabels].productIterator

  inline given derived[T](using m: Mirror.Of[T]): Decoder[T] =
    inline m match
      case s: Mirror.SumOf[T]     => ???
      case p: Mirror.ProductOf[T] => ???

  def decodeProduct[T](p: Mirror.ProductOf[T], elems: => List[Decoder[_]]): Decoder[T] =
    new Decoder[T] {
      val names = fieldNames(p).asInstanceOf[Iterator[String]]
      def decode(expr: Expr): Result[T] =
        names.zip(elems.iterator.asInstanceOf[Iterator[Decoder[_]]]).foldLeft(EmptyTuple){(acc: Tuple, n: (String, Decoder[_])) =>
          val (name, decoder) = n
          expr.normalize match {
            case RecordLiteral(fields) =>
              for {
                valueExpr <- fields
                  .get(name)
                  .toRight(
                    new DecodingFailure(s"missing key ${name} in record", expr)
                  )
                value <- decoder.decode(valueExpr)
            } yield field[K](hValue) :: tail
          case other =>
            Left(new DecodingFailure("expr is not a record", other))
        } 
        }
      def isValidType(typeExpr: Expr): Boolean = true
      def isExactType(typeExpr: Expr): Boolean = true
    }
  // implicit val decodeCNil: Decoder[CNil] = new Decoder[CNil] {
  //   def decode(expr: Expr): Result[CNil] =
  //     Left(new DecodingFailure("Inconceivable! Coproduct never be CNil", expr))
  //   def isValidType(typeExpr: Expr): Boolean = true
  //   def isExactType(typeExpr: Expr): Boolean = true
  // }

  // implicit def decodeGeneric[A, ARepr](implicit
  //     gen: LabelledGeneric.Aux[A, ARepr],
  //     decoder: Lazy[Decoder[ARepr]]
  // ): Decoder[A] =
  //   new Decoder[A] {
  //     def decode(expr: Expr): Result[A] =
  //       decoder.value.decode(expr).map(gen.from)
  //     def isValidType(typeExpr: Expr): Boolean = true
  //     def isExactType(typeExpr: Expr): Boolean = true
  //   }

  // implicit def decodeCoproduct[K <: Symbol, V, T <: Coproduct](implicit
  //     vDecoder: Lazy[Decoder[V]],
  //     tailDecoder: Decoder[T],
  //     kWitness: Witness.Aux[K]
  // ): Decoder[FieldType[K, V] :+: T] =
  //   new Decoder[FieldType[K, V] :+: T] {
  //     val key = kWitness.value.name
  //     def decode(expr: Expr): Result[FieldType[K, V] :+: T] = {
  //       val decoderEitherLeftOrRight = (arg: Expr, typ: String) =>
  //         if (key == typ) {
  //           vDecoder.value.decode(arg).map { hValue => Inl(field[K](hValue)) }
  //         } else {
  //           tailDecoder.decode(expr).map { Inr(_) }
  //         }
  //       expr.normalize match {
  //         case Application(FieldAccess(UnionType(_), typ), arg) =>
  //           decoderEitherLeftOrRight(arg, typ)
  //         case FieldAccess(UnionType(_), typ) =>
  //           decoderEitherLeftOrRight(expr, typ)
  //         case other =>
  //           Left(new DecodingFailure(s"expr $other is not a union", other))
  //       }
  //     }

  //     def isValidType(typeExpr: Expr): Boolean = true
  //     def isExactType(typeExpr: Expr): Boolean = true
  //   }
}
