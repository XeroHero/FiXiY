package dev.xerohero.fixiy;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class VectorIndexRepository implements AutoCloseable {

    private final Directory indexDirectory;
    private final IndexWriter indexWriter;
    private final SearcherManager searcherManager;

    // Schema Field Names
    public static final String FIELD_FILE_PATH = "file_path";
    public static final String FIELD_CHUNK_ID = "chunk_id";
    public static final String FIELD_RAW_TEXT = "raw_text_chunk";
    public static final String FIELD_VECTOR = "float_array_vector";

    /**
     * Initializes an embedded, high-performance Lucene vector store.
     * @param indexPath The directory path on disk where the index will persist.
     */
    public VectorIndexRepository(Path indexPath) throws IOException {
        // MMapDirectory offers optimal performance on Linux/macOS by leveraging OS page caches
        this.indexDirectory = new MMapDirectory(indexPath);

        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        this.indexWriter = new IndexWriter(indexDirectory, config);
        // Manage real-time thread-safe search visibility handles smoothly
        this.searcherManager = new SearcherManager(indexWriter, new SearcherFactory());
    }

    /**
     * Commits a document chunk to the index matching the required schema.
     */
    public void addChunk(String filePath, int chunkId, String rawTextChunk, float[] vector) throws IOException {
        Document doc = new Document();

        // 1. file_path: Stored for retrieval, StringField prevents tokenization
        doc.add(new StringField(FIELD_FILE_PATH, filePath, Field.Store.YES));

        // 2. chunk_id: Stored as a numeric value for structured ordering/sorting
        doc.add(new StoredField(FIELD_CHUNK_ID, chunkId));

        // 3. raw_text_chunk: Stored for snippet display, TextField if you want keyword fallbacks later
        doc.add(new StoredField(FIELD_RAW_TEXT, rawTextChunk));

        // 4. float_array_vector: Native 384-dimension HNSW vector index field
        // VectorSimilarityFunction.COSINE automatically handles similarity calculations
        doc.add(new KnnVectorField(FIELD_VECTOR, vector, VectorSimilarityFunction.COSINE));

        indexWriter.addDocument(doc);
    }

    /**
     * Commits index transactions to disk. Call this after bulk data indexing cycles.
     */
    public void commit() throws IOException {
        indexWriter.commit();
        searcherManager.maybeRefresh();
    }

    /**
     * Performs an ultra-fast k-NN semantic search against indexed vectors.
     */
    public List<SearchResult> searchNearest(float[] queryVector, int topK) throws IOException {
        searcherManager.maybeRefresh();
        IndexSearcher searcher = searcherManager.acquire();
        List<SearchResult> results = new ArrayList<>();

        try {
            // Native Lucene 384-dimensional vector query matching our schema field
            KnnVectorQuery knnQuery = new KnnVectorQuery(FIELD_VECTOR, queryVector, topK);
            TopDocs topDocs = searcher.search(knnQuery, topK);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.storedFields().document(scoreDoc.doc);

                String filePath = doc.get(FIELD_FILE_PATH);
                int chunkId = doc.getField(FIELD_CHUNK_ID).numericValue().intValue();
                String rawText = doc.get(FIELD_RAW_TEXT);

                // Map out the internal match frame representation back to FiXiY's UI models
                // (Reusing your SearchResult archetype structure or specialized sub-formats)
                results.add(new SearchResult(
                        new java.io.File(filePath),
                        chunkId,
                        rawText,
                        String.format("Score: %.4f [Chunk %d]", scoreDoc.score, chunkId)
                ));
            }
        } finally {
            searcherManager.release(searcher);
        }
        return results;
    }

    @Override
    public void close() throws Exception {
        searcherManager.close();
        indexWriter.close();
        indexDirectory.close();
    }
}