package play.data.parsing;

import org.junit.Test;
import play.mvc.Http;
import play.test.FunctionalTest;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Copyright (C) 2009-2016 Lightbend Inc. <http://www.lightbend.com>
 */
public class UrlEncodedParserTest {

    @Test
    public void parseTest() {

        Map<String, String[]> params = new HashMap<>();

        params.put("english", new String[] {"Hello"});
        params.put("russian", new String[] {"Привет"});
        params.put("chinese", new String[] {"嗨"});

        String queryString = params.entrySet()
                                   .stream()
                                   .map(entry -> entry.getKey() + "=" + entry.getValue()[0])
                                   .collect(Collectors.joining("&"));

        params.put("body", new String[] {queryString});

        Http.Request req = FunctionalTest.newRequest();
        Http.Request.current.set(req);

        Map<String, String[]> parse = UrlEncodedParser.parse(queryString);

        assertThat(parse.size(), is(params.size()));
        assertThat(transform(parse), is(transform(params)));
    }


    private Map<String, String> transform(Map<String, String[]> map) {
        // arrays in map values fail during asserting
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
    }
}