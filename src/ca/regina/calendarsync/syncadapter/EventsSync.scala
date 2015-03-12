package ca.regina.calendarsync.syncadapter

import ca.regina.calendarsync.monads._
import android.content.Context

class EventsSync(context: Context) {
  def openCursor(uri: android.net.Uri, calendarProjection: Array[String], selection: String, 
		  selectionArgs: Array[String]): OptionT[IO, android.database.Cursor] = {
    val cursor = context.getContentResolver().query(uri, calendarProjection, selection, selectionArgs, null)
    if (cursor != null) {
      OptionT(IO(Some(cursor)))
    } else {
      OptionT(IO(None))
    }
  }
		    
  def iterateeCursor[A]: IterateeT[A, IO, List[A]] = {
    def step(listOfElements: List[A]): Input[A] => IterateeT[A, IO, List[A]] = {
      case Element(x) => IterateeT(IO(StepT.scont(step(x :: listOfElements))))
      case Empty() => IterateeT(IO(StepT.scont(step(listOfElements))))
      case Eof() => IterateeT(IO(StepT.sdone(listOfElements, Eof())))
    }
    
    IterateeT(IO(StepT.scont(step(Nil))))
  }
		    
  
  def abstractQuery[A](sourceList: List[A],
      singleRowSelectionClause: A => String, 
      singleRowElementToValues: A => Array[String]) = {
    val singleRowSelectionClauses = sourceList map singleRowSelectionClause
    val multipleRowSelectionClause = singleRowSelectionClauses.foldLeft(("", true))((u, v) => 
      u._2 match {
        case true => (v, false)
        case false => ("%s and %s" format (u._1, v), false)
      })._1 
    
    val valuesList = sourceList.map(singleRowElementToValues(_)).flatten.toArray
    
    
    
    IO.using(new IO(() => context.))(f)
  }
  
	def insertAndUpdate(microsoftEvents: ListT[IO, MicrosoftEvent]): IO[Unit] = {
	  for (microsoftEvent <- microsoftEvents) yield ()
	}
}