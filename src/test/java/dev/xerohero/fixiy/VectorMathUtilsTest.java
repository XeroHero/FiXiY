package dev.xerohero.fixiy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VectorMathUtilsTest {

    @Test
    void testCosineSimilarityIdenticalVectors() {
        float[] vectorA = {1.0f, 2.0f, 3.0f};
        float[] vectorB = {1.0f, 2.0f, 3.0f};

        float similarity = VectorMathUtils.cosineSimilarity(vectorA, vectorB);
        // Identical direction must equal exactly 1.0 (with slight float tolerance)
        assertEquals(1.0f, similarity, 1e-5f);
    }

    @Test
    void testCosineSimilarityOrthogonalVectors() {
        float[] vectorA = {1.0f, 0.0f};
        float[] vectorB = {0.0f, 1.0f};

        float similarity = VectorMathUtils.cosineSimilarity(vectorA, vectorB);
        // Perpendicular (orthogonal) vectors have zero correlation
        assertEquals(0.0f, similarity, 1e-5f);
    }

    @Test
    void testDotProductCalculation() {
        float[] vectorA = {1.0f, 3.0f, -5.0f};
        float[] vectorB = {4.0f, -2.0f, -1.0f};

        // (1*4) + (3*-2) + (-5*-1) = 4 - 6 + 5 = 3
        float result = VectorMathUtils.dotProduct(vectorA, vectorB);
        assertEquals(3.0f, result, 1e-5f);
    }
}