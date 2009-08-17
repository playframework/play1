package controllers;

import models.*;
import notifiers.*;
import play.*;
import play.mvc.*;
import java.util.Collection;

import play.data.validation.*;
import play.modules.gae.*;

public class Lists extends Application {
    
    @Before
    static void checkConnected() {
        if(GAE.getUser() == null) {
            Application.login();
        } else {
            renderArgs.put("user", GAE.getUser().getEmail());
        }
    }
    
    // ~~~~~

    public static void index() {
        Collection<List> lists = List.findByUser(getUser());
        render(lists);
    }
    
    public static void show(Long id) {
        List list = List.findById(id);
        notFoundIfNull(list);
        checkOwner(list);
        Collection<Item> items = list.items();
        Collection<Item> oldItems = list.oldItems();
        render(list, items, oldItems);
    }
    
    public static void blank() {
        render();
    }
    
    public static void create(@Required String name) {
        if(validation.hasErrors()) {
            flash.error("Oops, please give a name to your new list");
            blank();
        }
        new List(getUser(), name).insert();
        index();
    }
    
    public static void delete(Long id) {
        List list = List.findById(id);
        notFoundIfNull(list);
        checkOwner(list);
        list.delete();
        flash.success("The list %s has been deleted", list  );
        index();
    }
    
    public static void edit(Long id) {
        List list = List.findById(id);
        notFoundIfNull(list);
        checkOwner(list);
        render(list);
    }
    
    public static void save(Long id, @Required String name, String notes) {
        if(validation.hasErrors()) {
            params.flash();
            flash.error("Oops, please give a name to your list");
            edit(id);
        }
        List list = List.findById(id);
        notFoundIfNull(list);
        checkOwner(list);
        list.name = name;
        list.notes = notes;
        list.update();
        show(list.id);
    }
    
    public static void addItem(Long id, String label) {
        List list = List.findById(id);
        notFoundIfNull(list);
        checkOwner(list);
        new Item(list, label).insert();
        list.update(); // to keep last position up to date
        show(id);
    }
    
    public static void changeItemState(Long id, Long itemId, boolean done) {
        Item item = Item.findById(itemId);
        notFoundIfNull(item);
        checkOwner(item);
        item.done = done;
        item.position = item.list.nextPosition++;
        item.update();
        item.list.update();
        ok();
    }
    
    public static void deleteItem(Long id, Long itemId) {
        Item item = Item.findById(itemId);
        notFoundIfNull(item);
        checkOwner(item);
        item.delete();
        ok();
    }
    
    public static void reorderItems(Long id, String newOrder) {
        List list = List.findById(id);
        notFoundIfNull(list);
        checkOwner(list);
        list.nextPosition = 0;
        for(String p : newOrder.split(",")) {
            Item item = Item.findById(Long.parseLong(p));
            if(item.list.id.equals(id)) {
                item.position = list.nextPosition++;
                item.update();
            }
        }
        list.update();
        ok();
    }
    
    public static void email(Long id) {
        List list = List.findById(id);
        notFoundIfNull(list);
        checkOwner(list);
        Notifier.emailList(list);
        flash.success("This list has been emailed to %s", list.user);
        show(id);
    }
    
    // ~~~~~~ utils
    
    static String getUser() {
        return renderArgs.get("user", String.class);
    }
    
    static void checkOwner(List list) {
        if(!getUser().equals(list.user)) {
            forbidden();
        }
    }
    
    static void checkOwner(Item item) {
        item.list.get();
        checkOwner(item.list);
    }
    
}

