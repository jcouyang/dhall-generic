import us.oyanglul.dhall.generic._
import org.dhallj.syntax._
import org.dhallj.codec.syntax._
import org.dhallj.codec.Decoder._

case class Module(
    org: String,
    name: String,
    version: String
)

case class Build(modules: List[Module])

object dhall {
  def load = {
    val Right(expr) = "./build.dhall".parseExpr.flatMap(_.resolve)
    val Right(decoded) = expr.normalize().as[Build]
    decoded
  }
}
