package org.gradle.api.testing.fabric;

/**
 * @author Tom Eyckmans
 */
public interface TestProcessor {
    TestClassProcessResult process(TestClassRunInfo testClassRunInfo);
}
