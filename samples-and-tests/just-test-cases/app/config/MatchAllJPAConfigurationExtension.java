package config;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ejb.Ejb3Configuration;
import play.db.jpa.JPAConfigurationExtension;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class MatchAllJPAConfigurationExtension extends JPAConfigurationExtension {

    public static List<Ejb3Configuration> configurations = new ArrayList<Ejb3Configuration>();

    @Override
    public void configure(Ejb3Configuration cfg) {
        configurations.add(cfg);
    }
}
