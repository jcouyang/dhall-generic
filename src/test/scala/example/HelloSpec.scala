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
  sealed trait Shape
  case class Rectangle(width: Double, height: Double) extends Shape
  case class Circle(radius: Double) extends Shape

  implicit val decodeHNil: Decoder[HNil] =  new Decoder[HNil] {
    def decode(expr: Expr): Result[HNil] = HNil.asRight

    def isValidType(typeExpr: Expr): Boolean = true

    def isExactType(typeExpr: Expr): Boolean = true
  }

    implicit val decodeCNil: Decoder[CNil] =  new Decoder[CNil] {
    def decode(expr: Expr): Result[CNil] = Left(new DecodingFailure("Inconceivable!", expr))

    def isValidType(typeExpr: Expr): Boolean = true

    def isExactType(typeExpr: Expr): Boolean = true
  }
  implicit def decodeGeneric[A, ARepr](implicit
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

  implicit def decodeCoproduct [K <: Symbol, V, T <: Coproduct](implicit
    vDecoder: Lazy[Decoder[V]],
    tailDecoder: Decoder[T],
    kWitness: Witness.Aux[K],
  ): Decoder[FieldType[K, V] :+: T] = new Decoder[FieldType[K, V] :+: T] {
    val key = kWitness.value.name
    def decode(expr: Expr): Result[FieldType[K, V] :+: T] = expr.normalize match {
      case RecordLiteral(fields) =>
        println("0---00-")
        println(s"""---union litral $fields, ${fields.get("Circle")}, ${fields.get("Rectangle")}""")
        for {
        valueExpr <- fields.get(key).toRight(new DecodingFailure(s"missing key ${key}", expr))
        hValue <- vDecoder.value.decode(valueExpr)
        tail <- tailDecoder.decode(expr)
        } yield Inl(field[K](hValue))
      case Application(FieldAccess(UnionType(unionType), typ), arg) =>
        println(unionType)
        println(typ)
        println(arg)
        if(key == typ) {
          println("inleft")
          vDecoder.value.decode(arg).map{hValue => Inl(field[K](hValue))}
        }else {
          println("inright")
          tailDecoder.decode(expr).map{Inr(_)}
        }
      case other =>
        println("------------")
        println(other)
        Left(new DecodingFailure("not a union", other))
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
        Left(new DecodingFailure("not a record", other))
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
    val decoder = implicitly[Decoder[Rectangle]]
    val Right(expr) = """{width = 1, height = 1.1}""".parseExpr
    val Right(decoded) = decoder.decode(expr)
    assertEquals(decoded, Rectangle(1, 1.1))
  }

  test("generic decode coproduct") {
    val decoder = implicitly[Decoder[Shape]]
    val Right(expr1) = """let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}> in Shape.Circle {radius = 1.1}""".parseExpr
    val Right(expr2) = """let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}> in Shape.Rectangle {width = 1.1, height = 1.2}""".parseExpr
    val Right(decoded1) = decoder.decode(expr1.normalize())
    val Right(decoded2) = decoder.decode(expr2.normalize())
    assertEquals(decoded1, Circle(1.1))
    assertEquals(decoded2, Rectangle(1.1,1.2))
  }

  test("nested coproduct and product") {
    case class OuterClass(name: String, shape: Shape)
    val Right(expr) = """|
                         |let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}>
                         |in {name = "Outer Class", shape = Shape.Circle {radius = 1.2}}
                         |""".stripMargin.parseExpr
    val decoder = implicitly[Decoder[OuterClass]]
    val Right(decoded) = decoder.decode(expr.normalize)
    assertEquals(decoded, OuterClass("Outer Class", Circle(1.2)))
  }
}
