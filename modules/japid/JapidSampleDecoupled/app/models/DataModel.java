package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

/**
 * @author Erdinc YILMAZEL
 * @since 1/30/11
 */
public class DataModel {
   public static DataModel dataModel = new DataModel(100, 1000, 1500);

   public static class User {
      int userId;
      String userName;
      String email;
      boolean active;
      Date registryDate;

      ArrayList<User> friends;

      public int getUserId() {
         return userId;
      }

      public String getUserName() {
         return userName;
      }

      public String getEmail() {
         return email;
      }

      public boolean isActive() {
         return active;
      }

      public ArrayList<User> getFriends() {
         return friends;
      }

      public Date getRegistryDate() {
         return registryDate;
      }

      public void setUserId(int userId) {
         this.userId = userId;
      }

      public void setUserName(String userName) {
         this.userName = userName;
      }

      public void setEmail(String email) {
         this.email = email;
      }

      public void setActive(boolean active) {
         this.active = active;
      }

      public void setRegistryDate(Date registryDate) {
         this.registryDate = registryDate;
      }

      public void setFriends(ArrayList<User> friends) {
         this.friends = friends;
      }
   }

   public static class Entry implements Comparable<Entry> {
      int entryId;
      String title;
      String body;
      User owner;
      Date entryDate;
      ArrayList<Comment> comments;

      public int compareTo(Entry o) {
         return entryDate.compareTo(o.entryDate);
      }

      public int getEntryId() {
         return entryId;
      }

      public String getTitle() {
         return title;
      }

      public String getBody() {
         return body;
      }

      public User getOwner() {
         return owner;
      }

      public Date getEntryDate() {
         return entryDate;
      }

      public ArrayList<Comment> getComments() {
         return comments;
      }

      public void setEntryId(int entryId) {
         this.entryId = entryId;
      }

      public void setTitle(String title) {
         this.title = title;
      }

      public void setBody(String body) {
         this.body = body;
      }

      public void setOwner(User owner) {
         this.owner = owner;
      }

      public void setEntryDate(Date entryDate) {
         this.entryDate = entryDate;
      }

      public void setComments(ArrayList<Comment> comments) {
         this.comments = comments;
      }
   }

   public static class Comment {
      int commentId;
      Entry entry;
      User owner;
      String commentText;

      public int getCommentId() {
         return commentId;
      }

      public Entry getEntry() {
         return entry;
      }

      public User getOwner() {
         return owner;
      }

      public String getCommentText() {
         return commentText;
      }

      public void setCommentId(int commentId) {
         this.commentId = commentId;
      }

      public void setEntry(Entry entry) {
         this.entry = entry;
      }

      public void setOwner(User owner) {
         this.owner = owner;
      }

      public void setCommentText(String commentText) {
         this.commentText = commentText;
      }
   }

   public HashMap<Integer, User> userList;
   public HashMap<Integer, Entry> entryList;
   public ArrayList<Entry> entries;
   public User loggedInUser;
   final int userCount;
   final int entryCount;
   final int commentCount;
   final Random random = new Random();

   public DataModel(int userCount, int entryCount, int commentCount) {
      this.userCount = userCount;
      this.entryCount = entryCount;
      this.commentCount = commentCount;

      userList = new HashMap<Integer, User>();
      entryList = new HashMap<Integer, Entry>();
      entries = new ArrayList<Entry>();

      // Create Users
      for (int i = 0; i < userCount; i++) {
         User user = new User();
         user.userId = i + 10001;
         user.userName = "User" + user.userId;
         user.active = random.nextInt(10) < 3;
         user.email = user.userName + "@domain.com";
         user.friends = new ArrayList<User>();

         userList.put(user.userId, user);
      }

      // Create Friends List
      for (int i = 0; i < userCount; i++) {
         int userId = i + 10001;
         User user = userList.get(userId);
         HashSet<Integer> friends = new HashSet<Integer>();
         friends.add(user.userId);
         int size = random.nextInt(5) + 1;
         while (friends.size() < size) {
            int friendId = random.nextInt(userCount) + 10001;
            if (!friends.contains(friendId)) {
               friends.add(friendId);
               user.friends.add(userList.get(friendId));
            }
         }
      }

      long oneMonth = 30 * 24 * 60 * 60 * 1000L;
      long aMonthAgo = System.currentTimeMillis() - oneMonth;

      for (int i = 0; i < entryCount; i++) {
         Entry entry = new Entry();
         entry.entryId = i + 10001;
         entry.title = "Entry title " + entry.entryId;
         entry.body = "Entry body " + entry.entryId;
         entry.owner = getRandomUser();
         entry.entryDate = new Date(aMonthAgo + random.nextInt((int) (oneMonth / 1000L)));
         entry.comments = new ArrayList<Comment>();
         entryList.put(entry.entryId, entry);
         entries.add(entry);
      }

      Collections.sort(entries);

      for (int i = 0; i < commentCount; i++) {
         Comment comment = new Comment();
         comment.commentId = i + 10001;
         comment.owner = getRandomUser();
         comment.entry = entryList.get(random.nextInt(entryCount) + 10001);
         comment.commentText = "Comment text " + comment.commentId;

         comment.entry.comments.add(comment);
      }

      loggedInUser = getRandomUser();
   }

   public User getRandomUser() {
      return userList.get(random.nextInt(userCount) + 10001);
   }

   public HashMap<Integer, User> getUserList() {
      return userList;
   }

   public HashMap<Integer, Entry> getEntryList() {
      return entryList;
   }

   public ArrayList<Entry> getEntries() {
      return entries;
   }

   public User getUser(int userId) {
      return userList.get(userId);
   }

   public Entry getEntry(int entryId) {
      return entries.get(entryId);
   }

   public User getLoggedInUser() {
      return loggedInUser;
   }
}
