package us.oyanglul.dhall

import org.dhallj.codec.Decoder._
import generic.{given,
_
}
enum Shape derives Decoder:
  case Rectangle(width: Double, height: Double)
  case Circle(radius: Double)

case class OuterClass(name: String, shape: Shape) derives Decoder

case class Empty() derives Decoder

enum Env derives Decoder:
  case Local,Sit,Prod

enum Env2 derives Decoder:
  case Local()
  case Sit()
  case Prod()
case class Config(env: Env) derives Decoder

case class Shapes(shapes: List[Shape]) derives Decoder
