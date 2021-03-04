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
  case object Local extends Env
  case object Sit extends Env
  case object Prod extends Env
}

sealed trait Env2
object Env2 {
  case class Local() extends Env2
  case class Sit() extends Env2
  case class Prod() extends Env2
}

case class Config(env: Env)

case class Shapes(shapes: List[Shape])
