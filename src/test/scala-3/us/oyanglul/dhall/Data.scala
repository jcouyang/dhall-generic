package us.oyanglul.dhall

import org.dhallj.codec.Decoder._
import org.dhallj.core.Expr
import generic._
import java.util.UUID
import generic.Decoder.{given}

enum Shape derives Decoder:
  case Rectangle(width: Double, height: Double)
  case Circle(radius: Double)

given Decoder[UUID] with {
      def decode(expr: Expr) = decodeString.map(UUID.fromString).decode(expr)
    }
case class OuterClass(name: String, shape: Shape, uuid: UUID) derives Decoder

case class Empty() derives Decoder

enum Env derives Decoder:
  case Local,Sit,Prod

enum Env2 derives Decoder:
  case Local()
  case Sit()
  case Prod()
case class Config(env: Env) derives Decoder

case class Shapes(shapes: List[Shape]) derives Decoder
