package controllers;

import models.Post;
import models.Tag;
import play.mvc.Controller;


@play.db.Transactional(readOnly=false)
public class Transactional extends Controller {

    // whatever we write here will not be committed
    @play.db.Transactional(readOnly=true)
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
}

