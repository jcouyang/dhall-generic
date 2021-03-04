package us.oyanglul.dhall

sealed trait Shape
object Shape {
  case class Rectangle(width: Double, height: Double) extends Shape
  case class Circle(radius: Double) extends Shape
}
case class OuterClass(name: String, shape: Shape)
case class Empty()

sealed trait Env
object Env {
  case class Local() extends Env
  case class Sit() extends Env
  case class Prod() extends Env
}
case class Config(env: Env)

case class Shapes(shapes: List[Shape])
