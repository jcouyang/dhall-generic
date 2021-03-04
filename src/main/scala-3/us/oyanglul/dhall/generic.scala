package us.oyanglul.dhall

import org.dhallj.ast.{Application, FieldAccess, RecordLiteral, UnionType}
import org.dhallj.codec.Decoder.Result
import org.dhallj.codec.{Decoder, DecodingFailure}
import org.dhallj.core.Expr
import scala.deriving._
import scala.compiletime.{erasedValue, summonInline,constValueTuple}
import org.dhallj.codec.Decoder._

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

  inline def fieldNames(p: Mirror) =
    constValueTuple[p.MirroredElemLabels].productIterator

  extension (d: Decoder.type)
    inline def derived[T](using m: Mirror.Of[T]) = _derived[T]
  inline given _derived[T](using m: Mirror.Of[T]): Decoder[T] =
      lazy val allDecoders = summonAll[m.MirroredElemTypes]
      lazy val names = fieldNames(m).asInstanceOf[Iterator[String]].toSeq
      inline m match
        case s: Mirror.SumOf[T] => 
          decodeCoproduct(s, allDecoders, names)
        case p: Mirror.ProductOf[T] => decodeProduct(p,allDecoders, names)

  def decodeProduct[T](p: Mirror.ProductOf[T], elems: => List[Decoder[_]], names: => Seq[String]): Decoder[T] =
    new Decoder[T] {
      def decode(expr: Expr): Result[T] =
         expr.normalize match {
           case RecordLiteral(fields) =>
             val product = names.zip(elems.iterator.asInstanceOf[Iterator[Decoder[_]]]).foldRight(Right(EmptyTuple)){( n: (String, Decoder[_]), acc: Result[Tuple]) =>
               val (name, decoder) = n
               for {
                 a <- acc
                 valueExpr <- fields
                  .get(name)
                  .toRight(
                    new DecodingFailure(s"missing key ${name} in record", expr)
                  )
                 value <- decoder.decode(valueExpr)
               } yield value *: a
             }
             product.map(p.fromProduct(_))
          case other =>
            Left(new DecodingFailure("expr is not a record", other)) 
        }
      def isValidType(typeExpr: Expr): Boolean = true
      def isExactType(typeExpr: Expr): Boolean = true
    }
  def decodeCoproduct[T](s: Mirror.SumOf[T], elems: => List[Decoder[_]], names: => Seq[String]): Decoder[T] =
    new Decoder[T] {
      def decode(expr: Expr): Result[T] = {
        val decoderEitherLeftOrRight = (arg: Expr, typ: String) =>
          elems(names.indexOf(typ)).decode(expr).asInstanceOf[Result[T]]
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
}
