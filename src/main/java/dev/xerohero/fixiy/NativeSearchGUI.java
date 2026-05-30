package dev.xerohero.fixiy;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.DirectoryChooser;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;

public class NativeSearchGUI extends Application {

    private final SearchEngine searchEngine = new SearchEngine();
    private File selectedDirectory;
    private Thread backgroundThread;

    // UI Elements
    private Label folderLabel;
    private Button browseBtn; // Fixed: Kept globally scoped for consistency
    private TextField searchField;
    private CheckBox regexCheck;
    private CheckBox enableExtCheck;
    private TextField extField;
    private Button searchBtn;
    private ListView<SearchResult> resultsListView;
    private Label statusBar;
    private ProgressIndicator progressIndicator;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("FiXiY - Find X in Y");

        initializeComponents(primaryStage);

        // Layout assemblies
        HBox optionsBox = new HBox(20, regexCheck, enableExtCheck, extField);
        optionsBox.setStyle("-fx-alignment: center-left;");

        HBox actionBox = new HBox(15, searchBtn, progressIndicator);
        actionBox.setStyle("-fx-alignment: center-left;");

        // Fixed Layout mapping: Explicitly feeding browseBtn into the layout grid
        VBox layout = new VBox(12,
                folderLabel,
                browseBtn,
                searchField, optionsBox, actionBox,
                new Label("Double-click a result to open the file natively:"),
                resultsListView, statusBar
        );
        layout.setStyle("-fx-padding: 20;");
        VBox.setVgrow(resultsListView, Priority.ALWAYS);

        primaryStage.setScene(new Scene(layout, 800, 600));
        primaryStage.show();
    }

    private void initializeComponents(Stage stage) {
        folderLabel = new Label("No folder selected");

        // Fixed: Instantiate browseBtn and hook up action right here
        browseBtn = new Button("Browse Folder");
        browseBtn.setOnAction(e -> handleBrowseAction(stage));

        searchField = new TextField();
        searchField.setPromptText("Enter search term or Regular Expression...");

        regexCheck = new CheckBox("Use Regular Expression (Regex)");
        enableExtCheck = new CheckBox("Filter by extension");

        extField = new TextField();
        extField.setPromptText("e.g., .log");
        extField.setPrefWidth(100);
        extField.setDisable(true);
        enableExtCheck.setOnAction(e -> extField.setDisable(!enableExtCheck.isSelected()));

        searchBtn = new Button("Search");
        searchBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        searchBtn.setOnAction(e -> handleSearchToggle());

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(25, 25);

        resultsListView = new ListView<>();
        setupDoubleClickHandler();

        statusBar = new Label("Ready");
        statusBar.setStyle("-fx-text-fill: #555555; -fx-font-style: italic;");
    }

    private void handleBrowseAction(Stage stage) {
        DirectoryChooser dc = new DirectoryChooser();

        dc.setTitle("Select Target Search Folder");

        File choice = dc.showDialog(stage);
        if (choice != null) {
            selectedDirectory = choice;
            folderLabel.setText("Folder: " + selectedDirectory.getAbsolutePath());
        }
    }

    private void setupDoubleClickHandler() {
        resultsListView.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                SearchResult selected = resultsListView.getSelectionModel().getSelectedItem();
                if (selected != null && Desktop.isDesktopSupported()) {
                    new Thread(() -> {
                        try {
                            Desktop.getDesktop().open(selected.getFile());
                        } catch (Exception ex) {
                            Platform.runLater(() -> statusBar.setText("Failed to open file: " + ex.getMessage()));
                        }
                    }).start();
                }
            }
        });
    }

    private void handleSearchToggle() {
        if (backgroundThread != null && backgroundThread.isAlive()) {
            backgroundThread.interrupt();
            statusBar.setText("Search aborted by user.");
            resetSearchButton(false);
            return;
        }

        String query = searchField.getText().trim();
        if (selectedDirectory == null || query.isEmpty()) {
            statusBar.setText("Error: Please select a folder and type a query first.");
            return;
        }

        runAsynchronousSearch(query);
    }

    private void runAsynchronousSearch(String query) {
        resultsListView.getItems().clear();
        progressIndicator.setVisible(true);
        searchBtn.setText("Cancel Search");
        searchBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        statusBar.setText("Scanning system paths...");

        long startTime = System.currentTimeMillis();
        SearchEngine.SearchStats stats = new SearchEngine.SearchStats();

        Task<Void> searchTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                searchEngine.executeSearch(
                        selectedDirectory, query,
                        regexCheck.isSelected(), enableExtCheck.isSelected(), extField.getText(),
                        match -> Platform.runLater(() -> resultsListView.getItems().add(match)),
                        stats
                );
                return null;
            }
        };

        searchTask.setOnSucceeded(e -> {
            long elapsedTime = System.currentTimeMillis() - startTime;
            statusBar.setText(String.format("Done! Found %d matches across %d scanned files in %d ms.",
                    stats.matchCount, stats.filesScanned, elapsedTime));
            resetSearchButton(true);
        });

        searchTask.setOnFailed(e -> {
            statusBar.setText("Search failed: " + searchTask.getException().getMessage());
            resetSearchButton(true);
        });

        backgroundThread = new Thread(searchTask);
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }

    private void resetSearchButton(boolean hideProgress) {
        if (hideProgress) progressIndicator.setVisible(false);
        searchBtn.setText("Search");
        searchBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
    }

    public static void main(String[] args) { launch(args); }
}