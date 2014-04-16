package controllers;

import play.*;
import play.mvc.*;

import java.util.*;
import java.util.stream.Collectors;

import models.*;

public class Application extends Controller {

    public static void index() {
        List<Integer> doubles = Arrays.asList(1, 2, 3)
                .stream()
                .map(e -> e*2)
                .collect(Collectors.toList());
        render(doubles);
    }

}