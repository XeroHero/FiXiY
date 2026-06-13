import dev.xerohero.fixiy.SearchResult;
import dev.xerohero.fixiy.VectorEngine;
import dev.xerohero.fixiy.VectorIndexRepository;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class VectorEngineTest {
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
