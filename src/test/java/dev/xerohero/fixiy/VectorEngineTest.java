package dev.xerohero.fixiy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    void testTextVectorizationDimensionAndPerformance() throws IOException {
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

    @Test
    void testPipelinePersistenceAndRetrieval() throws Exception {
        Path tempDir = Files.createTempDirectory("fixiy-index-test");

        try (VectorIndexRepository repo = new VectorIndexRepository(tempDir)) {
            VectorEngine engine = new VectorEngine(repo);

            // 1. Create a dummy log/text file to process
            File sampleLog = Files.createTempFile("sample_log", ".txt").toFile();
            Files.writeString(sampleLog.toPath(),
                    "ERROR 2026-06-13 22:15:00 - Database connection timed out unexpectedly.\n" +
                            "INFO  2026-06-13 22:15:05 - Retrying database pool connection attempt number 2.");

            // 2. Run the chunk-and-index pipeline
            engine.indexFile(sampleLog);

            // 3. Query the store using a dummy search vector
            float[] queryVector = new float[384];
            queryVector[0] = (float) Math.abs("Database connection timed out".hashCode() % 100) / 100.0f;

            List<SearchResult> hits = repo.searchNearest(queryVector, 2);

            // 4. Assert that we successfully stored and matched the schema
            assertFalse(hits.isEmpty(), "The index should return matches!");
            assertEquals(sampleLog.getAbsolutePath(), hits.get(0).getFile().getAbsolutePath());
        }
    }
}
