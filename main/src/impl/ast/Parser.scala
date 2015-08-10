package impl.ast

import java.io.File

import scala.io.Source
import scala.util.parsing.combinator.RegexParsers

object Parser {

  /**
   * A simple parser for s-expression based on Scala's parser combinators.
   */
  object InternalParser extends RegexParsers {

    /**
     * Keywords.
     */
    def keyword: Parser[SExp.Keyword] =
      ("def-type" | "def-bot" | "def-leq" | "def-lub" | "def-height" | "def-fun" | "fact" | "rule" |
        "variant" | "match" | "case" | "->" | "if" ) ^^ SExp.Keyword

    /**
     * Operators
     */
    def operator: Parser[SExp.Operator] =
      ("==" | "!=" | "<=" | ">=" | "+" | "-" | "*" | "/" | "%" | "and" | "lte" | "gte" | "min" | "max") ^^ SExp.Operator

    /**
     * Values.
     */
    // unit literal
    def unit: Parser[SExp.Unit] = "unit" ^^ SExp.Unit

    // boolean literal
    def bool: Parser[SExp.Bool] = ("true" | "false") ^^ SExp.Bool

    // integer literal
    def int: Parser[SExp.Int] = """-?[0-9]+""".r ^^ SExp.Int

    // string literal
    def str: Parser[SExp.Str] = ("\"" ~> """[^\"]+""".r <~ "\"") ^^ SExp.Str

    // value production
    def value: Parser[SExp] = unit | bool | int | str

    /**
     * Names.
     */
    def name: Parser[SExp.Name] = """[A-Z][A-Za-z+-/\*]*""".r ^^ SExp.Name

    /**
     * Variables.
     */
    def variable: Parser[SExp.Var] =  ("""[a-z][0-9a-z]*""".r | "_") ^^ SExp.Var

    /**
     * S-expression body.
     */
    def body: Parser[SExp] = keyword | value | operator | variable | name | sexp

    /**
     * A set-expression.
     */
    def setexp: Parser[SExp] =  "{" ~> rep(body) <~ "}" ^^ {
      case xs: List[SExp] => SExp.Lst(List(SExp.Keyword("set")) ::: xs)
    }

    /**
     * A vector-expression.
     */
    def vecexp: Parser[SExp] =  "<" ~> rep(body) <~ ">" ^^ {
      case xs: List[SExp] => SExp.Lst(List(SExp.Keyword("vec")) ::: xs)
    }

    /**
     * S-expression.
     */
    def sexp: Parser[SExp] = ("(" ~> rep(body) <~ ")" ^^ SExp.Lst) | ("[" ~> rep(body) <~ "]" ^^ SExp.Lst) | setexp | vecexp

    /**
     * Declaration.
     */
    def decl: Parser[List[SExp]] = rep(sexp)
  }

  /**
   * Returns a list of s-expressions parsed from the given file `f`.
   */
  def parse(f: File): List[SExp] = {
    // read all lines into a single string.
    val source = Source.fromFile(f).getLines().mkString("\n")
    parse(source)
  }

  def parse(source: String): List[SExp] = {
    // remove comments
    val uncommented = source.replaceAll("//[^\n]*", "")
    // parse the entire file
    val parseResult = InternalParser.parseAll(InternalParser.decl, uncommented)
    // inspect the result
    parseResult match {
      case InternalParser.Success(ast, next) => ast
      case InternalParser.Failure(msg, next) => throw new RuntimeException(s"Parsing Failed!: $msg\nline ${next.pos.line} column ${next.pos.column}")
      case InternalParser.Error(msg, next) => throw new RuntimeException(s"Parsing Failed!: $msg")
    }
  }

}