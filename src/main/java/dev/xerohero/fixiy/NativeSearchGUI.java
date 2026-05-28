package dev.xerohero.fixiy;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.DirectoryChooser;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class NativeSearchGUI extends Application {
    private File selectedDirectory;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Native Java String Finder");

        // UI Components
        Label folderLabel = new Label("No folder selected");
        Button browseBtn = new Button("Browse Folder");
        TextField searchField = new TextField();
        searchField.setPromptText("Enter string to search for...");
        Button searchBtn = new Button("Search");

        TextArea resultsArea = new TextArea();
        resultsArea.setEditable(false);

        // Handle Folder Browsing
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            selectedDirectory = dc.showDialog(primaryStage);
            if (selectedDirectory != null) {
                folderLabel.setText("Folder: " + selectedDirectory.getAbsolutePath());
            }
        });

        // Handle Search Logic (Pure Java)
        searchBtn.setOnAction(e -> {
            String searchTerm = searchField.getText().trim();
            if (selectedDirectory == null || searchTerm.isEmpty()) {
                resultsArea.setText("Please select a folder and enter a search term.");
                return;
            }

            resultsArea.setText("Searching... Please wait...\n");

            // We use a StringBuilder to accumulate our findings
            StringBuilder output = new StringBuilder();
            Path startPath = selectedDirectory.toPath();

            // Using try-with-resources to ensure system file handles are closed properly
            try (Stream<Path> stream = Files.walk(startPath)) {

                stream.filter(Files::isRegularFile) // Only look at files, ignore folders
                        .forEach(filePath -> {
                            try {
                                // Read all lines of the file and check for matches
                                long lineNumber = 1;
                                for (String line : Files.readAllLines(filePath)) {
                                    if (line.contains(searchTerm)) {
                                        // Get a relative path so the output looks clean
                                        Path relativePath = startPath.relativize(filePath);
                                        output.append(relativePath)
                                                .append(" [Line ").append(lineNumber).append("]: ")
                                                .append(line.trim())
                                                .append("\n");
                                    }
                                    lineNumber++;
                                }
                            } catch (IOException ex) {
                                // Skip binary files (like images/PDFs) that throw encoding errors
                            }
                        });

                resultsArea.setText(output.length() > 0 ? output.toString() : "No matches found.");

            } catch (IOException ex) {
                resultsArea.setText("Error walking through directories: " + ex.getMessage());
            }
        });

        // Layout setup
        VBox layout = new VBox(10, folderLabel, browseBtn, searchField, searchBtn, resultsArea);
        layout.setStyle("-fx-padding: 20;");
        VBox.setVgrow(resultsArea, Priority.ALWAYS); // Let the text area expand

        Scene scene = new Scene(layout, 700, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}