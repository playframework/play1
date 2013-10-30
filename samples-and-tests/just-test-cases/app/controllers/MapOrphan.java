package controllers;

import models.orphans.maps.BaseModel;
import models.orphans.maps.LevelOne;
import models.orphans.maps.LevelTwo;
import play.mvc.Controller;

public class MapOrphan extends Controller {

    public static void create() {
        BaseModel base = new BaseModel();

        LevelOne levelOne = new LevelOne();
        levelOne.mapKey = "levelOne";
        levelOne.baseModel = base;

        LevelTwo levelTwo = new LevelTwo();
        levelTwo.mapKey = "levelTwo";
        levelTwo.levelOne = levelOne;

        levelOne.levelTwoMap.put(levelTwo.mapKey, levelTwo);
        base.levelOneMap.put(levelOne.mapKey, levelOne);

        base.save();

        renderText(base.id);
    }

    public static void update(Long id) {
        BaseModel base = BaseModel.findById(id);
        base.levelOneMap.remove("levelOne");
        base.save();
    }
}
