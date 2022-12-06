package fix

import scalafix.v1.{Patch, _}

import scala.meta._

class SemiAuto extends SemanticRule("SemiAuto") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    //    println("Tree.syntax: " + doc.tree.syntax)
    //    println("Tree.structure: " + doc.tree.structure)
    //    println("Tree.structureLabeled: " + doc.tree.structureLabeled)

    val config = Set(
      SemiAuto.Config("io.circe.Encoder.AsObject", "Encoder.AsObject"),
      SemiAuto.Config("io.circe.Encoder", "Encoder.AsObject"),
      SemiAuto.Config("io.circe.Decoder", "Decoder"),
      SemiAuto.Config("io.circe.Codec.AsObject", "Codec.AsObject"),
      SemiAuto.Config("io.circe.Codec", "Codec.AsObject"),
      SemiAuto.Config("doobie.util.Read", "Read"),
      SemiAuto.Config("doobie.Types.Read", "Read"),
      SemiAuto.Config("doobie.util.Write", "Write"),
      SemiAuto.Config("doobie.Types.Write", "Write")
    ).map(c => c.typ -> c).toMap

    doc.tree.collect { case CaseClassWithCompanion(caseClass, companion @ SemiAutoDerived(items)) =>
      items.flatMap(item => config.get(item.deriveType).map(item -> _)) match {
        case Nil => Patch.empty
        case toRewrite =>
          val derivePos =
            caseClass.templ.derives.lastOption
              .orElse(caseClass.templ.inits.lastOption)
              .orElse(if (caseClass.templ.stats.nonEmpty) Some(caseClass.ctor) else None)
              .getOrElse(caseClass)
          val base = if (caseClass.templ.derives.isEmpty) " derives " else ", "
          val derivePatch = Patch.addRight(derivePos, base ++ toRewrite.map(_._2.derived).mkString(", "))
          val removePatch =
            if (childrenInCompanion(companion) == toRewrite.size) Patch.removeTokens(companion.tokens)
            else Patch.removeTokens(toRewrite.flatMap(_._1.defnVal.tokens))

          derivePatch + removePatch
      }
    }.asPatch
  }

  private def childrenInCompanion(companion: Defn.Object): Int = {
    companion.templ.children.count {
      case Self(Name.Anonymous(), None) => false
      case _                            => true
    }
  }
}

object SemiAuto {

  case class Config(
      typ: String,
      derived: String
  )
}

object CaseClassWithCompanion {
  def unapply(t: Tree): Option[(Defn.Class, Defn.Object)] =
    t match {
      case c @ Defn.Class(mods, cName, _, _, _) if isCaseClass(mods) =>
        c.parent.flatMap { st =>
          st.children.collectFirst {
            case o @ Defn.Object(_, oName, _) if cName.value == oName.value => c -> o
          }
        }
      case _ => None
    }

  private def isCaseClass(mods: List[Mod]) =
    mods.exists {
      case _: Mod.Case => true
      case _           => false
    }
}

case class SemiAutoDerived(
    deriveType: String,
    defnVal: Defn.Val
)
object SemiAutoDerived {

  def unapply(o: Defn.Object)(implicit doc: SemanticDocument): Option[List[SemiAutoDerived]] =
    nonEmptyList(o.templ.stats.collect {
      case v @ Defn.Val(mods, _, Some(typeApply @ Type.Apply(_, (typeName: Type.Name) :: Nil)), t)
          if matchingType(o, typeName) && hasImplicitMod(mods) && isSemiAuto(t) =>
        SemiAutoDerived(typeApply.symbol.normalized.value.dropRight(1), v)
    })

  private def matchingType(o: Defn.Object, typeName: Type.Name) =
    typeName.value == o.name.value

  private def isSemiAuto(t: Term)(implicit doc: SemanticDocument) = {
    t.symbol.normalized.value.contains(".semiauto.") || t.symbol.normalized.value.contains("derived")
  }

  private def nonEmptyList[A](l: List[A]): Option[List[A]] =
    if (l.isEmpty) None else Some(l)

  private def hasImplicitMod(mods: List[Mod]) =
    mods.exists {
      case _: Mod.Implicit => true
      case _               => false
    }
}