import models.orphans.collections.BaseModel;
import models.orphans.collections.LevelOne;
import models.orphans.collections.LevelTwo;
import org.junit.Ignore;
import org.junit.Test;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.test.UnitTest;

public class JPABaseTest extends UnitTest {

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
