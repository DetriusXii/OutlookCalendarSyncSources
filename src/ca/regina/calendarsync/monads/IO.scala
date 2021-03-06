package ca.regina.calendarsync.monads

sealed class IO[A](a: () => A) {
  def unsafePerformIO: A = a()
  def pure[A](a : A) = new IO[A](() => a)
  def map[B](f: A => B): IO[B] = pure[B](f(a()))
  def flatMap[B](f: A => IO[B]): IO[B] = f(a())
}

object IO {
  def apply[A](a: A) = new IO[A](() => a)
  
  implicit val ioMonad = new Monad[IO] {
    def pure[A](a: A) = new IO(() => a)
    def map[A, B](ioA: IO[A])(f: A => B) =  ioA.map(f)
    def flatMap[A, B](ioA: IO[A])(f: A => IO[B]) = ioA.flatMap(f)
  }
  
  def using[A <: android.database.Cursor, C](openHandle: IO[A])(f: A => IO[C]): IO[C] =
    for (cursor <- openHandle;
    		c <- f(cursor);
    		_ <- IO[Unit](cursor.close())
    ) yield c
    
  def using[A <: android.database.Cursor, C](openHandle: IO[Option[A]])(f: A => IO[C]): IO[Option[C]] = 
    for (cursorOption <- openHandle;
    		c <- cursorOption.map(f).map(_.map(Some(_))).getOrElse({IO(None)});
    		_ <- IO[Unit](cursorOption.map(_.close()))
    ) yield c
    
  def writeToLogVerbose(tag: String)(message: String): IO[Int] =  IO(android.util.Log.v(tag, message))
}