package org.gradle.api.testing.pipelinesplit;

import org.gradle.api.testing.execution.Pipeline;
import org.gradle.api.testing.fabric.TestClassRunInfo;
import org.gradle.api.testing.pipelinesplit.policies.SplitPolicyMatcher;
import org.gradle.util.queues.AbstractBlockingQueueItemConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Tom Eyckmans
 */
public class PipelineSplitWorker extends AbstractBlockingQueueItemConsumer<TestClassRunInfo> {
    private static final Logger logger = LoggerFactory.getLogger(PipelineSplitWorker.class);

    private final TestPipelineSplitOrchestrator splitOrchestrator;
    private final AtomicLong matchCount = new AtomicLong(0);
    private final AtomicLong discardedCount = new AtomicLong(0);
    private final List<SplitPolicyMatcher> splitPolicyMatchers;
    private Map<SplitPolicyMatcher, Pipeline> pipelineMatchers;

    public PipelineSplitWorker(TestPipelineSplitOrchestrator splitOrchestrator, BlockingQueue<TestClassRunInfo> toConsumeQueue, long pollTimeout, TimeUnit pollTimeoutTimeUnit, List<SplitPolicyMatcher> splitPolicyMatchers, Map<SplitPolicyMatcher, Pipeline> pipelineMatchers) {
        super(toConsumeQueue, pollTimeout, pollTimeoutTimeUnit);
        this.splitOrchestrator = splitOrchestrator;
        this.splitPolicyMatchers = splitPolicyMatchers;
        this.pipelineMatchers = pipelineMatchers;
    }

    public void setUp() {
        splitOrchestrator.splitWorkerStarted(this);
    }

    protected boolean consume(TestClassRunInfo queueItem) {
        logger.debug("[pipeline-splitting >> test-run] {}", queueItem.getTestClassName());

        SplitPolicyMatcher matcher = null;

        final Iterator<SplitPolicyMatcher> matcherIterator = splitPolicyMatchers.iterator();
        while (matcher == null && matcherIterator.hasNext()) {
            final SplitPolicyMatcher currentMatcher = matcherIterator.next();
            if (currentMatcher.match(queueItem)) {
                matcher = currentMatcher;
            }
        }

        if (matcher != null) {
            pipelineMatchers.get(matcher).addTestClassRunInfo(queueItem);
            matchCount.incrementAndGet();
        } else {
            discardedCount.incrementAndGet();
        }

        return false; // don't stop
    }

    protected void tearDown() {
        logger.debug("[split-worker-match-count] " + matchCount.get());
        logger.debug("[split-worker-discarded-count] " + discardedCount.get());

        splitOrchestrator.splitWorkerStopped(this);
    }
}
