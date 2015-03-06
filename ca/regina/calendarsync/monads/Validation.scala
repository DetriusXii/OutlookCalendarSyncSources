package ca.regina.calendarsync.monads

import scala.reflect.ClassTag

object Validation {
  def tryCatchExpression[E <: Throwable, A](tryExpression: () => A)(implicit m:ClassTag[E]): Validation[E, A] =
    try {
      Success(tryExpression())
    } catch {
      case e : E => Failure(e)
    }
    
  implicit def validationMonad[E] =  new Monad[({type M[A] = Validation[E, A]})#M] {
    def pure[A](a: A) = Success(a)
    def map[A, B](validation: Validation[E, A])(f: A => B): Validation[E, B] = validation.map(f)
    def flatMap[A, B](validation: Validation[E, A])(f: A => Validation[E, B]): Validation[E, B] =  validation.flatMap(f)
  }
}

sealed abstract class Validation[E, A] {
  def flatMap[B](f: (A) => Validation[E, B]): Validation[E, B] = this match {
    case Success(a) => f(a)
    case Failure(e) => Failure(e)
  }
  
  def map[B](f: A => B): Validation[E, B] = this match {
    case Success(a) => Success(f(a))
    case Failure(e) => Failure(e)
  }
  
  def mapFailure[B](f: E => B): Validation[B, A] = this match {
    case Success(a) => Success(a)
    case Failure(e) => Failure(f(e))
  }
  
  def getOrElse(f: E => A): A = this match {
    case Success(a) => a
    case Failure(e) => f(e)
  }
}
case class Success[E, A](a: A) extends Validation[E, A]
case class Failure[E, A](e: E) extends Validation[E, A]
