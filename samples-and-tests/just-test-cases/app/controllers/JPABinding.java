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
        validation.keep();
        show(project.id);
    }
    
    public static void createCompany(Company company) {
        company.save();
        render(company);
    }
    
    public static void withMap(String companyId) {
        Company company = Company.findById(companyId);
        Project project = new Project();
        project.companies = new HashMap();
        project.companies.put(company.name, company);
        project.save();
        render("@show", project);
    }
    
    public static void editMap(Long id) {
        Project project = Project.findById(id);
        project.companies.get("zenexity").name = "Coucou";
        show(id);
    }
    
    public static void editMapAndSave(Long id) {
        Project project = Project.findById(id);
        project.companies.get("zenexity").name = "Coucou";
        project.save();
        show(id);
    }
    
}

