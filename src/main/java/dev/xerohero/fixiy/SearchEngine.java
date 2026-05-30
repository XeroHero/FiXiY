package dev.xerohero.fixiy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SearchEngine {

    public static class SearchStats {
        public int filesScanned = 0;
        public int matchCount = 0;
    }

    public void executeSearch(File directory, String query, boolean isRegex,
                              boolean filterByExt, String extension,
                              Consumer<SearchResult> onMatchFound, SearchStats stats) throws Exception {

        final Pattern compiledPattern = isRegex ? Pattern.compile(query) : null;
        final String sanitizedExt = sanitizeExtension(extension);
        Path startPath = directory.toPath();

        try (Stream<Path> stream = Files.walk(startPath)) {
            stream.filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        stats.filesScanned++;

                        if (filterByExt && !sanitizedExt.isEmpty()) {
                            if (!filePath.toString().toLowerCase().endsWith(sanitizedExt)) {
                                return;
                            }
                        }

                        scanFile(filePath, startPath, query, compiledPattern, onMatchFound, stats);
                    });
        }
    }

    private void scanFile(Path filePath, Path startPath, String query, Pattern pattern,
                          Consumer<SearchResult> onMatchFound, SearchStats stats) {
        try {
            int lineNumber = 1;
            for (String line : Files.readAllLines(filePath)) {
                boolean isMatch = (pattern != null) ? pattern.matcher(line).find() : line.contains(query);

                if (isMatch) {
                    stats.matchCount++;
                    Path relativePath = startPath.relativize(filePath);

                    SearchResult result = new SearchResult(filePath.toFile(), lineNumber, line, relativePath.toString());
                    onMatchFound.accept(result);
                }
                lineNumber++;
            }
        } catch (IOException ex) {
            // Drop unreadable system/binary data silently
        }
    }

    private String sanitizeExtension(String ext) {
        if (ext == null) return "";
        String trimmed = ext.trim().toLowerCase();
        if (!trimmed.isEmpty() && !trimmed.startsWith(".")) {
            return "." + trimmed;
        }
        return trimmed;
    }
}