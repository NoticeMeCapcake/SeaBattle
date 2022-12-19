module com.example.seabattle_8 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;

    opens com.example.seabattle_8 to javafx.fxml;
    exports com.example.seabattle_8;
}