package controllers;

import play.*;
import play.mvc.*;
import play.db.jpa.NoTransaction;
import models.World;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Application extends Controller {

    @NoTransaction
    public static void plaintext() {
        response.contentType = "text/plain";
        renderText("Hello, World!");
    }

    @NoTransaction
    public static void json() {
        Map<String, String> result = new HashMap<>();
        result.put("message", "Hello, World!");
        renderJSON(result);
    }

    public static void db() {
        int id = ThreadLocalRandom.current().nextInt(1, 10001);
        World world = World.findById(id);
        renderJSON(world);
    }

    public static void queries(int queries) {
        queries = Math.min(500, Math.max(1, queries));
        List<World> worlds = new ArrayList<>(queries);
        for (int i = 0; i < queries; i++) {
            int id = ThreadLocalRandom.current().nextInt(1, 10001);
            worlds.add((World) World.findById(id));
        }
        renderJSON(worlds);
    }

    public static void updates(int queries) {
        queries = Math.min(500, Math.max(1, queries));
        List<World> worlds = new ArrayList<>(queries);
        for (int i = 0; i < queries; i++) {
            int id = ThreadLocalRandom.current().nextInt(1, 10001);
            World world = World.findById(id);
            world.randomNumber = ThreadLocalRandom.current().nextInt(1, 10001);
            world.save();
            worlds.add(world);
        }
        renderJSON(worlds);
    }

}
