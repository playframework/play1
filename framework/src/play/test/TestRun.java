
/*******************************************************************************
 * Copyright (c) 2018 Nosto Solutions Ltd All Rights Reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Nosto Solutions Ltd ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the agreement you entered into with
 * Nosto Solutions Ltd.
 ******************************************************************************/
package play.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Computer;
import org.junit.runner.Request;

public class TestRun {

    private final List<Class> classes;

    TestRun() {
         classes = new ArrayList<>();
    }

    public static TestRun parse() {
        TestRun result = new TestRun();
        result.classes.addAll(TestEngine.allUnitTests());
        return result;
    }

    public Request createRequest(Computer computer) {
        return Request.classes(computer, classes.toArray(new Class<?>[0]));
    }
}
