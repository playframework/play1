import models.orphans.collections.BaseModel;
import models.orphans.collections.LevelOne;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.UnitTest;

public class JPAPluginTest extends UnitTest {
    @Test
    public void testTransientError() {
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

        // now clear them out and add new ones

        bmA.levelOnes.clear();
        bmB.levelOnes.clear();

        levelOneA = new LevelOne();
        levelOneA.baseModel = bmA;
        bmA.levelOnes.add(levelOneA);

        levelOneB = new LevelOne();
        levelOneB.baseModel = bmB;
        bmB.levelOnes.add(levelOneB);

        bmA.save();
        bmB.save();

    }
}
