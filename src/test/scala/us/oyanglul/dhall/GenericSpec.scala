package us.oyanglul.dhall

import generic._
import munit._
import org.dhallj.syntax._
import org.dhallj.codec.syntax._
import org.dhallj.codec.Decoder._

import java.util.UUID
import Shape._
import Env._
import org.dhallj.core.DhallException
import scala.reflect.ClassTag

class GenericSpec extends FunSuite {


  test("generic decode product") {
    val expr = """{width = 1, height = 1.1}""".parseExpr.asRight
    val decoded = expr.as[Rectangle].asRight
    assertEquals(Rectangle(1, 1.1), decoded)
  }

  test("generic decode empty product") {
    val expr = """{=}""".parseExpr.asRight
    val decoded = expr.as[Empty].asRight
    assertEquals(decoded, Empty())
  }
  test("generic decode coproduct") {
    val expr1 =
      """let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}> in Shape.Circle {radius = 1.1}""".parseExpr
    val expr2 =
      """let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}> in Shape.Rectangle {width = 1.1, height = 1.2}""".parseExpr
    val decoded1 = expr1.flatMap(_.normalize().as[Shape]).asRight
    val decoded2 = expr2.flatMap(_.normalize().as[Shape]).asRight
    assertEquals(decoded1, Circle(1.1))
    assertEquals(decoded2, Rectangle(1.1, 1.2))
  }

  test("nested coproduct and product") {
    import OuterClass._
    val expr =
      """|
         |let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}>
         |in {name = "Outer Class", shape = Shape.Circle {radius = 1.2}, uuid = "644BA20E-9C09-4C70-BDEB-8998ED92157B"}
         |""".stripMargin.parseExpr.asRight
    val decoded = expr.normalize.as[OuterClass].asRight
    assertEquals(
      decoded,
      OuterClass(
        "Outer Class",
        Circle(1.2),
        UUID.fromString("644BA20E-9C09-4C70-BDEB-8998ED92157B")
      )
    )
  }

  test("exception") {
    val expr = "{width = \"asdf\", height = 1.1}".parseExpr.asRight
    val error = expr.normalize.as[Circle].asLeft
    assertEquals(
      error.getMessage,
      "Error decoding missing key radius in record"
    )
  }

  test("case object") {
    val expr = """{env = <Local | Sit | Prod>.Sit}""".parseExpr.asRight
    val decoded = expr.normalize().as[Config].asRight
    assertEquals(decoded, Config(Sit))
  }

  test("empty case class") {
    val expr = """<Local | Sit | Prod>.Sit""".parseExpr.asRight
    val decoded = expr.normalize().as[Env2].asRight
    assertEquals(decoded, Env2.Sit())
  }

  test("list of Shape") {
    val expr =
      """let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}> in {shapes = [Shape.Circle {radius = 1.1}]}""".parseExpr.asRight
    val decoded = expr.normalize().as[Shapes].asRight

    assertEquals(decoded, Shapes(List(Circle(1.1))))
  }


  private implicit class X[A](either: Either[DhallException, A])(implicit classTag: ClassTag[A]) {
    def asRight: A = either match {
      case Left(err) => fail(s"expected Right[${classTag.runtimeClass.getName}] but was Left($err)")
      case Right(value) => value
    }

    def asLeft: DhallException = either match {
      case Left(value) => value
      case Right(value) => fail(s"expected Left[DhallException] but was Right($value)")
    }
  }
}
