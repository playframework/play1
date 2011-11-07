package controllers;

import play.data.validation.*;

import models.*;

public class Topics extends Application {

    public static void show(Long forumId, Long topicId, Integer page) {
        Topic topic = Topic.findById(topicId);
        notFoundIfNull(topic);
        topic.views += 1;
        topic.save();
        render(topic, page);
    }

    @Secure
    public static void post(Long forumId) {
        Forum forum = Forum.findById(forumId);
        notFoundIfNull(forum);
        render(forum);
    }

    @Secure
    public static void create(Long forumId, @Required String subject, String content) {
        if (validation.hasErrors()) {
            validation.keep();
            params.flash();
            flash.error("Please correct these errors !");
            post(forumId);
        }
        Forum forum = Forum.findById(forumId);
        notFoundIfNull(forum);
        Topic newTopic = forum.newTopic(connectedUser(), subject, content);
        show(forumId, newTopic.id, null);
    }

    @Secure
    public static void reply(Long forumId, Long topicId) {
        Topic topic = Topic.findById(topicId);
        notFoundIfNull(topic);
        render(topic);
    }

    @Secure
    public static void createReply(Long forumId, Long topicId, String content) {
        Topic topic = Topic.findById(topicId);
        notFoundIfNull(topic);
        topic.reply(connectedUser(), content);
        show(forumId, topicId, null);
    }

    @Secure(admin = true)
    public static void delete(Long forumId, Long topicId) {
        Topic topic = Topic.findById(topicId);
        notFoundIfNull(topic);
        topic.delete();
        flash.success("The topic has been deleted");
        Forums.show(forumId, null);
    }
}

