package ca.regina.calendarsync.monads

sealed trait StepT[E, F[_], A] {
  def fold[Z](
               cont: (Input[E] => IterateeT[E, F, A]) => Z
               , done: (=> A, => Input[E]) => Z
               ): Z

  /** An alias for `fold` */
  def apply[Z](
                cont: (Input[E] => IterateeT[E, F, A]) => Z
                , done: (=> A, => Input[E]) => Z
                ): Z = fold(cont, done)
                
  def pointI(implicit F: Monad[F]): IterateeT[E, F, A] = IterateeT.iterateeT(F.pure(this))
  
  def mapContOr[Z](k: (Input[E] => IterateeT[E, F, A]) => Z, z: => Z): Z = fold(k(_), (_, _) => z)
    
  def mapCont(k: (Input[E] => IterateeT[E, F, A]) => IterateeT[E, F, A])(implicit F : Monad[F]): IterateeT[E, F, A] =
    mapContOr[IterateeT[E, F, A]](k, pointI)
    
}

case class Done[E, F[_], A](d: A, r: Input[E]) extends StepT[E, F, A] {
	def fold[Z](
	           cont: (Input[E] => IterateeT[E, F, A]) => Z
	           , done: (=> A, => Input[E]) => Z
	           ) = done(d, r)
}

case class Cont[E, F[_], A](c: Input[E] => IterateeT[E, F, A]) extends StepT[E, F, A] {
    def fold[Z](
               cont: (Input[E] => IterateeT[E, F, A]) => Z
               , done: (=> A, => Input[E]) => Z
               ): Z =  cont(c)
}

object StepT {
  def scont[E, F[_], A](c: Input[E] => IterateeT[E, F, A]): StepT[E, F, A] = Cont(c)
  def sdone[E, F[_], A](d: A, r: Input[E]): StepT[E, F, A] = Done(d, r)
}