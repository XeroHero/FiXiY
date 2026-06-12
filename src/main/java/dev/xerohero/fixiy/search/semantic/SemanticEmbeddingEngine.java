package dev.xerohero.fixiy.search.semantic;

import dev.langchain4j.model.embedding.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.PoolingMode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SemanticEmbeddingEngine {
    private OnnxEmbeddingModel model;

    public void initialize() {
        try {
            // 1. Pull the compressed assets from the JAR classpath
            InputStream modelStream = getClass().getResourceAsStream("/models/model.onnx");
            InputStream tokenizerStream = getClass().getResourceAsStream("/models/tokenizer.json");

            if (modelStream == null || tokenizerStream == null) {
                throw new IllegalStateException("Failed to locate local AI assets on the classpath!");
            }

            // 2. Create zero-dependency temporary files on the host OS file system
            Path tempModelPath = Files.createTempFile("fixiy-model-", ".onnx");
            Path tempTokenizerPath = Files.createTempFile("fixiy-tokenizer-", ".json");

            // 3. Delete the temporary files automatically when FiXiY exits
            tempModelPath.toFile().deleteOnExit();
            tempTokenizerPath.toFile().deleteOnExit();

            // 4. Stream the internal data out to the physical temporary files
            Files.copy(modelStream, tempModelPath, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(tokenizerStream, tempTokenizerPath, StandardCopyOption.REPLACE_EXISTING);

            // 5. Instantiation matches the exact (Path, Path, PoolingMode) constructor
            this.model = new OnnxEmbeddingModel(tempModelPath, tempTokenizerPath, PoolingMode.MEAN);

        } catch (IOException e) {
            throw new RuntimeException("Failed to extract and load localized AI environment weights", e);
        }
    }
}