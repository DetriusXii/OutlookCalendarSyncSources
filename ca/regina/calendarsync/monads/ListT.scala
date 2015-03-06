package ca.regina.calendarsync.monads

case class ListT[M[_], A](run: M[List[A]])(implicit M: Monad[M]) {
	def ::(a: A): ListT[M, A] = ListT(M.map(run)(a :: _))
	def ::(ma: M[A]): ListT[M, A] = ListT(M.flatMap(run)(list => M.map(ma)(_ :: list)))
	def isEmpty = M.map(run)(_.isEmpty)
	def headOption = M.map(run)(_.headOption)
	
	def ++(bs: => ListT[M, A]) =  ListT(M.flatMap(run)(list1 => M.map(bs.run)(list2 => list1 ++ list2)))
	def map[B](f: A => B) = ListT(M.map(run)(_.map(f)))
	def flatMap[B](f: A => ListT[M, B]): ListT[M, B] = ListT(M.flatMap(run)(_ match {
	  case Nil => M.pure(Nil)
	  case nonEmpty => nonEmpty.map(f).reduce(_ ++ _).run
	}))
	
	def tail: ListT[M, A] =  ListT(M.map(run)(_.tail))
	def foldLeft[B](z: => B)(f: (=> B, => A) => B): M[B] = M.map(run)(_.foldLeft(z)((left, right) => f(left, right)))
	def toList =  run
	def foldRight[B](z: => B)(f: (=> A, => B) => B): M[B] = M.map(run)(_.foldRight(z)((right, left) => f(right, left)))
	def length: M[Int] = M.map(run)(_.length)
}

object ListT {
  implicit def listTMonad[F[_]](implicit F: Monad[F])  = new Monad[({type M[A] = ListT[F, A]})#M] {
    def pure[A](a: A) = ListT(F.pure(a :: Nil))
    def map[A, B](listT: ListT[F, A])(f: A => B) = listT.map(f)
    def flatMap[A, B](listT: ListT[F, A])(f: A => ListT[F, B]) = listT.flatMap(f)
  }
  
  def emptyListTMonad[M[_], A](implicit M: Monad[M]) =  ListT[M, A](M.pure(Nil))
}