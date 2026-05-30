module dev.xerohero.fixiy {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens dev.xerohero.fixiy to javafx.fxml;
    exports dev.xerohero.fixiy;
}