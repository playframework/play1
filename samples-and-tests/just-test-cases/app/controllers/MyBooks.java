package controllers;

import play.db.Model;
import controllers.CRUD.ObjectType;
import controllers.OptimisticLockingCRUD.CustomizableObjectType;
import models.MyBook;

public class MyBooks extends OptimisticLockingCRUD {
    public static CustomizableObjectType createObjectType(Class<? extends Model> entityClass) {
        final CustomizableObjectType type = OptimisticLockingCRUD.createObjectType(entityClass);
        type.addExcludedFields("excludedProperty", "notFound");
        return type;
    }
}
