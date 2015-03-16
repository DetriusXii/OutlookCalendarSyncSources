package ca.regina.calendarsync.monads

import ListExtensions._
import OptionExtensions._

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
        
  def enumEofT[E, M[_]](implicit M: Monad[M]): EnumeratorT[E, M] = new EnumeratorT[E, M] {
    def apply[A] = _.mapCont(_(Input.eofInput))
  }
  
  def enumCursor[A](
      columnNames: Array[String], arrayMapping: Array[String] => A)
      (cursor: android.database.Cursor)
  = {
    val columnIndexes = columnNames.toList.map(columnName => 
      Validation.tryCatchExpression[Exception, Int](() =>
      	cursor.getColumnIndexOrThrow(columnName)).map(Option(_)).getOrElse(_ => None))
      	
    def readColumnValues: Option[List[Option[String]]] = columnIndexes.sequence.map(_.map(columnIndex => {
      val columnValue = cursor.getString(columnIndex)
      if (columnValue == null) Some(columnValue)
      else None
    }))
    
    
    
    new EnumeratorT[A, IO] {
      def apply[B] = {
        cursor.moveToFirst()
        def go(s: StepT[A, IO, B]): IterateeT[A, IO, B] = {
          if (cursor.isBeforeFirst() || cursor.isAfterLast()) s.pointI
          else {
            val columnValuesOption = readColumnValues.map(_.sequence).flatten
            val elementOption = columnValuesOption.map(columnValues => arrayMapping(columnValues.toArray))
            elementOption match {
              case Some(element) => s.mapCont(k => {
                cursor.moveToNext()
                k(Input.elInput(element)) >>== go
              })
              case None => s.pointI 
            }
          }
        }
        go
      }
    }
  }
}