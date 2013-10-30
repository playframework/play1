package controllers;

import models.orphans.collections.BaseModel;
import models.orphans.collections.LevelOne;
import models.orphans.collections.LevelTwo;
import play.mvc.Controller;

public class CollectionOrphan extends Controller {

    public static void create() {
        BaseModel base = new BaseModel();

        LevelOne levelOne = new LevelOne();
        levelOne.baseModel = base;

        LevelTwo levelTwo = new LevelTwo();
        levelTwo.levelOne = levelOne;

        levelOne.levelTwos.add(levelTwo);

        base.levelOnes.add(levelOne);
        base.save();
        
        renderText(base.id);
    }

    public static void update(Long id) {
        BaseModel base = BaseModel.findById(id);
        base.levelOnes.remove(0);
        base.save();
    }

}
