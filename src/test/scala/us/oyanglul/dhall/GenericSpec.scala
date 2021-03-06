package us.oyanglul.dhall

import generic._
import munit._
import org.dhallj.syntax._
import org.dhallj.codec.syntax._
import org.dhallj.codec.Decoder._
import java.util.UUID
import Shape._
import Env._
class GenericSpec extends FunSuite {

  test("generic decode product") {
    val Right(expr) = """{width = 1, height = 1.1}""".parseExpr
    val Right(decoded: Rectangle) = expr.as[Rectangle]
    assertEquals(Rectangle(1, 1.1), decoded)
  }

  test("generic decode empty product") {
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
    import OuterClass._
    val Right(expr) =
      """|
         |let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}>
         |in {name = "Outer Class", shape = Shape.Circle {radius = 1.2}, uuid = "644BA20E-9C09-4C70-BDEB-8998ED92157B"}
         |""".stripMargin.parseExpr
    val Right(decoded) = expr.normalize.as[OuterClass]
    assertEquals(decoded, OuterClass("Outer Class", Circle(1.2), UUID.fromString("644BA20E-9C09-4C70-BDEB-8998ED92157B")))
  }

  test("exception") {
    val Right(expr) = "{width = \"asdf\", height = 1.1}".parseExpr
    val Left(error) = expr.normalize.as[Circle]
    assertEquals(
      error.getMessage,
      "Error decoding missing key radius in record"
    )
  }

  test("case object") {
    val Right(expr) = """{env = <Local | Sit | Prod>.Sit}""".parseExpr
    val Right(decoded) = expr.normalize().as[Config]
    assertEquals(decoded, Config(Sit))
  }

  test("empty case class") {
    val Right(expr) = """<Local | Sit | Prod>.Sit""".parseExpr
    val Right(decoded) = expr.normalize().as[Env2]
    assertEquals(decoded, Env2.Sit())
  }

  test("list of Shape") {
    val Right(expr) =
      """let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}> in {shapes = [Shape.Circle {radius = 1.1}]}""".parseExpr
    val Right(decoded) = expr.normalize().as[Shapes]

    assertEquals(decoded, Shapes(List(Circle(1.1))))
  }
}
