package controllers;

import play.*;
import play.mvc.*;
import play.data.validation.*;

import java.util.*;

import models.*;

public class JPABinding extends Controller {

    public static void index() {
        render();
    }
    
    // #1195 Use @Valid to trigger the validation before the action-invoking 
    public static void create(@Valid Project project) {
        System.out.println(project);
        project.create();
        show(project.id);
    }
    
    public static void show(Long id) {
        Project project = Project.findById(id);
        render(project);
    }
    
    public static void save(Project project) {
        System.out.println("---> " + project.isPersistent());
        Logger.warn("Next warning is intended!");
        project.save();
        validation.keep();
        show(project.id);
    }
    
    public static void createCompany(Company company) {
        company.create();
        render(company);
    }
    
    public static void withMap(String companyId) {
        Company company = Company.findById(companyId);
        Project project = new Project();
        project.companies = new HashMap();
        project.companies.put(company.name, company);
        project.create();
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
    
    public static void aSaveForm() {
        long a = A.count();
        long b = B.count();
        render(a, b);
    }
    
    public static void aSubmitForm(@Valid A a) {
        System.out.println(a.id);
        System.out.println(a.b);
        System.out.println(a.b.id);
        System.out.println(a.b.name);
        aSaveForm();
    }
    
    public static void echoEntityBinding(@Valid A a) {
        renderText(String.format("a.id=%d, a.b.id=%d, a.b.name=%s", a.id, a.b.id, a.b.name));
    }
    
    
}

