package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;
import play.db.Model;
import play.mvc.Http.Request;

public class OptimisticLockingCRUD extends CRUD {

    protected static CustomizableObjectType createObjectType(Class<? extends Model> entityClass) {
        return new CustomizableObjectType(entityClass);
    }
    
    
    public static class CustomizableObjectType extends ObjectType {

        private Set<String> hiddenFieldNames = new HashSet<String>();
        private Set<String> excludedFieldNames = new HashSet<String>();
        
        private String[] showFieldNames = null;
        private String[] blankFieldNames = null;
        
        private Map<String, ObjectField> fieldMap = new HashMap<String, ObjectField>();

        
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
        
        public void defineShowFields(String... showFields) {
            showFieldNames = showFields;
        }

        public void defineBlankFields(String... blankFields) {
            blankFieldNames = blankFields;
        }
        
        @Override
        public List<ObjectField> getFields() {
            String methodName = Request.current().actionMethod; 
            if ("blank".equals(methodName) && blankFieldNames != null) {
                return getFields(blankFieldNames);
            } else if ("show".equals(methodName) && showFieldNames != null) {
                return getFields(showFieldNames);
            } else {
                return getFieldsCommon();
            }
        }
        
        
        
        private List <ObjectField> getFields(String[] fieldnameList) {
            if (fieldMap.isEmpty()) {
                final List <ObjectField> fields = getFieldsCommon();
                for (ObjectField objectField : fields) {
                    fieldMap.put(objectField.name, objectField);    
                }
            }
            final List<ObjectField> result = new ArrayList<ObjectField>(fieldnameList.length);
            for (String fieldname : fieldnameList) {
                if (fieldMap.containsKey(fieldname)) {
                    result.add(fieldMap.get(fieldname));    
                } else {
                    Logger.warn("Unknown field with name >%s<", fieldname);
                }
                                    
            }
            return result;            
        }
        
        private List<ObjectField> getFieldsCommon() {
            final List <ObjectField> fields = super.getFields();
            final Set<String> hiddenFieldsNotChanged = new HashSet<String>(hiddenFieldNames);
            final Set<String> excludedFieldsNotChanged = new HashSet<String>(excludedFieldNames);            
            final List<ObjectField> hiddenFields = new ArrayList<ObjectField>();
            final List<ObjectField> normalFields = new ArrayList<ObjectField>();
            for (Iterator iterator = fields.iterator(); iterator.hasNext() && 
                    (!hiddenFieldsNotChanged.isEmpty() || !excludedFieldsNotChanged.isEmpty());) {
                final ObjectField field = (ObjectField) iterator.next();
                if (excludedFieldsNotChanged.remove(field.name)) {
                    Logger.debug("ignore " + field.name);
                    //Ignore this field.
                } else if (hiddenFieldsNotChanged.remove(field.name)) {
                    Logger.debug("hidden " + field.name);
                    field.type = "hidden";
                    hiddenFields.add(field);
                } else {
                    Logger.debug("normal " + field.name);
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
