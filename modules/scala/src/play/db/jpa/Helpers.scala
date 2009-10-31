package play.db.jpa

object Helpers {

    def jpql(query: String, args: Any*) = new SQuery(query, args:_*)

}

class SQuery(val jpql: String, val args: Any*) {

    val jpaQuery = new JPASupport.JPAQuery(JPA.em().createQuery(jpql))
    JPQLDialect.instance.bindParameters(jpaQuery.query, args.map(_.asInstanceOf[AnyRef]):_*)
    
    // ~~

    def first[T] = jpaQuery.first.asInstanceOf[T]
    def from(start: Int) = {
        jpaQuery.from(start)
        this
    }
    def fetch[T] = asList[T](jpaQuery.fetch())
    def fetch[T](size: Int) = asList[T](jpaQuery.fetch(size))

    // ~~

    private def asList[T](jlist: java.util.List[T]): List[T] = {
        import scala.collection.mutable.ListBuffer
        val buffer = ListBuffer[T]()
        for(e <- jlist.toArray) {
            buffer += e.asInstanceOf[T]
        }
        buffer.toList
    }

}