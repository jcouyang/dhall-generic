package example
import munit._
import shapeless._
import shapeless.labelled.FieldType
import shapeless.labelled.field

class MySuite extends FunSuite {
  import org.dhallj.codec._
  import org.dhallj.core.Expr
  import org.dhallj.ast._
  import org.dhallj.codec.Decoder._

  implicit def decodeProduct[K <: Symbol, V, T <: HList](implicit
    vDecoder: Lazy[Decoder[V]],
    tailDecoder: Decoder[T],
    kWitness: Witness.Aux[K],
  ): Decoder[FieldType[K, V] :: T] = new Decoder[FieldType[K, V] :: T] {
    val key = kWitness.value.name
    def decode(expr: Expr): Result[FieldType[K, V] :: T] = expr.normalize match {
      case RecordLiteral(fields) => for {
        expr <- fields.get(key).toRight(new DecodingFailure(s"missing key ${key}", null))
        hValue <- vDecoder.value.decode(expr)
        tail <- tailDecoder.decode(expr)
      } yield field[K](hValue) :: tail
      case other => Left(new DecodingFailure("Record", other))
    }

    def isValidType(typeExpr: Expr): Boolean = typeExpr match {
      case Application(Expr.Constants.LIST, elementType) => true
      case _                                             => false
    }

    def isExactType(typeExpr: Expr): Boolean = typeExpr match {
      case Application(Expr.Constants.LIST, elementType) => true
      case _                                             => false
    }
  }
  test("generic decode product") {
    case class AProduct(field1: String, field2: Int, field3: Long)

    val obtained = 42
    val expected = 43
    assertEquals(obtained, expected)
  }
}
