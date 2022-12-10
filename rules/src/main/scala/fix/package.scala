/*
 * Copyright 2022 Arktekk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import scala.annotation.tailrec
import scala.meta._
import scalafix.XtensionScalafixProductInspect

package object fix {

  implicit class ModExt(m: Mod) {
    def isModImplicit: Boolean =
      m match {
        case _: Mod.Implicit => true
        case _               => false
      }

    def isModCase: Boolean =
      m match {
        case _: Mod.Case => true
        case _           => false
      }
  }

  def printDefn(t: Defn): Unit = {
    println(t.structureLabeled)
  }

  implicit class TokensExt(tokens: Tokens) {

    def findToken(p: Token => Boolean): Option[Tokens] = {
      val newTokens = tokens.dropWhile(!p(_)).take(1)
      if (newTokens.length == 1) Some(newTokens)
      else None
    }

    def tokensWithTailingSpace(): List[Token] = {
      @tailrec
      def run(pos: Int, ws: List[Token]): List[Token] = {
        val next = pos + 1
        if (next < tokens.tokens.length && tokens.tokens(next).is[Token.Space]) run(next, tokens.tokens(next) :: ws)
        else ws
      }
      tokens.toList ++ run(tokens.start, Nil)
    }
  }

}