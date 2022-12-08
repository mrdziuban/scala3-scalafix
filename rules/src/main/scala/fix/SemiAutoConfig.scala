package fix

import fix.SemiAutoConfig.Rewrite
import metaconfig.{Conf, ConfDecoder, ConfError, Configured}
import metaconfig.generic.Surface

case class SemiAutoConfig(
    bundles: List[String] = List("all"),
    rewrites: List[Rewrite] = List.empty
) {
  def allRewrites: Set[SemiAutoConfig.Rewrite] = {
    val fromBundle =
      if (bundles == List("all")) SemiAutoConfig.rewriteBundles.values.flatten.toSet
      else bundles.flatMap(name => SemiAutoConfig.rewriteBundles.get(name)).flatten.toSet
    fromBundle ++ rewrites.toSet
  }
}

object SemiAutoConfig {
  def default: SemiAutoConfig = SemiAutoConfig()
  implicit val surface: Surface[SemiAutoConfig] =
    metaconfig.generic.deriveSurface[SemiAutoConfig]
  implicit val decoder: ConfDecoder[SemiAutoConfig] =
    metaconfig.generic.deriveDecoder(default)

  private val rewriteBundles = Map(
    "circe" -> List(
      SemiAutoConfig.Rewrite("io.circe.Encoder.AsObject", "Encoder.AsObject"),
      SemiAutoConfig.Rewrite("io.circe.Encoder", "Encoder.AsObject"),
      SemiAutoConfig.Rewrite("io.circe.Decoder", "Decoder"),
      SemiAutoConfig.Rewrite("io.circe.Codec.AsObject", "Codec.AsObject"),
      SemiAutoConfig.Rewrite("io.circe.Codec", "Codec.AsObject")
    ),
    "doobie" -> List(
      SemiAutoConfig.Rewrite("doobie.util.Read", "Read"),
      SemiAutoConfig.Rewrite("doobie.Types.Read", "Read"),
      SemiAutoConfig.Rewrite("doobie.util.Write", "Write"),
      SemiAutoConfig.Rewrite("doobie.Types.Write", "Write")
    )
  )

  case class Rewrite(
      typeClass: String,
      derived: String
  )

  object Rewrite {
    implicit val surface: Surface[Rewrite] =
      metaconfig.generic.deriveSurface[Rewrite]
    implicit val reader: ConfDecoder[Rewrite] =
      ConfDecoder.from[Rewrite] {
        case c: Conf.Obj =>
          (c.get[String]("typeclass", "typeClass") |@|
            c.get[String]("derived")).map { case (tc, d) => Rewrite(tc, d) }
        case _ => Configured.NotOk(ConfError.message("Wrong config format"))
      }
  }

}