package ca.regina.calendarsync.monads

trait EnumeratorT[E, F[_]] {
  def apply[A]: StepT[E, F, A] => IterateeT[E, F, A]
}

object EnumeratorT {
  def enumIoSource[T, E, F[_]](get: () => IoExceptionOr[T], 
      gotdata: IoExceptionOr[T] => Boolean, 
      render: T => E)(implicit F: Monad[F]): EnumeratorT[IoExceptionOr[E], F] = 
        
        new EnumeratorT[IoExceptionOr[E], F] {
    	  def apply[A]  = (s: StepT[IoExceptionOr[E], F, A]) =>
          	s.mapCont(k => {
          	  val i = get()
          	  if (gotdata(i)) k(Input.elInput(i.map(render))) >>== apply[A]
          	  else s.pointI
          	})
      	}
        
  def doSomething(cursor: android.database.Cursor) = {
    
  }
  
  /*def enumCursor[E, F[_]](cursor: android.database.Cursor)(implicit F: Monad[F]): EnumeratorT[IoExceptionOr[E], F] =
    new EnumeratorT[IoExceptionOr[E], F] {
	  def apply[A] = {
	    def go(cursor2: android.database.Cursor)(s: StepT[E, F, A]): IterateeT[E, F, A] = 
	      if (cursor2.isBeforeFirst() || cursor2.isAfterLast()) s.pointI
	      else s.mapCont(k => {
	        val bool = cursor2.moveToNext()
	      })
	  }
  	}*/
}