package utils;

import com.google.inject.*;

public class GuicyDummy extends AbstractModule {
    public void configure() {
        bind(TestInter.class).to(Test.class);
    }
}
