package play.test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

public class PlayJUnitRunnerTest {

    @Test
    public void testFilter() throws Exception {
//        PlayJUnitExtension runner = mock(PlayJUnitExtension.class);
//        doCallRealMethod().when(runner).filter(any());
//
//        runner.filter(new Filter() {
//
//            @Override
//            public boolean shouldRun(Description arg0) {
//                return arg0.getMethodName().indexOf("testFilter") > -1;
//            }
//
//            @Override
//            public String describe() {
//                return "";
//            }
//        });
//
//        when(runner.testCount()).thenCallRealMethod();
//        when(runner.getDescription()).thenCallRealMethod();
//        assertEquals(1, runner.testCount());
    }

}
