package us.oyanglul.dhall

import org.dhallj.codec.Decoder
import org.dhallj.codec.Decoder._
import generic._

import scala.deriving.*
import scala.compiletime.{erasedValue, summonInline}

// given Decoder[Shape.Rectangle] = Decoder.derived[Shape.Rectangle]
inline def summonAll[T <: Tuple]: List[Eq[_]] =
   inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) => summonInline[Eq[t]] :: summonAll[ts]

trait Eq[T]:
   def eqv(x: T, y: T): Boolean

object Eq:
   given Eq[Int] with
      def eqv(x: Int, y: Int) = x == y

   def check(elem: Eq[_])(x: Any, y: Any): Boolean =
      elem.asInstanceOf[Eq[Any]].eqv(x, y)

   def iterator[T](p: T) = p.asInstanceOf[Product].productIterator

   def eqSum[T](s: Mirror.SumOf[T], elems: => List[Eq[_]]): Eq[T] =
      new Eq[T]:
         def eqv(x: T, y: T): Boolean =
            val ordx = s.ordinal(x)
            (s.ordinal(y) == ordx) && check(elems(ordx))(x, y)

   def eqProduct[T](p: Mirror.ProductOf[T], elems: => List[Eq[_]]): Eq[T] =
      new Eq[T]:
         def eqv(x: T, y: T): Boolean =
            iterator(x).zip(iterator(y)).zip(elems.iterator).forall {
               case ((x, y), elem) => check(elem)(x, y)
            }

   inline given derived[T](using m: Mirror.Of[T]): Eq[T] =
      lazy val elemInstances = summonAll[m.MirroredElemTypes]
      inline m match
         case s: Mirror.SumOf[T]     => eqSum(s, elemInstances)
         case p: Mirror.ProductOf[T] => eqProduct(p, elemInstances)
end Eq

enum Opt[+T] derives Eq:
   case Sm(t: T)
   case Nn

enum Shape:
  case Rectangle(width: Double, height: Double)
  case Circle(radius: Double)


// given Decoder[Shape.Circle] = Decoder.derived[Shape.Circle]
// given Decoder[Shape] = Decoder.derived[Shape]

case class OuterClass(name: String, shape: Shape)

case class Empty() derives Decoder

enum Env derives Eq:
  case Local()
  case Sit()
  case Prod()

case class Config(env: Env)

case class Shapes(shapes: List[Shape])
