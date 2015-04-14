package ca.regina.calendarsync.monads

case class ValidationT[F[_], E, A](run: F[Validation[E, A]])(implicit F: Monad[F]) {
  def pure(a: A) =  F.pure(Success(a))
  def map[B](f: A => B): ValidationT[F, E, B] = ValidationT(F.map(run)(_.map(f)))
  def flatMap[B](f: A => ValidationT[F, E, B]): ValidationT[F, E, B] =
    ValidationT(F.flatMap(run)(_ match {
      case Success(a) => f(a).run
      case Failure(e) => F.pure(Failure(e))
    }))
    
  def getOrElse(f: E => F[A]): F[A] = F.flatMap(run)(_.map(F.pure(_)).getOrElse(f))
  
}

object ValidationT {
  implicit def validationTMonad[E, F[_]](implicit F: Monad[F]) = new Monad[({type M[A] =  ValidationT[F, E, A]})#M] {
    def pure[A](a: A) = ValidationT(F.pure(Success(a)))
    def map[A, B](validationT: ValidationT[F, E, A])(f: A => B) = validationT.map(f)
    def flatMap[A, B](validationT: ValidationT[F, E, A])(f: A => ValidationT[F, E, B]) = validationT.flatMap(f)
  }
  
  implicit def richValidationMonad[F[_], E, A](runMonad: F[Validation[E, A]])(implicit F: Monad[F]) = new {
    def toValidationT: ValidationT[F, E, A] = ValidationT(runMonad) 
  }
}