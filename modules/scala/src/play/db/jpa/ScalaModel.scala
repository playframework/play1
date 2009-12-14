package play.db.jpa

trait ScalaModel[T] extends play.db.jpa.JPAModel with QuerySupport[T]
