package us.oyanglul.dhall

import generic._
import java.util.UUID

enum Shape derives Decoder:
  case Rectangle(width: Double, height: Double)
  case Circle(radius: Double)

given (using d: Decoder[String]): Decoder[UUID] = d.map(UUID.fromString)

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
