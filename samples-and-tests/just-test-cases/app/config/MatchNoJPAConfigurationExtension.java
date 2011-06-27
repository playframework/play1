package config;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ejb.Ejb3Configuration;
import play.db.jpa.JPAConfigurationExtension;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class MatchNoJPAConfigurationExtension extends JPAConfigurationExtension {

    public static final String NON_EXISTING = "nonExisting";

    public static List<Ejb3Configuration> configuration = new ArrayList<Ejb3Configuration>();

    @Override
    public void configure(Ejb3Configuration cfg) {
        configuration.add(cfg);
    }

    @Override
    public String getConfigurationName() {
        return NON_EXISTING;
    }
}
