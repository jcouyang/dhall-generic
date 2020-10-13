package example
import munit._
import shapeless._
import shapeless.labelled.FieldType
import shapeless.labelled.field
import org.dhallj.codec._
import org.dhallj.core.Expr
import org.dhallj.ast._
import org.dhallj.codec.Decoder._
import cats.implicits._
import shapeless.tag.Tagged
import org.dhallj.syntax._

class MySuite extends FunSuite {

  implicit val decodeHNil: Decoder[HNil] =  new Decoder[HNil] {
    def decode(expr: Expr): Result[HNil] = HNil.asRight

    def isValidType(typeExpr: Expr): Boolean = typeExpr match {
      // case Application(Expr.Constants.LIST, elementType) => true
      case _                                             => true
    }

    def isExactType(typeExpr: Expr): Boolean = typeExpr match {
      // case Application(Expr.Constants.LIST, elementType) => true
      case _                                             => true
    }
  }
  implicit def decodeProductGeneric[A, ARepr](implicit
    gen: LabelledGeneric.Aux[A, ARepr],
    decoder: Decoder[ARepr]
  ): Decoder[A] =  new Decoder[A] {
    def decode(expr: Expr): Result[A] = decoder.decode(expr).map(gen.from(_))

    def isValidType(typeExpr: Expr): Boolean = typeExpr match {
      case a                                             =>
        println(s"===isValidType $typeExpr, $a")
        true
    }

    def isExactType(typeExpr: Expr): Boolean = typeExpr match {
       case a                                             =>
        println(s"====isValidType $typeExpr, $a")
        true
    }
  }

  implicit def decodeProduct[K <: Symbol, V, T <: HList](implicit
    vDecoder: Lazy[Decoder[V]],
    tailDecoder: Decoder[T],
    kWitness: Witness.Aux[K],
  ): Decoder[FieldType[K, V] :: T] = new Decoder[FieldType[K, V] :: T] {
    val key = kWitness.value.name
    def decode(expr: Expr): Result[FieldType[K, V] :: T] = expr.normalize match {
      case RecordLiteral(fields) =>
        println(s"---record litral $fields")
        for {
        valueExpr <- fields.get(key).toRight(new DecodingFailure(s"missing key ${key}", null))
        hValue <- vDecoder.value.decode(valueExpr)
        tail <- tailDecoder.decode(expr)
      } yield field[K](hValue) :: tail
      case other =>
        println("------------")
        println(other)
        Left(new DecodingFailure("Record", other))
    }

    def isValidType(typeExpr: Expr): Boolean = typeExpr match {
      case a                                             =>
        println(s"---isValidType $typeExpr, $a")
        true
    }

    def isExactType(typeExpr: Expr): Boolean = typeExpr match {
      // case Application(Expr.Constants.LIST, elementType) => true
      case a                                             =>
        println(s"---isExactType $typeExpr, $a")
        true
    }
  }
  test("generic decode product") {
    case class AProduct(field1: String, field2: Int)
    val gen = LabelledGeneric[AProduct].to(AProduct("Sundae", 1))
    val decoder = implicitly[Decoder[AProduct]]
    val Right(expr) = """{field1 = "Sundae", field2 = 1}""".parseExpr
    val Right(decoded) = decoder.decode(expr)
    assertEquals(decoded, AProduct("Sundae", 1))
  }
}
