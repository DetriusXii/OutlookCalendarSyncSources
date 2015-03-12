package ca.regina.calendarsync.monads

sealed trait IoExceptionOr[A] {
  import IoExceptionOr._
  
  def fold[X](ioException: java.io.IOException => X, or: A => X): X
  def map[B](f: A => B): IoExceptionOr[B] =  fold(ioException, a => IoExceptionOr(() => f(a)))
  
  def flatMap[B](f: A => IoExceptionOr[B]): IoExceptionOr[B] =  fold(ioException, f)
}

object IoExceptionOr {
  type IoException = java.io.IOException
  
  implicit def ioExceptionOrMonad =  new Monad[IoExceptionOr] {
    def pure[A](a: A) = ioExceptionOr(a)
    def map[A, B](ioExceptionOr: IoExceptionOr[A])(f: A => B) = ioExceptionOr.map(f)
    def flatMap[A, B](ioExceptionOr: IoExceptionOr[A])(f: A => IoExceptionOr[B]) =
      ioExceptionOr.flatMap(f)
  } 
  
  def ioException[A]: java.io.IOException => IoExceptionOr[A] = e => new IoExceptionOr[A] {
    def fold[X](ioException: IoException => X, or: A => X) = ioException(e)
  }
  
  def ioExceptionOr[A](a: A): IoExceptionOr[A] =  new IoExceptionOr[A] {
    def fold[X](ioException: IoException => X, or: A => X) = or(a)
  }
  
  def apply[A](a: () => A): IoExceptionOr[A] = try {
    ioExceptionOr(a())
  } catch {
    case e: java.io.IOException => ioException(e) 
  }
}