package controllers.admin;

import java.lang.reflect.Field;
import java.util.List;

import javax.persistence.Version;

import models.Company;
import play.db.Model;
import play.exceptions.UnexpectedException;
import controllers.CRUD;

@CRUD.For(Company.class)
public class CustomAdminCompany extends CRUD {
    protected static ObjectType createObjectType(Class<? extends Model> entityClass) {
        return new VersionObjectType(entityClass);
    }
    
    public static class VersionObjectType extends ObjectType {
        
        private final String versionColumn;
        
        public VersionObjectType(Class<? extends Model> modelClass) {
            super(modelClass);
            versionColumn = getVersionColumnName(modelClass);
        }

        private String getVersionColumnName(Class modelClass) {
            Class c = modelClass;
            try {
                while (!c.equals(Object.class)) {
                    for (Field field : c.getDeclaredFields()) {
                        if (field.isAnnotationPresent(Version.class)) {
                            return field.getName();
                        }
                    }
                    c = c.getSuperclass();
                }
            } catch (Exception e) {
                throw new UnexpectedException("Error while determining the object @Version for an object of type " + modelClass);
            }
            return null;
        }

        @Override
        public List<ObjectField> getFields() {
            List<ObjectField> result = super.getFields();
            for (ObjectField objectField : result) {
                if (objectField.name.equals(versionColumn)) {
                    objectField.type = "hidden";
                }
            }
            return result;
        }

    }
    
}

