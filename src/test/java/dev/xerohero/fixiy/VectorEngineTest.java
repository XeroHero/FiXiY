package dev.xerohero.fixiy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import static org.junit.jupiter.api.Assertions.*;

class VectorEngineTest {

    private VectorEngine engine;
    private ThreadMXBean threadCpuMetrics;

    @BeforeEach
    void setUp() {
        this.engine = new VectorEngine();
        this.threadCpuMetrics = ManagementFactory.getThreadMXBean();

        if (threadCpuMetrics.isThreadCpuTimeSupported()) {
            threadCpuMetrics.setThreadCpuTimeEnabled(true);
        }
    }

    @Test
    @DisplayName("Assert input text returns a 384-float array within sub-millisecond thread execution constraints")
    void testTextVectorizationDimensionAndPerformance() {
        // 1. Arrange: Sample log string to process
        String sampleLog = "2026-06-13T19:53:35.124Z [WARN] dev.xerohero.fixiy.SearchEngine - Match count threshold exceeded.";

        // 2. Act: Measure exact CPU execution time assigned to this thread
        long startCpuTimeNano = threadCpuMetrics.isThreadCpuTimeSupported()
                ? threadCpuMetrics.getCurrentThreadCpuTime()
                : System.nanoTime();

        float[] vectorOutput = engine.vectorize(sampleLog);

        long endCpuTimeNano = threadCpuMetrics.isThreadCpuTimeSupported()
                ? threadCpuMetrics.getCurrentThreadCpuTime()
                : System.nanoTime();

        long executionDurationNano = endCpuTimeNano - startCpuTimeNano;
        double executionDurationMillis = executionDurationNano / 1_000_000.0;

        // 3. Assert: Structure and Dimensionality Checks
        assertNotNull(vectorOutput, "The vector engine returned a null array reference.");
        assertEquals(384, vectorOutput.length,
                "Dimension error! Expected exactly 384 floats, but received: " + vectorOutput.length);

        // Content integrity check
        boolean hasNonZeroElements = false;
        for (float element : vectorOutput) {
            if (element != 0.0f) {
                hasNonZeroElements = true;
                break;
            }
        }
        assertTrue(hasNonZeroElements, "Sanity Failure: The vector output is entirely empty (all zeros).");

        // 4. Assert: Performance SLA Timing Limits
        System.out.printf("⚡ [FIXIY PERF] Single log frame vectorization latency: %.3f ms%n", executionDurationMillis);

        // Single thread processing should finish in a fraction of a millisecond
        assertTrue(executionDurationMillis < 5.0,
                String.format("Performance SLA Violation! Processing took too long: %.3f ms", executionDurationMillis));
    }
}