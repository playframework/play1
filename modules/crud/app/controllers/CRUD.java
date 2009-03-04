package controllers;

import java.util.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import javax.persistence.*;

import play.*;
import play.mvc.*;
import play.db.jpa.*;
import play.data.validation.*;
import play.exceptions.*;
import play.i18n.*;

public abstract class CRUD extends Controller {

    public static void index() {
        try {
            render();
        } catch (TemplateNotFoundException e) {
            render("CRUD/index.html");
        }
    }
	
    public static void list(int page, String search, String searchFields, String orderBy, String order) {
        if (page < 1) {
            page = 1;
        }
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        List<JPAModel> objects = type.findPage(page, search, searchFields, orderBy, order, params.get("where"));
        Long count = type.count(search, searchFields, params.get("where"));
        Long totalCount = type.count(null, null, params.get("where"));
        try {
            render(type, objects, count, totalCount, page, orderBy, order);
        } catch (TemplateNotFoundException e) {
            render("CRUD/list.html", type, objects, count, totalCount, page, orderBy, order);
        }
    }

    public static void show(Long id) {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        JPAModel object = type.findById(id);
        try {
            render(type, object);
        } catch (TemplateNotFoundException e) {
            render("CRUD/show.html", type, object);
        }
    }

	public static void attachment(Long id, String field) throws Exception {
		ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        JPAModel object = type.findById(id);
		FileAttachment attachment = (FileAttachment)object.getClass().getField(field).get(object);
		if(attachment == null) {
			notFound();
		}
		renderBinary(attachment.get(), attachment.filename);
	}

    public static void save(Long id) throws Exception {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        JPAModel object = type.findById(id);
        validation.valid(object.edit("object", params));
        if (validation.hasErrors()) {
            object.refresh();
            validation.keep();
            params.flash();
            flash.error(Messages.get("crud.hasErrors"));
            redirect(request.controller + ".show", object.id);
        }
        flash.success(Messages.get("crud.saved", type.modelName, object.id));
        if (params.get("_save") != null) {
            redirect(request.controller + ".list");
        }
        redirect(request.controller + ".show", object.id);
    }

    public static void blank() {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        try {
            render(type);
        } catch (TemplateNotFoundException e) {
            render("CRUD/blank.html", type);
        }
    }

    public static void create() throws Exception {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        JPAModel object = type.entityClass.newInstance();
        validation.valid(object.edit("object", params));
        if (validation.hasErrors()) {
            validation.keep();
            params.flash();
            flash.error(Messages.get("crud.hasErrors"));
            redirect(request.controller + ".blank");
        }
        object.save();
        flash.success(Messages.get("crud.created", type.name, object.id));
        if (params.get("_save") != null) {
            redirect(request.controller + ".list");
        }
        if (params.get("_saveAndAddAnother") != null) {
            redirect(request.controller + ".blank");
        }
        redirect(request.controller + ".show", object.id);
    }

    public static void delete(Long id) {
        ObjectType type = ObjectType.get(getControllerClass());
        notFoundIfNull(type);
        JPAModel object = type.findById(id);
        object.delete();
        flash.success(Messages.get("crud.deleted", type.name, object.id));
        redirect(request.controller + ".list");
    }

    // ~~~~~~~~~~~~~
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface For {

        Class value();
    }

    // ~~~~~~~~~~~~~
    static int getPageSize() {
        return Integer.parseInt(Play.configuration.getProperty("crud.pageSize", "30"));
    }

    public static class ObjectType {

        public Class<? extends CRUD> controllerClass;
        public Class<? extends JPAModel> entityClass;
        public String name;
        public String modelName;
        public String controllerName;

        public static ObjectType get(Class controllerClass) {
            Class entityClass = getEntityClassForController(controllerClass);
            if (entityClass == null || !JPAModel.class.isAssignableFrom(entityClass)) {
                return null;
            }
            ;
            ObjectType type = new ObjectType();
            type.name = controllerClass.getSimpleName();
            type.modelName = entityClass.getSimpleName();
            type.controllerName = controllerClass.getSimpleName().toLowerCase();
            type.entityClass = entityClass;
            type.controllerClass = controllerClass;
            return type;
        }

        public static Class getEntityClassForController(Class controllerClass) {
            if (controllerClass.isAnnotationPresent(For.class)) {
                return ((For) (controllerClass.getAnnotation(For.class))).value();
            }
            String name = controllerClass.getSimpleName();
            name = "models." + name.substring(0, name.length() - 1);
            try {
                return Play.classloader.loadClass(name);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        public Object getListAction() {
            return Router.reverse(controllerClass.getName() + ".list");
        }

        public Object getBlankAction() {
            return Router.reverse(controllerClass.getName() + ".blank");
        }

        public Long count(String search, String searchFields, String where) {
            String q = "select count(*) from " + entityClass.getSimpleName();
            if (search != null && !search.equals("")) {
                q += getSearchQuery(searchFields);
				q += (where != null ? " and "+where : "" );
            } else {
				q += (where != null ? " where "+where : "" );
			}
            Query query = JPA.getEntityManager().createQuery(q);
            if (search != null && !search.equals("") && q.indexOf("?1") != -1) {
                query.setParameter(1, "%" + search.toLowerCase() + "%");
            }
            return (Long) query.getSingleResult();
        }

        public List findPage(int page, String search, String searchFields, String orderBy, String order, String where) {
            int pageLength = getPageSize();
            String q = "from " + entityClass.getSimpleName();
            if (search != null && !search.equals("")) {
                q += getSearchQuery(searchFields);
				q += (where != null ? " and "+where : "" );
            } else {
				q += (where != null ? " where "+where : "" );
			}
            if (orderBy == null) {
                orderBy = "id";
            }
            if (order == null || (!order.equals("ASC") && !order.equals("DESC"))) {
                order = "ASC";
            }
            q += " order by " + orderBy + " " + order;
            Query query = JPA.getEntityManager().createQuery(q);
            if (search != null && !search.equals("") && q.indexOf("?1") != -1) {
                query.setParameter(1, "%" + search.toLowerCase() + "%");
            }
            query.setFirstResult((page - 1) * pageLength);
            query.setMaxResults(pageLength);
            return query.getResultList();
        }

        public String getSearchQuery(String searchFields) {
            List<String> fields = null;
            if (searchFields != null && !searchFields.equals("")) {
                fields = Arrays.asList(searchFields.split("[ ]"));
            }
            String q = "";
            for (ObjectField field : getFields()) {
                if (field.searchable && (fields == null ? true : fields.contains(field.name))) {
                    q += " or lower(" + field.name + ") like ?1";
                }
            }
            if (q.length() > 0) {
                q = " where " + q.substring(3);
            }
            return q;
        }

        public JPAModel findById(Long id) {
            return (JPAModel) JPA.getEntityManager().createQuery("from " + entityClass.getSimpleName() + " where id = " + id).getSingleResult();
        }

        public List<ObjectField> getFields() {
            List fields = new ArrayList();
            for (Field f : entityClass.getFields()) {
                ObjectField of = new ObjectField(f);
                if (of.type != null) {
                    fields.add(of);
                }
            }
            return fields;
        }

        public ObjectField getField(String name) {
            for (ObjectField field : getFields()) {
                if (field.name.equals(name)) {
                    return field;
                }
            }
            return null;
        }

        public static class ObjectField {

            public String type;
            public String name;
            public String relation;
            public boolean multiple;
            public boolean searchable;

            public ObjectField(Field field) {
                if (CharSequence.class.isAssignableFrom(field.getType())) {
                    type = "text";
                    searchable = true;
                    if (field.isAnnotationPresent(MaxSize.class)) {
                        int maxSize = field.getAnnotation(MaxSize.class).value();
                        if (maxSize > 100) {
                            type = "longtext";
                        }
                    }
                }
                if (Number.class.isAssignableFrom(field.getType())) {
                    type = "number";
                }
                if (Boolean.class.isAssignableFrom(field.getType())) {
                    type = "boolean";
                }
                if (Date.class.isAssignableFrom(field.getType())) {
                    type = "date";
                }
				if (FileAttachment.class.isAssignableFrom(field.getType())) {
                    type = "file";
                }
                if (JPAModel.class.isAssignableFrom(field.getType())) {
                    if (field.isAnnotationPresent(OneToOne.class)) {
                        if (field.getAnnotation(OneToOne.class).mappedBy().equals("")) {
                            type = "relation";
                            relation = field.getType().getSimpleName();
                        }
                    }
                    if (field.isAnnotationPresent(ManyToOne.class)) {
                        type = "relation";
                        relation = field.getType().getSimpleName();
                    }
                }
                if (Collection.class.isAssignableFrom(field.getType())) {
                    Class fieldType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    if (field.isAnnotationPresent(OneToMany.class)) {
                        if (field.getAnnotation(OneToMany.class).mappedBy().equals("")) {
                            type = "relation";
                            relation = fieldType.getSimpleName();
                            multiple = true;
                        }
                    }
                    if (field.isAnnotationPresent(ManyToMany.class)) {
                        if (field.getAnnotation(ManyToMany.class).mappedBy().equals("")) {
                            type = "relation";
                            relation = fieldType.getSimpleName();
                            multiple = true;
                        }
                    }
                }
                if (field.isAnnotationPresent(Id.class)) {
                    type = null;
                }
                name = field.getName();
            }
        }
    }
}

