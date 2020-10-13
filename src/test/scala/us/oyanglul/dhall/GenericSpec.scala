package us.oyanglul.dhall

import generic._
import munit._
import org.dhallj.syntax._
import org.dhallj.codec.syntax._

class GenericSpec extends FunSuite {
  sealed trait Shape
  case class Rectangle(width: Double, height: Double) extends Shape
  case class Circle(radius: Double) extends Shape

  test("generic decode product") {
    val Right(expr) = """{width = 1, height = 1.1}""".parseExpr
    val Right(decoded) = expr.as[Rectangle]
    assertEquals(decoded, Rectangle(1, 1.1))
  }

  test("generic decode empty product") {
    case class Empty()
    val Right(expr) = """{=}""".parseExpr
    val Right(decoded) = expr.as[Empty]
    assertEquals(decoded, Empty())
  }
  test("generic decode coproduct") {
    val Right(expr1) =
      """let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}> in Shape.Circle {radius = 1.1}""".parseExpr
    val Right(expr2) =
      """let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}> in Shape.Rectangle {width = 1.1, height = 1.2}""".parseExpr
    val Right(decoded1) = expr1.normalize().as[Shape]
    val Right(decoded2) = expr2.normalize().as[Shape]
    assertEquals(decoded1, Circle(1.1))
    assertEquals(decoded2, Rectangle(1.1, 1.2))
  }

  test("nested coproduct and product") {
    case class OuterClass(name: String, shape: Shape)
    val Right(expr) =
      """|
                         |let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}>
                         |in {name = "Outer Class", shape = Shape.Circle {radius = 1.2}}
                         |""".stripMargin.parseExpr
    val Right(decoded) = expr.normalize.as[OuterClass]
    assertEquals(decoded, OuterClass("Outer Class", Circle(1.2)))
  }

  test("exception") {
    val Right(expr) = "{width = \"asdf\", height = 1.1}".parseExpr
    val Left(error) = expr.normalize.as[Circle]
    assertEquals(error.getMessage, "Error decoding missing key radius in record")

  }
}
