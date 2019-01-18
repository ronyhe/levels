package com.ronyhe.levels

import scala.util.parsing.combinator.Parsers

/** A mixin for [[scala.util.parsing.combinator.Parsers]] that enables easier expression parsing.
  *
  * The main element of is the `levels` method which affords easy abstraction over
  * the so-called 'levels' that emerge when describing non left recursive expression grammars.
  *
  * Each level is a function that takes the top-level parser and the next-level parser.
  * It returns a parser for the current level.
  * The levels become decoupled, so the shuffling of operator precedence in the grammar is as easy
  * as reordering the arguments to the `levels` function.
  *
  * For example, instead of the tightly coupled:
  * {{{
  * def expr = term ("+" ~ term)* ^^ combineAddition
  * def term = factor ("*" ~ factor)* ^^ combineMultiplication
  * def factor = numberParser | "(" ~> expr <~ ")"
  * }}}
  *
  * One can use this. Which is easier to modify, extend and reason about:
  * {{{
  * val bottom = failure("No viable alternative")
  * levels(bottom)(Seq(
  *   (top, next) => chainl1(next, "+" ~> next, success(combineAddition)),
  *   (top, next) => chainl1(next, "*" ~> next, success(combineMultiplication)),
  *   (top, next) => numberParser | next
  *   (top, next) => "(" ~> expr <~ ")" | next
  * ))
  * }}}
  *
  * Even better, use the utility methods to make the intent clearer:
  * {{{levelsWithDefault(Seq(
  *   leftAssociativeNextLevel("+", combineAddition),
  *   leftAssociativeNextLevel("*", combineMultiplication),
  *   orNext(numberParser),
  *   (top, next) => "(" ~> top <~ ")" | next
  * ))}}}
  *
  * For a full example see [[com.ronyhe.levels.ArithmeticExample]]
  */
trait Levels {
  this: Parsers =>

  /** A binary function that produces a Level parser.
    *
    * The first argument for the function is the top level parser.
    * The second argument for the function is the next level parser.
    */
  type LevelCreator[A] = (=> Parser[A], Parser[A]) => Parser[A]

  /** Returns a parser that connects the individual Level parsers.
    *
    * @param bottom   a default parser for when all alternatives fail
    * @param creators the individual Level creators
    * @tparam A the value the parser eventually produces
    */
  def levels[A](bottom: Parser[A])(creators: Seq[LevelCreator[A]]): Parser[A] = {
    lazy val top: Parser[A] = creators.foldRight(bottom)((creator, acc) => creator(top, acc))
    top
  }

  /** Returns a levels parser with a default bottom parser that emits an error message */
  def levelsWithDefault[A](creators: Seq[LevelCreator[A]]): Parser[A] =
    levels(failure("Bottom parser reached. No viable alternative"))(creators)

  /** Returns a `LevelCreator[A]` for left associative operators.
    *
    * It parses the next level and then repeatedly parses the `op` followed by the next level.
    * It combines each occurrence of a next level expression with the previous result using the `combine` function
    *
    * @param op      the operator (usually a symbol like "+" or "-")
    * @param combine a function that combines each next level expression with the previous results
    * @tparam A the value the parser eventually produces
    */
  def leftAssociativeNextLevel[A](op: Parser[Any], combine: (A, A) => A): LevelCreator[A] =
    (_, next) => chainl1(next, op ~> next, success(combine))

  /** Returns a `LevelCreator[A]` for right associative operators.
    *
    * It parses the next level and then repeatedly parses the `op` followed by the next level.
    * It combines each occurrence of a next level expression with the previous result using the `combine` function
    *
    * @param op      the operator (usually a symbol like "+" or "-")
    * @param combine a function that combines each next level expression with the previous results
    * @tparam A the value the parser eventually produces
    */
  def rightAssociativeNextLevel[A](op: Parser[Any], combine: (A, A) => A): LevelCreator[A] = {
    // If only one element is parsed, we should just return it.
    // That won't happen with a direct call to chainr1 which would return the single value combined with the seed.
    // So, we first parse a list and then reduce it (instead of folding)
    // Is there a more elegant solution? Suggestions are welcome
    val cons: (A, List[A]) => List[A] = (a, as) => a :: as

    (_, next) => {
      val asList: Parser[List[A]] = chainr1(next, op ^^^ cons, cons, Nil)
      asList ^^ (_ reduceRight combine)
    }
  }

  /** Returns a `LevelCreator[A]` that 'falls through' to the next level when it fails.
    *
    * @param parser the parser for the current level
    * @tparam A the value the parser eventually produces
    */
  def orNext[A](parser: Parser[A]): LevelCreator[A] =
    (_, next) => parser | next

}
