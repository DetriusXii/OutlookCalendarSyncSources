package ca.regina.calendarsync.monads

sealed abstract class Input[E] {
  def fold[Z](empty : => Z, el: E => Z, eof : => Z): Z
  
  def apply[Z](empty : => Z, el: E => Z, eof : => Z) = fold(empty, el, eof)
  
  def isEmpty(): Boolean = apply(true, _ => false, false)
  
  def isEl(): Boolean = apply(false, _ => true, false)
  
  def isEof(): Boolean = apply(false, _ => false, true)
  
  def map[B](f: E => B): Input[B] = fold(Input.emptyInput, e => Input.elInput(f(e)), Input.eofInput)
  
  def flatMap[B](f: E => Input[B]): Input[B] =  fold(Input.emptyInput, e => f(e), Input.eofInput)
}

case class Empty[E]() extends Input[E] {
  def fold[Z](empty : => Z, el: E => Z, eof : => Z): Z =  empty
}

case class Element[E](e: E) extends Input[E] {
  def fold[Z](empty : => Z, el: E => Z, eof : => Z): Z = el(e)
}

case class Eof[E]() extends Input[E] {
  def fold[Z](empty : => Z, el: E => Z, eof : => Z): Z =  eof
}

object Input {
  def apply[E](e: E): Input[E] = elInput(e)
  
  def emptyInput[E]: Input[E] = Empty[E]
  def elInput[E](e: => E): Input[E] = Element(e)
  def eofInput[E]: Input[E] = Eof[E]
}