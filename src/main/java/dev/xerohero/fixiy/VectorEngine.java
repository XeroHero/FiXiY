package dev.xerohero.fixiy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class VectorEngine {

    /**
     * Transforms an input text string into a deterministic feature vector
     * of exactly 384 float dimensions.
     */
    public float[] vectorize(String text) {
        if (text == null) {
            return new float[384];
        }

        float[] embedding = new float[384];
        try {
            // High-speed deterministic hashing to simulate token feature extraction
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));

            // Populate the 384 dimensions quickly using a deterministic pseudo-random sequence
            long seed = 0;
            for (byte b : hash) {
                seed = (seed << 8) | (b & 0xFF);
            }

            java.util.Random deterministicRandom = new java.util.Random(seed);
            for (int i = 0; i < 384; i++) {
                embedding[i] = deterministicRandom.nextFloat() * 2.0f - 1.0f; // Values between -1.0 and 1.0
            }

        } catch (NoSuchAlgorithmException e) {
            // Fallback layout initialization if digest provider is missing
            for (int i = 0; i < 384; i++) {
                embedding[i] = (float) Math.sin(text.hashCode() + i);
            }
        }
        return embedding;
    }
}