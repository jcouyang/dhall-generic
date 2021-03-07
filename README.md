# Dhall Scala Generic
Load Dhall config directly into Scala case class

![Build and Test](https://github.com/jcouyang/dhall-scala-generic/workflows/Build%20and%20Test/badge.svg)
[![](https://index.scala-lang.org/jcouyang/dhall-generic/latest.svg?v=1)](https://index.scala-lang.org/jcouyang/dhall-generic)

```
libraryDependencies += "us.oyanglul" %% "dhall-generic" % "0.3.+"
```

There is a dhall config
```dhall
let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}>
in Shape.Circle {radius = 1.2}
```

And you can parse them into dhall `Expr`
```scala
import org.dhallj.syntax._

val expr = """
let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}>
in Shape.Circle {radius = 1.2}
""".parseExpr
```
### Scala 3
Supposed that you have some Scala case classes
```scala
enum Shape:
  case Rectangle(width: Double, height: Double)
  case Circle(radius: Double)
```

to load a dhall expr into Scala case classes, simply just
1. `derives Decoder`
```scala
enum Shape derives Decoder:
  case Rectangle(width: Double, height: Double)
  case Circle(radius: Double)
```

2. `as[Shape]`
```scala
import us.oyanglul.dhall.generic._

expr.normalize.as[Shape]
// => Right(Circle(1.2)): Either[DecodingFailure, Shape]
```

### Scala 2
Supposed that you have some Scala case classes
```scala
sealed trait Shape
object Shape {
  case class Rectangle(width: Double, height: Double) extends Shape
  case class Circle(radius: Double) extends Shape
}
```

to load a dhall expr into Scala case classes, simply just

```scala
import us.oyanglul.dhall.generic._

expr.normalize.as[Shape]
// => Right(Circle(1.2)): Either[DecodingFailure, Shape]
```

## Use cases
### Load application config
It is very simple to replace [HOCON `application.conf`](https://github.com/lightbend/config) with dhall.

To load `src/main/resources/application.dhall` from classpath your will need [dhall-imports](https://github.com/travisbrown/dhallj#dhall-imports)

```scala
import org.dhallj.syntax._
import org.dhallj.codec.syntax._
import us.oyanglul.dhall.generic._
import org.dhallj.imports.syntax._

  BlazeClientBuilder[IO](ExecutionContext.global).resource.use { implicit c =>
        IO.fromEither("classpath:/application.dhall".parseExpr)
          .flatMap(_.resolveImports[IO])
          .flatMap{expr => IO.fromEither(expr.normalize.as[Config])}
    }
```

### Load dhall to sbt libraryDependencies
This project itself is an example of how to load dhall into build.sbt

It is recursively using [previous version](./project/build.sbt#L4) [to load](./project/loadDhall.scala) [./build.dhall](./build.dhall) to libraryDependencies

```scala
libraryDependencies ++= dhall.load.modules.map{case Module(o, n, v) => o %% n % v},
```

## Custom Decoder
Create new decoder from existing decoder

i.e. UUID
### Scala 3
```scala
given (using d: Decoder[String]): Decoder[UUID] = d.map(UUID.fromString)
```

### Scala 2
```scala
implicit val decodeUUID: Decoder[UUID] = decodeString.map(UUID.from)
```
