package ca.regina.calendarsync.monads

object Identity {
	type Id[X] = X
	
	implicit val id = new Monad[Id] {
	  def pure[A](a: A) = a
	  def map[A, B](idA: Id[A])(f: A => B) =  f(idA)
	  def flatMap[A, B](idA: Id[A])(f: A => Id[B]) =  f(idA)
	}
}