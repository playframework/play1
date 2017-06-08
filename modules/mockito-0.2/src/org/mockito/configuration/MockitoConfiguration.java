package org.mockito.configuration;

import play.Logger;

public class MockitoConfiguration extends DefaultMockitoConfiguration {
    
    public MockitoConfiguration() {
        super();
        Logger.debug("Instantiated Mockito Configuration From play-mockito");
    }
    
    @Override
    public boolean enableClassCache() {
        return false;
    }
}
