package play.db.jpa

class ScalaQuery[T](val query: JPASupport.JPAQuery) {

    def first = query.first().asInstanceOf[T]
    def fetch() = asList[T](query.fetch())
    def all = fetch()
    def fetch(size: Int) = asList[T](query.fetch(size))
    def from(offset: Int) = {
        query.from(offset)
        this
    }

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