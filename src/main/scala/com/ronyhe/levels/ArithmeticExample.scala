package com.ronyhe.levels

import com.ronyhe.levels.ArithmeticExample.Expr
import com.ronyhe.levels.ArithmeticExample.Expr._

import scala.util.parsing.combinator.RegexParsers

class ArithmeticExample extends RegexParsers with Levels {

  def integer: Parser[Expr] = """\d+""".r ^^ (i => Number(i.toInt))

  def parens: LevelCreator[Expr] = (top, next) => literal("(") ~> top <~ literal(")") | next

  def expr: Parser[Expr] = levelsWithDefault[Expr](Seq(
    leftAssociativeNextLevel("+", Addition),
    leftAssociativeNextLevel("-", Subtraction),
    leftAssociativeNextLevel("*", Multiplication),
    leftAssociativeNextLevel("/", Division),
    rightAssociativeNextLevel("^", Exponentiation),
    orNext(integer),
    parens
  ))
}

object ArithmeticExample {

  sealed trait Expr

  object Expr {

    case class Addition(left: Expr, right: Expr) extends Expr

    case class Subtraction(left: Expr, right: Expr) extends Expr

    case class Multiplication(left: Expr, right: Expr) extends Expr

    case class Division(left: Expr, right: Expr) extends Expr

    case class Exponentiation(left: Expr, right: Expr) extends Expr

    case class Number(value: Int) extends Expr

  }

}
