package controllers;

import java.util.*;

import play.mvc.*;

import models.*;

public class Application extends Controller {

    public static void index(String category, String tagCodes) {
        List<Category> categories = Category.findAll();
        if (category == null && categories.size() > 0) {
            index(categories.get(0).code, null);
        }
        String[] filterTags = tagCodes != null ? tagCodes.split(",") : new String[0];
        if ("all".equals(category)) {
            category = null;
        }
        List jobs = Job.findByCategoryAndTags(category, filterTags);
        List tags = Tag.findByCategory(category);
        render(categories, category, jobs, tags, filterTags);
    }

    public static void jobdetails(Long jobid) {
        Job job = Job.findById(jobid);
        notFoundIfNull(job);
        String category = job.category.code;
        List categories = Category.findAll();
        render(categories, category, job);
    }

    public static void companyLogo(Long id) {
        Company company = Company.findById(id);
        renderBinary(company.logo.get());
    }

    public static void search(String by) {
        if (by == null || by.trim().equals("")) {
            index(null, null);
        }
        List jobs = Job.search(by);
        render(jobs, by);
    }
    
}