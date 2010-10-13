package controllers;

import play.db.Model;
import controllers.CRUD.ObjectType;
import controllers.OptimisticLockingCRUD.CustomizableObjectType;
import models.Book;

@CRUD.For(Book.class) 
public class TableCruds extends OptimisticLockingCRUD {
    public static CustomizableObjectType createObjectType(Class<? extends Model> entityClass) {
        final CustomizableObjectType type = OptimisticLockingCRUD.createObjectType(entityClass);
        type.addExcludedFields("exludedProperty", "notFound");
        return type;
    }
}
