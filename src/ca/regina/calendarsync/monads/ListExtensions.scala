package ca.regina.calendarsync.monads

object ListExtensions {
	implicit def richList[M[_], A](list: List[M[A]])(implicit M: Monad[M]) = new {
	  def sequence: M[List[A]] =
			  M.map(list.foldLeft(M.pure[List[A]](Nil))((u, v) => 
			  M.flatMap(u)(builtList => M.map(v)(_ :: builtList))
					  ))(_.reverse)
	}
	
	implicit val listMonad: Monad[List] = new Monad[List] {
	  def pure[A](a: A): List[A] = a :: Nil
	  def map[A, B](list: List[A])(f: A => B): List[B] =  list.map(f)
	  def flatMap[A, B](list: List[A])(f: A => List[B]): List[B] =  list.flatMap(f)
	}
}