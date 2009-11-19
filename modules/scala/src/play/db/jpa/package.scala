package play.db {

    package object jpa {

        // Let's shorten things a bit here internally
	import JPQL.{instance => i}
	type M[T] = Manifest[T]
	implicit private def manifest2entity[T](m: M[T]): String = m.erasure.getName()

	def em = JPA.em
	def count[T](implicit m: M[T]) = i.count(m)
	def count[T](q: String, ps: AnyRef*)(implicit m: M[T]) = i.count(m, q, ps.toArray)
	def findAll[T](implicit m: M[T]) = i.findAll(m)
	def findById[T](id: Any)(implicit m: M[T]) = i.findById(m, id).asInstanceOf[T]
	def findBy[T](q: String, ps: AnyRef*)(implicit m: M[T]) = i.findBy(m, q, ps.toArray)
	def find[T](q: String, ps: AnyRef*)(implicit m: M[T]) = new ScalaQuery[T](i.find(m, q, ps.toArray))
	def all[T](implicit m: M[T]) = i.all(m)
	def delete[T](q: String, ps: AnyRef*)(implicit m: M[T]) = i.delete(m, q, ps.toArray)
	def deleteAll[T](implicit m: M[T]) = i.deleteAll(m)
	def findOneBy[T <: JPASupport](q: String, ps: AnyRef*)(implicit m: M[T]): T = i.findOneBy(m, q, ps.toArray).asInstanceOf[T]
	def create[T <: JPASupport](name: String, ps: play.mvc.Scope.Params)(implicit m: M[T]): T = i.create(m, name, ps).asInstanceOf[T]

    }

}