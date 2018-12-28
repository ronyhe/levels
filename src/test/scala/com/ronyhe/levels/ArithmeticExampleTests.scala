package com.ronyhe.levels

import com.ronyhe.levels.ArithmeticExample.Expr
import com.ronyhe.levels.ArithmeticExample.Expr.{Addition, Exponentiation, Multiplication, Number}
import org.scalatest.FunSpec

import scala.language.implicitConversions

class ArithmeticExampleTests extends FunSpec {

  implicit private def intToExpr(i: Int): Expr = Number(i)

  def assertExpr(input: String, expected: Expr): Unit = {
    val parsers = new ArithmeticExample
    val result = parsers.parseAll(parsers.expr, input)
    result match {
      case parsers.Success(actual, _) =>
        assertResult(expected)(actual)
      case _: parsers.NoSuccess => fail(s"Cannot parse '$input'")
    }
  }


  describe("Associativity") {
    describe("Addition") {
      it("is left associative") {
        assertExpr(
          "1 + 2 + 3",
          Addition(Addition(1, 2), 3)
        )
      }
    }

    describe("Exponentiation") {
      it("is right associative") {
        assertExpr(
          "1 ^ 2 ^ 3",
          Exponentiation(1, Exponentiation(2, 3))
        )
      }
    }
  }

  describe("Precedence") {

    it("Multiplication binds tighter than addition") {
      assertExpr(
        "1 + 2 * 3",
        Addition(
          1,
          Multiplication(2, 3)
        )
      )
    }

    it("Exponentiation binds tighter than multiplication") {
      assertExpr(
        "1 * 2 ^ 3",
        Multiplication(
          1,
          Exponentiation(2, 3)
        )
      )
    }

    it("Parenthesized expression bind the tightest") {
      assertExpr(
        "(1 * 2) ^ 3",
        Exponentiation(
          Multiplication(1, 2),
          3
        )
      )
    }

  }

}
