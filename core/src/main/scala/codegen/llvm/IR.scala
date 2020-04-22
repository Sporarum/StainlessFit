package stainlessfit
package core
package codegen.llvm

import core.codegen.utils.LocalHandler

object IR {

  abstract class Instruction

  case class Block(index: Int, label: Label, instructions: List[Instruction]) {
    def <:>(instr: Instruction) = Block(index, label, instructions :+ instr)
    def <:>(is: List[Instruction]) = Block(index, label, instructions ++ is)
  }

  case class Label (val label: String){
    override def toString: String = s"%$label"
    def printLabel: String = s"$label:"
  }

  case class Local (val name: String){
    override def toString: String = s"%$name"
  }

  case class Global (val name: String){
    override def toString: String = s"@$name"
  }

  abstract class Type
  case object BooleanType extends Type {
    override def toString(): String =  "i1"
  }
  case object UnitType extends Type {
    override def toString(): String =  "i1"
  }

  case object NatType extends Type {
    override def toString(): String =  "i32"
  }

  case class PairType(firstType: Type, secondType: Type) extends Type
  case class FirstType(nested: Type) extends Type
  case class SecondType(nested: Type) extends Type

  case class EitherType(leftType: Type, rightType: Type) extends Type
  case class LeftType(either: Type) extends Type
  case class RightType(either: Type) extends Type

  case class FunctionReturnType(funName: Global) extends Type

  case class ParamDef(tpe: Type, local: Local)

  class Value(val v: Either[Local, Literal])
  object Value {
    def apply(local: Local): Value = new Value(Left(local))
    def apply(literal: Literal): Value = new Value(Right(literal))
  }

  abstract class Literal extends Instruction
  case class BooleanLiteral(b: Boolean) extends Literal
  case class Nat(n: BigInt) extends Literal
  case object UnitLiteral extends Literal
  case class PairLiteral(first: Literal, second: Literal) extends Literal

  //Boolean operations
  abstract class Op extends Instruction {
    def returnType: Type
  }

  abstract class BooleanOperator extends Op {
    override def returnType: Type = BooleanType
  }

  abstract class ComparisonOperator extends Op {
    override def toString(): String = "icmp "
    override def returnType: Type = BooleanType
  }

  abstract class NatOperator extends Op {
    override def returnType: Type = NatType
  }

  case object And extends BooleanOperator {
    override def toString: String = "and"
  }
  case object Or extends BooleanOperator {
    override def toString: String = "or"
  }
  case object Not extends BooleanOperator {
    override def toString: String = "fneg"
  }

  case object Neq extends ComparisonOperator {
    override def toString: String = super.toString + "ne"
  }
  case object Eq extends ComparisonOperator {
    override def toString: String = super.toString + "eq"
  }
  case object Lt extends ComparisonOperator {
    override def toString: String = super.toString + "slt"
  }
  case object Gt extends ComparisonOperator {
    override def toString: String = super.toString + "sgt"
  }
  case object Leq extends ComparisonOperator {
    override def toString: String = super.toString + "sle"
  }
  case object Geq extends ComparisonOperator {
    override def toString: String = super.toString + "sge"
  }

  case object Nop extends Op {
    override def returnType: Type = UnitType  //TODO is Nop even used?
  }

  case object Plus extends NatOperator {
    override def toString: String = "add"
  }
  case object Minus extends NatOperator {
    override def toString: String = "sub"
  }
  case object Mul extends NatOperator {
    override def toString: String = "mul"
  }
  case object Div extends NatOperator {
    override def toString: String = "sdiv"
  }

  case class Variable(local: Local) extends Instruction

  case class BinaryOp(op: Op, result: Local, lhs: Value, rhs: Value) extends Instruction
  case class UnaryOp(op: Op, result: Local, operand: Value) extends Instruction

  case class Phi(res: Local, typee: Type, candidates: List[(Local, Label)]) extends Instruction
  case class Assign(result: Local, typee: Type, from: Value) extends Instruction
  case class Call(res: Local, function: Global, args: List[Value]) extends Instruction

  //Terminator instructions
  case class Branch(condition: Value, ifTrue: Label, ifFalse: Label) extends Instruction
  case class Jump(destination: Label) extends Instruction
  case class Return(result : Value, typee: Type) extends Instruction

  // %sizeptr = getelementptr {i32, i32}, {i32, i32}* null, i32 1
  //
  // %size = ptrtoint {i32, i32}* %sizeptr to i64
  //
  // %pair = call noalias i8* @malloc(i64 %size)
  case class Store(value: Value, tpe: Type, ptr: Local) extends Instruction
  case class Load(result: Local, tpe: Type, ptr: Local) extends Instruction

  // case class GepToFirst(result: Local, tpe: Type, pair: Local) extends Instruction
  // case class GepToSecond(result: Local, tpe: Type, pair: Local) extends Instruction
  case class GepToIdx(result: Local, tpe: Type, ptr: Value, idx: Option[Int]) extends Instruction
  case class Malloc(result: Local, temp1: Local, temp2: Local, temp3: Local, tpe: Type) extends Instruction

  //Pretty printing instructions
  case class PrintResult(toPrint: Local, tpe: Type, lh: LocalHandler) extends Instruction

  case class PrintNat(value: Value) extends Instruction
  case class PrintBool(bool: Local) extends Instruction

  case object PrintOpen extends Instruction
  case object PrintClose extends Instruction
  case object PrintComma extends Instruction
  case object PrintTrue extends Instruction
  case object PrintFalse extends Instruction
  case object NoOp extends Instruction
}
