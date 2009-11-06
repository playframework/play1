package play.db.jpa
import scala.reflect.Manifest

trait QueryMethods {
	def find(query: String, params: AnyRef*) = {
		val entityName: String = this.getClass().getName().stripSuffix("$")
		JPQL.instance.find(entityName, query, params.toArray)
	}
}