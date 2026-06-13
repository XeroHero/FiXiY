module dev.xerohero.fixiy {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens dev.xerohero.fixiy to org.junit.platform.commons, javafx.fxml;
    exports dev.xerohero.fixiy;
}