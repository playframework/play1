package controllers;

import play.mvc.*;
import play.db.*;
import play.db.jpa.*;
import play.Logger;

import models.*;
import models.otherdb.*;
import models.otherdb.sub.*;
import play.exceptions.JPAException;


public class Transactional extends Controller {

    // whatever we write here will not be committed
    @play.db.jpa.Transactional(readOnly=true)
    public static void readOnlyTest() {
        Post post = new Post();
        post.name = "TransactionalTest";
        Tag tag1 = new Tag();
        tag1.name = "TransactionalTest";
        Tag tag2 = new Tag();
        tag2.name = "TransactionalTest";
        post.tags.add(tag1);
        post.tags.add(tag2);
        post.save();
        // since this is read only the count will not go up with successive 
        // calls as the Post we just stored will be rolled back
        renderText("Wrote 1 post: total is now " + Post.count());
    }

    // defaults to readOnly=false, as this is what the class
    // is annotated with
    public static void writingTest() {
        Post post = new Post();
        post.name = "TransactionalTest";
        Tag tag1 = new Tag();
        tag1.name = "TransactionalTest";
        Tag tag2 = new Tag();
        tag2.name = "TransactionalTest";
        post.tags.add(tag1);
        post.tags.add(tag2);
        post.save();
        renderText("Wrote 1 post: total is now " + Post.count());
    }
    
    public static void echoHowManyPosts() {
        long cnt = Post.count();
        renderText("There are " + cnt + " posts");
    }

	//This should be excluded from any transactions.
	@play.db.jpa.NoTransaction
	public static void disabledTransactionTest() {
		// try to do JPA action and it will fail because of @NoTransaction
		try {
		    Post.count();
		} catch(JPAException e) {
		    renderText("isInsideTransaction: false");
		}
	}
	
	//This should automatically run inside transaction and return true
	public static void verifyIsInsideTransaction() {
	    // must execute JPA action to lazy initialize transaction
	    Post.count();
		renderText("isInsideTransaction: " + JPA.isInsideTransaction());
	}
	
	public static void useMultipleJPAConfigs() {
	    
	    Logger.info("Other db url: " + DB.getDBConfig("other").getUrl());
	    
	    EntityInOtherDb other = new EntityInOtherDb();
	    other.name = "test";
	    other.create();
	    
	    other = EntityInOtherDb.find("byName", "test").first();
	    
	    if (!other.name.equals("test")) {
	        renderText("failed");
	    }
	    
	    // do something with the default db also
	    Post.count();
	    
	    // other db again
	    long otherCount = EntityInOtherDb.count();
	    
	    // also check that Entity annotated via package-info works
	    Entity2InOtherDb other2 = new Entity2InOtherDb();
    	other2.name = "test2";
    	other2.create();

        other2 = Entity2InOtherDb.find("byName", "test2").first();

	    if (!other2.name.equals("test2")) {
	        renderText("failed2");
	    }
	    
	    // make sure Entity2InOtherDb resolved to correct JPAConfig
	    if (!other2.getJPAContext().getJPAConfig().equals(JPA.getJPAConfig("other")) ) {
	        renderText("package-info annotation resolving does not work");
	    }
	    
	    renderText("ok " + otherCount);
	}
	
}

