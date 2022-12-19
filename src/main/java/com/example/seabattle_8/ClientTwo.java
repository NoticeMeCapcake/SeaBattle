package com.example.seabattle_8;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


import java.io.IOException;


public class ClientTwo extends Application {
    public Client client;

    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientOne.class.getResource("hello-view.fxml"));
        System.out.println("2");
        client = new Client("localhost", 8843, "Victor");
        Scene scene = new Scene(fxmlLoader.load(), 879, 586);
        stage.setResizable(false);
        stage.setTitle("Sea Battle2!");
        stage.setScene(scene);
        stage.show();
        ClientGUI controller = fxmlLoader.getController();
        try {
            client.connect();
        }
        catch (IOException e) {
            System.out.println("Невозможно подключиться");
        }
        controller.setClient(client); //проставляете значение в котроллер
        stage.setOnHiding(event -> Platform.runLater(() -> {
            try {
                client.disconnect();
            } catch (IOException e) {
                System.out.println("Не удалось закрыть сокет или он уже  закрыт");
            }
            System.exit(0);
        }));
    }


    public static void main(String[] args) {
        launch();
    }
}
