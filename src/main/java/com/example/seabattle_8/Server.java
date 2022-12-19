package com.example.seabattle_8;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

//import java.util.ArrayList;
//import java.util.List;

public class Server {
    private String host = "localhost"; //127.0.0.1
    private Integer port = 8843;

//    private final List<ClientHandler> clients = new ArrayList<>();


    public Server() {
    }

    public Server(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        System.out.println("Инициализация сервера");
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            AtomicInteger turn = new AtomicInteger(0);
            System.out.println("Сервер стартовал и ожидает подключение клиентов");
//            while (true) {
            Socket client1 = serverSocket.accept();
            System.out.println("Подключился новый клиент: " + client1.toString());
            ClientHandler clientHandler1 = new ClientHandler(client1, this, turn, 0);

            Socket client2 = serverSocket.accept();
            System.out.println("Подключился новый клиент: " + client2.toString());
            ClientHandler clientHandler2 = new ClientHandler(client2, this, turn, 1);

            clientHandler1.setOpponent(clientHandler2);
            clientHandler2.setOpponent(clientHandler1);

//            clients.add(clientHandler1);
//            clients.add(clientHandler2);
            new Thread(clientHandler1).start();
            new Thread(clientHandler2).start();
//            }
        } catch (IOException e) {
            System.err.println("Проблема с сервером");
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ex) {
                    System.err.println("Проблема при закрытии сервера");
                }
            }
        }
    }

}
