# Dhall Scala Generic
Load Dhall config directly into Scala case class

![Build and Test](https://github.com/jcouyang/dhall-scala-generic/workflows/Build%20and%20Test/badge.svg)
[![](https://index.scala-lang.org/jcouyang/dhall-generic/latest.svg?v=1)](https://index.scala-lang.org/jcouyang/dhall-generic)

```
libraryDependencies += "us.oyanglul" %% "dhall-generic" % "0.2.0"
```

Supposed that you have some Scala case classes
```scala
sealed trait Shape
case class Rectangle(width: Double, height: Double) extends Shape
case class Circle(radius: Double) extends Shape
```

and a dhall config
```dhall
let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}>
in Shape.Circle {radius = 1.2}
```

to load a dhall configuration into Scala case classes, simply just

```scala
import org.dhallj.syntax._
import org.dhallj.codec.syntax._
import us.oyanglul.dhall.generic._

val expr = """
let Shape = <Rectangle: {width: Double, height: Double}| Circle: {radius: Double}>
in Shape.Circle {radius = 1.2}
""".parseExpr

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
```scala
implicit val decodeUUID: Decoder[UUID] = decodeString.map(UUID.from)
```
