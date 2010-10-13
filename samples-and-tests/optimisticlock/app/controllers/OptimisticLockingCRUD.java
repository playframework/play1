package controllers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import controllers.CRUD.ObjectType.ObjectField;

import play.Logger;
import play.classloading.enhancers.ControllersEnhancer.ByPass;
import play.db.Model;

public class OptimisticLockingCRUD extends CRUD {

    @ByPass
    public static CustomizableObjectType createObjectType(Class<? extends Model> entityClass) {
        return new CustomizableObjectType(entityClass);
    }
    
    
    public static class CustomizableObjectType extends ObjectType {

        private static Set<String> hiddenFieldNames = new HashSet<String>();
        private static Set<String> excludedFieldNames = new HashSet<String>();
        
        public CustomizableObjectType(Class<? extends Model> modelClass) {
            super(modelClass);
            hiddenFieldNames.add("version");
        }

        public CustomizableObjectType(String modelClass)
                throws ClassNotFoundException {
            super(modelClass);
        }
        
        public void addHiddenFields(String... hiddenFields) {
            for (String hiddenField : hiddenFields) {
                hiddenFieldNames.add(hiddenField);
            }
        }

        public void addExcludedFields(String... excludedFields) {
            for (String excludedField : excludedFields) {
                excludedFieldNames.add(excludedField);
            }
        }

        @Override
        public List<ObjectField> getFields() {
            List <ObjectField> fields = super.getFields();
            final Set<String> hiddenFieldsNotChanged = new HashSet<String>(hiddenFieldNames);
            final Set<String> excludedFieldsNotChanged = new HashSet<String>(excludedFieldNames);            
            final List<ObjectField> hiddenFields = new ArrayList<ObjectField>();
            final List<ObjectField> normalFields = new ArrayList<ObjectField>();
            for (Iterator iterator = fields.iterator(); iterator.hasNext() && 
                    (!hiddenFieldsNotChanged.isEmpty() || !excludedFieldsNotChanged.isEmpty());) {
                final ObjectField field = (ObjectField) iterator.next();
                if (excludedFieldsNotChanged.remove(field.name)) {
                    //Ignore this field.
                } else if (hiddenFieldsNotChanged.remove(field.name)) {
                    field.type = "hidden";
                    hiddenFields.add(field);
                } else {
                    normalFields.add(field);
                }
            }
            hiddenFields.addAll(normalFields);
            if (!hiddenFieldsNotChanged.isEmpty()) {
                final StringBuilder message = new StringBuilder(
                        "Not all hidden fields was found in model: ");
                for (String hiddenFieldName : hiddenFieldsNotChanged) {
                    message.append(hiddenFieldName).append(", ");
                }
                Logger.warn(message.toString());
            }
            if (!excludedFieldsNotChanged.isEmpty()) {
                final StringBuilder message = new StringBuilder(
                        "Not all excluded fields was found in model: ");
                for (String excludedField : excludedFieldsNotChanged) {
                    message.append(excludedField).append(", ");
                }
                Logger.warn(message.toString());
            }
            return hiddenFields;
        }
        
        

    }
}
