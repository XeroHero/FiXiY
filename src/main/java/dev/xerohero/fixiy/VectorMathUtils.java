package dev.xerohero.fixiy;

public class VectorMathUtils {

    /**
     * Calculates the Dot Product of two vectors.
     * High performance, single-pass implementation.
     */
    public static float dotProduct(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must be non-null and of the same length.");
        }

        float dotProduct = 0.0f;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
        }
        return dotProduct;
    }

    /**
     * Calculates the Cosine Similarity between two vectors.
     * Returns a value between -1.0 (completely opposite) and 1.0 (identical directional alignment).
     */
    public static float cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must be non-null and of the same length.");
        }

        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;

        // Single-pass execution loop to minimize CPU cache misses
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0.0f || normB == 0.0f) {
            return 0.0f; // Handle edge-case division by zero for empty/zero vectors
        }

        return (float) (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}