package ca.regina.calendarsync.monads

trait Monad[F[_]] {
	def pure[A](a: A): F[A]
	def map[A, B](fa: F[A])(f: A => B): F[B]
	def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
}