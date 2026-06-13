package dev.xerohero.fixiy.search.semantic;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a Fixed-Window text chunking strategy for local file processing.
 * Designed to optimize string content for 128-token context windows while
 * preserving structural file parameters like line positions.
 * * @author Lorenzo Battilocchi
 */
public class DocumentChunker {

    /**
     * Data carrier linking a physical chunk of text to its location constraints.
     */
    public static class FileChunk {
        private final Path filePath;
        private final int startLine;
        private final int endLine;
        private final String content;

        public FileChunk(Path filePath, int startLine, int endLine, String content) {
            this.filePath = filePath;
            this.startLine = startLine;
            this.endLine = endLine;
            this.content = content;
        }

        public Path getFilePath() { return filePath; }
        public int getStartLine() { return startLine; }
        public int getEndLine() { return endLine; }
        public String getContent() { return content; }

        @Override
        public String toString() {
            return String.format("%s [Lanes %d-%d]: %s", filePath.getFileName(), startLine, endLine, content);
        }
    }

    /**
     * Reads a text file line-by-line and groups lines into a fixed window
     * based on character limits, maintaining metadata for search hit rendering.
     * * @param filePath The path of the targeted source file on disk.
     * @return A list of discrete, manageable text components.
     * @throws IOException If file reads encounter low-level OS access errors.
     */
    public List<FileChunk> chunkFile(Path filePath) throws IOException {
        List<FileChunk> chunks = new ArrayList<>();

        // Approximate safe target: ~80-100 words (approx 4 characters per word)
        // to fit completely within the 128 token threshold without clipping.
        final int MAX_CHARACTER_LIMIT = 400;

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            int currentLineNumber = 0;

            StringBuilder currentChunkText = new StringBuilder();
            int chunkStartLine = 1;

            while ((line = reader.readLine()) != null) {
                currentLineNumber++;
                String trimmedLine = line.strip();

                if (trimmedLine.isEmpty()) {
                    continue;
                }

                // If adding this line exceeds our window limit, flush out the current window
                if (currentChunkText.length() + trimmedLine.length() > MAX_CHARACTER_LIMIT && currentChunkText.length() > 0) {
                    chunks.add(new FileChunk(
                            filePath,
                            chunkStartLine,
                            currentLineNumber - 1,
                            currentChunkText.toString().strip()
                    ));

                    // Reset window context
                    currentChunkText.setLength(0);
                    chunkStartLine = currentLineNumber;
                }

                currentChunkText.append(trimmedLine).append(" ");
            }

            // Collect any trailing residual text left at EOF
            if (currentChunkText.length() > 0) {
                chunks.add(new FileChunk(
                        filePath,
                        chunkStartLine,
                        currentLineNumber,
                        currentChunkText.toString().strip()
                ));
            }
        }

        return chunks;
    }
}