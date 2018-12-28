## levels - Easier expression parsing with parser combinators

### The problem
Expression grammars tend to tightly couple several "levels" of expression.
Each level corresponds to a binding strength. This is a result of the common
need to eliminate left recursion. An arithmetic example:
```scala
def expr = term ("+" ~ term)* ^^ combineAddition
def term = factor ("*" ~ factor)* ^^ combineMultiplication
def factor = numberParser | "(" ~> expr <~ ")"
```

These are hardcoded relationships that are inflexible and hard to get right.

### The solution
The `Levels` trait factors out the the "top level parser" and the "next level parser".
Each level becomes a function that takes those two and produces a parser.
```scala
type LevelCreator[A] = (=> Parser[A], Parser[A]) => Parser[A]
```

These level creators can be passed to the `levels` method that combines them into a single expression parser:
```scala
class Arithmetic extends Parsers with Levels {
    val bottom = failure("No viable alternative")
    levels(bottom)(Seq(
      (top, next) => chainl1(next, "+" ~> next, success(combineAddition)),
      (top, next) => chainl1(next, "*" ~> next, success(combineMultiplication)),
      (top, next) => numberParser | next
      (top, next) => "(" ~> expr <~ ")" | next
    ))
}
```
Now the grammar is easier to modify, extend and reason about.
Any change is a simple matter of rearranging the Seq.


### But wait, there's more
As long as we're here, might as well use the utility methods in the `Levels` trait to make our intentions clear:
```scala
class Arithmetic extends Parsers with Levels {
    levelsWithDefault(Seq(
      leftAssociativeNextLevel("+", combineAddition),
      leftAssociativeNextLevel("*", combineMultiplication),
      orNext(numberParser),
      (top, next) => "(" ~> top <~ ")" | next
    ))
}
```

Want to add an exponentiation operator? No problem:
```scala
class Arithmetic extends Parsers with Levels {
    levelsWithDefault(Seq(
      leftAssociativeNextLevel("+", combineAddition),
      leftAssociativeNextLevel("*", combineMultiplication),
      rightAssociativeNextLevel("^", combineExponentiation)
      orNext(numberParser),
      (top, next) => "(" ~> top <~ ")" | next
    ))
}
```
