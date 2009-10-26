package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class JPABinding extends Controller {

    public static void index() {
        render();
    }
    
    public static void create(Project project) {
        System.out.println(project);
        project.save();
        show(project.id);
    }
    
    public static void show(Long id) {
        Project project = Project.findById(id);
        render(project);
    }
    
    public static void save(Project project) {
        project.save();
        show(project.id);
    }
    
    public static void createCompany(Company company) {
        company.save();
        render(company);
    }
    
}

