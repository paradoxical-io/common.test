package io.paradoxical.common.test.test;


import io.paradoxical.common.test.junit.RetryRule;
import org.junit.Rule;
import org.junit.Test;

public class LibraryTest extends TestBase {


    @Rule
    public RetryRule retry = new RetryRule(2);

    private int retryCount = 0;

    @Test
    public void test() {
        if (retryCount == 0) {
            retryCount++;
            throw new RuntimeException("Retry!");
        }

        // should succeed the second time
    }
}
