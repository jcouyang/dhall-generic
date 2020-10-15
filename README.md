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
