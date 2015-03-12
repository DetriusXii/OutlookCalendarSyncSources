package ca.regina.calendarsync.monads

object OptionExtensions {
	implicit val optionMonad: Monad[Option] = new Monad[Option] {
	  def pure[A](a: A): Option[A] = Some(a)
	  def map[A, B](option: Option[A])(f: A => B): Option[B] = option.map(f)
	  def flatMap[A, B](option: Option[A])(f: A => Option[B]): Option[B] =  option.flatMap(f)
	}
}