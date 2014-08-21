import models.orphans.collections.BaseModel;
import models.orphans.collections.LevelOne;
import models.orphans.collections.LevelTwo;
import models.zoo.Animal;
import models.zoo.Meal;
import models.zoo.Zoo;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.test.Fixtures;
import play.test.UnitTest;


public class JPAPluginTest extends UnitTest {

    @Before
    public void cleanup() {
	Fixtures.deleteDatabase();
    }
    
    private void commit() {
	try {
	    JPA.em().getTransaction().commit();
	} finally {
	    JPA.em().getTransaction().begin();
	}
    }

    private void commitAndClear() {
	try {
	    JPAPlugin.closeTx(false);
	} finally {
	    JPAPlugin.startTx(false);
	}
    }

    @Ignore("wait for full fix")
    @Test
    public void testOrphanRemovalDepDeletion() {
	final Zoo zoo = new Zoo().save();
	zoo.lion = new Animal();
	zoo.save();
	zoo.lion = null; // lion escaped from zoo
	zoo.save(); // this line causes a violation exception: seems to be due
		    // to a Hibernate bug (see
		    // http://stackoverflow.com/questions/20280271/hibernate-jpa-onetoone-orphan-removal-still-not-working-as-of-4-2-7-4-3-0-cr1)
	assertEquals("There should be no animal left", 0, Animal.count());
	assertEquals("There should not be any meals left", 0, Meal.count());
    }
    
    @Test
    public void testTransientErrorAddingSeparatly() {
	BaseModel bmA = new BaseModel();

	LevelOne levelOneA = new LevelOne();
	levelOneA.baseModel = bmA;
	bmA.levelOnes.add(levelOneA);

	BaseModel bmB = new BaseModel();

	LevelOne levelOneB = new LevelOne();
	levelOneB.baseModel = bmB;
	bmB.levelOnes.add(levelOneB);

	bmA.save();
	bmB.save();
	
	assertEquals("We must have 2 LevelOne", 2L, LevelOne.count("baseModel = ?1 OR baseModel = ?2", bmA, bmB));

	// now clear them out
	bmA.levelOnes.clear();
	bmB.levelOnes.clear();

	bmA.save();
	bmB.save();
	
	assertEquals("We must have 0 LevelOne", 0L, LevelOne.count("baseModel = ?1 OR baseModel = ?2", bmA, bmB));

	// now add new ones 
	levelOneA = new LevelOne();
	levelOneA.baseModel = bmA;
	bmA.levelOnes.add(levelOneA);
	bmA.save();
	
	levelOneB = new LevelOne();
	levelOneB.baseModel = bmB;
	bmB.levelOnes.add(levelOneB);

	bmB.save();
	assertEquals("We must have 2 LevelOne", 2L, LevelOne.count("baseModel = ?1 OR baseModel = ?2", bmA, bmB));
    }
    
    @Ignore("wait for full fix")
    @Test
    public void testTransientErrorAddingTogether() {
	BaseModel bmA = new BaseModel();

	LevelOne levelOneA = new LevelOne();
	levelOneA.baseModel = bmA;
	bmA.levelOnes.add(levelOneA);

	BaseModel bmB = new BaseModel();

	LevelOne levelOneB = new LevelOne();
	levelOneB.baseModel = bmB;
	bmB.levelOnes.add(levelOneB);

	bmA.save();
	bmB.save();
	
	assertEquals("We must have 2 LevelOne", 2L, LevelOne.count("baseModel = ?1 OR baseModel = ?2", bmA, bmB));

	// now clear them out
	bmA.levelOnes.clear();
	bmB.levelOnes.clear();

	bmA.save();
	bmB.save();
	
	assertEquals("We must have 0 LevelOne", 0L, LevelOne.count("baseModel = ?1 OR baseModel = ?2", bmA, bmB));

	// now add new ones 
	levelOneA = new LevelOne();
	levelOneA.baseModel = bmA;
	bmA.levelOnes.add(levelOneA);
	
	levelOneB = new LevelOne();
	levelOneB.baseModel = bmB;
	bmB.levelOnes.add(levelOneB);

	bmA.save();
	bmB.save();
	assertEquals("We must have 2 LevelOne", 2L, LevelOne.count("baseModel = ?1 OR baseModel = ?2", bmA, bmB));
    }
    
    @Test
    public void testCollectionOwnerError() {
	BaseModel bmA = new BaseModel();
	bmA.save();
	commitAndClear();

	bmA = BaseModel.findById(bmA.id);

	LevelOne levelOneA = new LevelOne();
	levelOneA.baseModel = bmA;
	bmA.levelOnes.add(levelOneA);

	LevelTwo levelTwo = new LevelTwo();
	levelTwo.levelOne = levelOneA;
	levelOneA.levelTwos.add(levelTwo);

	LevelOne levelOneB = new LevelOne();
	levelOneB.baseModel = bmA;
	bmA.levelOnes.add(levelOneB);

	bmA.save();
	commitAndClear();

	bmA = BaseModel.findById(bmA.id);

	LevelOne removed = bmA.levelOnes.remove(0);
	bmA.save();

	JPA.em().flush(); // where bug actually occurs
    }

    @Test
    public void testDuplicateError() {
	BaseModel bmA = new BaseModel();
	bmA.save();
	commit();

	LevelOne levelOneA = new LevelOne();
	levelOneA.baseModel = bmA;
	bmA.levelOnes.add(levelOneA);

	BaseModel bmB = new BaseModel();
	bmB.parent = levelOneA;
	levelOneA.children.add(bmB);

	bmA.save();
	commit();
    }
}
