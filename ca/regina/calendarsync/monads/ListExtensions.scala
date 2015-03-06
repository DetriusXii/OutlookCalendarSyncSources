package ca.regina.calendarsync.monads

object ListExtensions {
	implicit def richList[M[_], A](list: List[M[A]])(implicit M: Monad[M]) = new {
	  def sequence: M[List[A]] =
			  M.map(list.foldLeft(M.pure[List[A]](Nil))((u, v) => 
			  M.flatMap(u)(builtList => M.map(v)(_ :: builtList))
					  ))(_.reverse)
	}
}