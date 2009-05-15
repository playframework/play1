package controllers;

import models.*;
import play.*;
import play.mvc.*;
import java.util.*;

public class Topics extends Application {
	
	public static void show(Long forumId, Long topicId, Integer page) {
		Topic topic = Topic.findById(topicId);
		notFoundIfNull(topic);
		topic.views += 1;
		render(topic, page);
	}

	@Secure
	public static void post(Long forumId) {
		Forum forum = Forum.findById(forumId);
		notFoundIfNull(forum);
		render(forum);
	}
	
	@Secure
	public static void create(Long forumId, String subject, String content) {
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
	
	@Secure(admin=true)
	public static void delete(Long forumId, Long topicId) {
		Topic topic = Topic.findById(topicId);
		notFoundIfNull(topic);
		topic.delete();
		Forums.show(forumId, null);
	}
    
}

