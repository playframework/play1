import play.test._

import org.junit._
import org.scalatest.junit._
import org.scalatest._
import org.scalatest.matchers._

class JUnitStyle extends UnitTest with AssertionsForJUnit {
    
    @Before def setUp = Fixtures.deleteAll()
    
    @Test def verifyEasy {
        assert("A" == "A")
        intercept[StringIndexOutOfBoundsException] {
            "concise".charAt(-1)
        }
    }
    
}

class JUnitStyleWithShould extends UnitTest with ShouldMatchersForJUnit {
    
    @Before def setUp = Fixtures.deleteAll()
    
    @Test def verifyEasy {        
        val name = "Guillaume"        
        name should be ("Guillaume")       
        evaluating { "name".charAt(-1) } should produce [StringIndexOutOfBoundsException]       
        name should have length (9)       
        name should include ("i")       
        name.length should not be < (8)       
        name should not startWith ("Hello")
    }
    
}

class FunctionsSuiteStyle extends UnitTest with FunSuite with ShouldMatchers {
    
    Fixtures.deleteAll()
    
    test("Hello...") (pending)
    
    test("1 + 1") {        
        (1 + 1) should be (2)        
    }
    
    test("Something") {
        "Guillaume" should not include ("X")
    }
    
    test("1 + 1 again") {        
        (1 + 1) should be (2)       
    }
    
}

class SpecStyle extends UnitTest with FlatSpec with ShouldMatchers {

    val name = "Hello World"

    "'Hello World'" should "not contain the X letter" in {
        name should not include ("X")
    }

    it should "have 11 chars" in {
        name should have length (11)      
    }
    
}

class FeatureStyle extends UnitTest with FeatureSpec {
 
    feature("The user can pop an element off the top of the stack") { 
        scenario("pop is invoked on a non-empty stack") (pending) 
        scenario("pop is invoked on an empty stack") (pending)
    }
  
}