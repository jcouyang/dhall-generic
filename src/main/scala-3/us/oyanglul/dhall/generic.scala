package us.oyanglul.dhall

import org.dhallj.ast.{Application, FieldAccess, RecordLiteral, UnionType}
import org.dhallj.codec.Decoder.Result
import org.dhallj.codec.{Decoder => BasicDecoder, DecodingFailure, Encoder}
import org.dhallj.core.Expr
import scala.deriving._
import scala.compiletime.{erasedValue, summonInline,constValueTuple}
import org.dhallj.codec.Decoder._

object generic {
  sealed trait Decoder[T] {
    def decode(expr: Expr): Result[T]
  }

  sealed trait Functor[F[_]]:
    def fmap[A, B](f: A=> B): F[A] => F[B]
    extension [A](fa: F[A])
      def map[B](f: A => B) = fmap[A, B](f)(fa)

  object Decoder {
    given Functor[Decoder] with {
      def fmap[A, B](f: A => B) = (da) => new Decoder[B] {
        def decode(expr: Expr) = da.decode(expr).map(f)
      }
    }
    given Decoder[Double] with {
      def decode(expr: Expr) = decodeDouble.decode(expr)
    }
    given Decoder[String] with {
      def decode(expr: Expr) = decodeString.decode(expr)
    }
    given Decoder[Long] with {
      def decode(expr: Expr) = decodeLong.decode(expr)
    }
    given Decoder[Int] with {
      def decode(expr: Expr) = decodeInt.decode(expr)
    }
    given Decoder[BigInt] with {
      def decode(expr: Expr) = decodeBigInt.decode(expr)
    }
    given Decoder[Boolean] with {
      def decode(expr: Expr) = decodeBoolean.decode(expr)
    }
    given [A:Decoder]: Decoder[List[A]] with {
      def decode(expr: Expr) = decodeList[A].decode(expr)
    }
    given [A:Decoder]: Decoder[Vector[A]] with {
      def decode(expr: Expr) = decodeVector[A].decode(expr)
    }
    given [A:Decoder]: Decoder[Option[A]] with {
      def decode(expr: Expr) = decodeOption[A].decode(expr)
    }
    given [A:Encoder,B:Decoder]: Decoder[Function1[A,B]] with {
      def decode(expr: Expr) = decodeFunction1[A,B].decode(expr)
    }

    inline given summonEmptyTuple[H]: Tuple.Map[EmptyTuple.type, Decoder] =
      EmptyTuple

    inline given summonTuple[H, T <: Tuple](using hd: Decoder[H], td: Tuple.Map[T, Decoder]): Tuple.Map[H *: T, Decoder] =
      hd *: td
  
    inline def fieldNames(p: Mirror) =
      constValueTuple[p.MirroredElemLabels].productIterator
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
            case FieldAccess(UnionType(_), typ) =>
              Right(p.fromProduct(EmptyTuple))
            case other =>
              Left(new DecodingFailure(s"expr is not a record ${other}", other)) 
          }
      }
    def decodeCoproduct[T](s: Mirror.SumOf[T], elems: => List[Decoder[_]], names: => Seq[String]): Decoder[T] =
        new Decoder[T] {
        def decode(expr: Expr): Result[T] = {
          val decoderEitherLeftOrRight = (arg: Expr, typ: String) =>
            elems(names.indexOf(typ)).decode(arg).asInstanceOf[Result[T]]
          expr.normalize match {
            case Application(FieldAccess(UnionType(_), typ), arg) =>
              decoderEitherLeftOrRight(arg, typ)
            case FieldAccess(UnionType(_), typ) =>
              decoderEitherLeftOrRight(expr, typ)
            case other =>
              Left(new DecodingFailure(s"expr $other is not a union", other))
          }
        }
  
      }  
    inline given derived[T](using m: Mirror.Of[T], d: Tuple.Map[m.MirroredElemTypes, Decoder]): Decoder[T] =
      lazy val allDecoders = d.toList.asInstanceOf[List[Decoder[_]]]
      lazy val names = fieldNames(m).asInstanceOf[Iterator[String]].toSeq
      inline m match
        case s: Mirror.SumOf[T] => 
          decodeCoproduct(s, allDecoders, names)
        case p: Mirror.ProductOf[T] =>
          decodeProduct(p,allDecoders, names)
  }
  implicit def dhalljDecoder[T](using d: Decoder[T]): BasicDecoder[T] = new BasicDecoder[T] {
      def decode(expr: Expr): Result[T] = d.decode(expr)
      def isValidType(typeExpr: Expr): Boolean = true
      def isExactType(typeExpr: Expr): Boolean = true
  }

  extension (expr: Expr)
    def as[A](using d: Decoder[A]) = d.decode(expr)
}
