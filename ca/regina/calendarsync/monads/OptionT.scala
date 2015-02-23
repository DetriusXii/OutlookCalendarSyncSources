package ca.regina.calendarsync.monads

final case class OptionT[F[_], A](run: F[Option[A]])(implicit F: Monad[F]) { self =>
	def map[B](f: A => B): OptionT[F, B] = OptionT[F, B](F.map(run)(_.map(f)))
	def flatMap[B](f: A => OptionT[F, B]): OptionT[F, B] = OptionT[F, B](F.flatMap(run)(_ match {
	  case None => F.pure(None)
	  case Some(z) => f(z).run 
	}))
	
	def getOrElse(default: => A): F[A] = F.map(run)(_.getOrElse(default))
}

object OptionT {
  implicit def optionTMonad[M[_]](implicit M: Monad[M]) = new Monad[({type G[A] = OptionT[M, A]})#G] {
    def pure[A](a: A) = OptionT(M.pure(Some(a)))
    def map[A, B](optionT: OptionT[M, A])(f: A => B) = optionT.map(f)
    def flatMap[A, B](optionT: OptionT[M, A])(f: A => OptionT[M, B]) = optionT.flatMap(f)
  }
}