package ca.regina.calendarsync.monads

trait LiftIO[F[_]] { self =>
  def liftIO[A](ioa: IO[A]): F[A]
}

object LiftIO {
  def apply[F[_]](implicit F: LiftIO[F]): LiftIO[F] = F 
  
  implicit val ioLiftIO: LiftIO[IO] = new LiftIO[IO] {
    def liftIO[A](ioa: IO[A]): IO[A] = ioa
  }
  
  implicit def validationTLiftIO[F[_]: LiftIO, E](implicit F: Monad[F]) = new LiftIO[({type M[A] =  ValidationT[F, E, A]})#M] {
    def liftIO[A](ioa: IO[A]) = ValidationT[F, E, A](LiftIO[F].liftIO(ioa.map(Success(_))))
  }
  
  implicit def listTLiftIO[F[_] : LiftIO](implicit F: Monad[F]) = new LiftIO[({type M[A] = ListT[F, A]})#M] {
    def liftIO[A](ioa: IO[A]): ListT[F, A] = ListT[F, A](LiftIO[F].liftIO(ioa.map(_ :: Nil)))
  }
  
  implicit def optionTLiftIO[F[_] : LiftIO](implicit F: Monad[F]) = new LiftIO[({type M[A] = OptionT[F, A]})#M] {
    def liftIO[A](ioa: IO[A]) = OptionT[F, A](LiftIO[F].liftIO(ioa.map(Some(_))))
  }
}