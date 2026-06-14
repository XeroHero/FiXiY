package dev.xerohero.fixiy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class VectorEngine {

    private final VectorIndexRepository repository;

    // A standard 500-character chunk size works great for logs and code files
    private static final int CHUNK_SIZE = 500;

    public VectorEngine(VectorIndexRepository repository) {
        this.repository = repository;
    }

    /**
     * Reads a file, splits it into chunks, generates embeddings, and saves to Lucene.
     */
    public void indexFile(File file) throws IOException {
        String content = Files.readString(file.toPath());
        String filePath = file.getAbsolutePath();

        // Simple fixed-character moving window chunker
        int chunkId = 0;
        int index = 0;

        while (index < content.length()) {
            int endIndex = Math.min(index + CHUNK_SIZE, content.length());
            String textChunk = content.substring(index, endIndex).trim();

            if (!textChunk.isEmpty()) {
                // 1. Generate the 384-dimensional embedding for this chunk
                float[] embedding = generateEmbedding(textChunk);

                // 2. Persist to our embedded storage using our strict schema
                repository.addChunk(filePath, chunkId, textChunk, embedding);
                chunkId++;
            }
            index += CHUNK_SIZE;
        }

        // Commit changes to disk so they are instantly searchable
        repository.commit();
    }

    /**
     * Dummy placeholder matching your performance test format.
     * Replace with your actual LangChain4j / ONNX model call tomorrow.
     */
    private float[] generateEmbedding(String text) {
        float[] vector = new float[384];
        // Ensure deterministic fill or run your active model inference here
        vector[0] = (float) Math.abs(text.hashCode() % 100) / 100.0f;
        return vector;
    }
}