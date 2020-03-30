package stainlessfit
package core

import core.trees._
import core.interpreter._
import core.typechecker._
import core.typechecker.Derivation._

import core.codegen._
import core.codegen.utils.{Printer => LLVMPrinter}

import parser.FitParser
import parser.FitLexer

import extraction._

import java.io.File

import core.util.RunContext

object Core {

  def parseString(s: String)(implicit rc: RunContext): Either[String, Tree] = {
    val it = s.iterator

    rc.parser(FitLexer(it)) match {
      case rc.parser.LL1.Parsed(value, _) =>
        Right(value)
      case rc.parser.LL1.UnexpectedEnd(rest) =>
        Left(
          s"""|Error during parsing, unexpected end of input.
              |Expected token: ${rest.first.mkString("   ")}""".stripMargin
        )
      case rc.parser.LL1.UnexpectedToken(t, rest) =>
        Left(
          s"""|Error during parsing, unexpected token at position ${t.pos}: $t
              |Expected token: ${rest.first.mkString("   ")}""".stripMargin
        )
    }
  }

  def parseFile(f: File)(implicit rc: RunContext): Either[String, Tree] = {
    val s = scala.io.Source.fromFile(f).getLines.mkString("\n")
    val regex = """Include\("(.*)"\)""".r
    val completeString = regex.replaceAllIn(s, m =>
      scala.io.Source.fromFile(new File(f.getAbsoluteFile().getParentFile().getCanonicalPath(), m.group(1))).getLines.mkString("\n") + "\n"
    )
    rc.bench.time("Scallion parsing") { parseString(completeString) }
  }

  def evalFile(f: File)(implicit rc: RunContext): Either[String, Tree] =
    parseFile(f) flatMap { src =>

      val pipeline =
        DebugPhase(new DefFunctionElimination(), "DefFunctionElimination") andThen
        DebugPhase(new Namer(), "Namer") andThen
        DebugPhase(new BuiltInIdentifiers(), "BuiltInIdentifiers") andThen
        DebugPhase(new Erasure(), "Erasure")

      val (t, _) = pipeline.transform(src)

      Interpreter.evaluate(t) match {
        case Error(error, _) => Left(error)
        case v => Right(v)
      }
    }

  def typeCheckFile(f: File)(implicit rc: RunContext): Either[String, (Boolean, NodeTree[Judgment])] = {
    parseFile(f) flatMap { src =>

      val pipeline =
        DebugPhase(new DefFunctionElimination(), "DefFunctionElimination") andThen
        DebugPhase(new FixIndexing(), "FixIndexing") andThen
        DebugPhase(new Namer(), "Namer") andThen
        DebugPhase(new BuiltInIdentifiers(), "BuiltInIdentifiers")

      val (t, ((_, max), _)) = pipeline.transform(src)

      rc.bench.time("Type Checking") { new TypeChecker().infer(t, max) } match {
        case None => Left(s"Could not typecheck: $f")
        case Some((success, tree)) =>
          if (rc.config.html)
            rc.bench.time("makeHTMLFile") {
              util.HTMLOutput.makeHTMLFile(f, List(tree), success)
            }

          Right((success, tree))
      }
    }
  }

  def compileFile(f: File)(implicit rc: RunContext): Either[String, Boolean] = {
    parseFile(f) flatMap { src =>

      val (t, _) = extraction.compilePipeline.transform(src)

      // val vrai = BooleanLiteral(true)
      // val faux = BooleanLiteral(false)
      //
      // val or = Primitive(Or, List(faux, vrai))
      // val neg = Primitive(Not, List(faux))
      // val testTree = Primitive(And, List(vrai, neg, or))
      //
      val module = CodeGen.genLLVM(t, true)

      LLVMPrinter.run(rc)(module)

      //println(s"Printing the AST:\n$t")

      Right(true)
    }
  }

  def evalFile(s: String)(implicit rc: RunContext): Either[String, Tree] =
    evalFile(new File(s))

  def typeCheckFile(s: String)(implicit rc: RunContext): Either[String, (Boolean, NodeTree[Judgment])] =
    typeCheckFile(new File(s))

}
