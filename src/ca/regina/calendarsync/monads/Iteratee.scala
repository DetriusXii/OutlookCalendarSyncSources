package ca.regina.calendarsync.monads


class IterateeT[E, F[_], A](val value: F[StepT[E, F, A]])(implicit F: Monad[F]) {
  def map[B](f: A => B): IterateeT[E, F, B] = flatMap(a => StepT.sdone(f(a), Input.emptyInput).pointI)
  
  def flatMap[B](f: A => IterateeT[E, F, B]): IterateeT[E, F, B] = {
    def through(x: IterateeT[E, F, A]): IterateeT[E, F, B] =
      IterateeT.iterateeT(
        F.flatMap(x.value)((s: StepT[E, F, A]) => s.fold[F[StepT[E, F, B]]](
          cont = k => F.pure(StepT.scont(u => through(k(u))))
          , done = (a, i) =>
            if (i.isEmpty)
              f(a).value
            else
              F.flatMap(f(a).value)(_.fold(
                cont = kk => kk(i).value
                , done = (aa, _) => F.pure(StepT.sdone[E, F, B](aa, i))
              ))
        )))
    through(this)
  }
  
  def >>==[EE, AA](f: StepT[E, F, A] => IterateeT[EE, F, AA]): IterateeT[EE, F, AA] =
    IterateeT.iterateeT(F.flatMap(value)(s => f(s).value))
  
  def &=(e: EnumeratorT[E, F]): IterateeT[E, F, A] = this >>== e[A]
}

object IterateeT {
  def apply[E, F[_], A](s: F[StepT[E, F, A]])(implicit F: Monad[F]): IterateeT[E, F, A] = iterateeT(s)
  
  def iterateeT[E, F[_], A](s: F[StepT[E, F, A]])(implicit F: Monad[F]): IterateeT[E, F, A] = new IterateeT[E, F, A](s)
  
  implicit def iterateeTMonad[E, F[_]](implicit F: Monad[F]) = new Monad[({type M[A] = IterateeT[E, F, A]})#M] {
    def pure[A](a: A) = iterateeT(F.pure(Done(a, Input.emptyInput)))
    def map[A, B](iterateeT: IterateeT[E, F, A])(f: A => B): IterateeT[E, F, B] = iterateeT.map(f)
    def flatMap[A, B](iterateeT: IterateeT[E, F, A])(f: A => IterateeT[E, F, B]): IterateeT[E, F, B] =  iterateeT.flatMap(f)
  }
  
  type Iteratee[E, A] =  IterateeT[E, Identity.Id, A]
}