package com.example.seabattle_8;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {
    private final String host;
    private final Integer port;
    private final String name;
    public Socket server;

    public ObjectOutputStream outputStream;
    public ObjectInputStream objectInputStream;
    public Client(String host, Integer port, String name) {
        this.host = host;
        this.port = port;
        this.name = name;
    }

    public void connect() throws IOException {
//        try {
            System.out.println("Подключаемся");
            server = new Socket(host, port);
            System.out.println("Подключились");
            outputStream = new ObjectOutputStream(server.getOutputStream());
            System.out.println("22");
            objectInputStream = new ObjectInputStream(server.getInputStream());
            System.out.println("11");
//        }
//        catch (IOException e) {
//            System.out.println("Невозможно подключиться к серверу");
//            throw new RuntimeException(e);
//            TODO: throw exception
//        }
    }
    //public Socket server;
    public void sendMessage(Message message) throws IOException {
        if (server != null && server.isConnected()) {
                try {
                    System.out.println("Отправляем сообщение");
                    System.out.flush();
                    outputStream.writeObject(message);
                }
                catch (IOException e) {
                    System.out.println("Ошибка отправки сообщения на сервер");
                    throw e;
//            TODO: throw exception
                }

        }
    }

    public Message receiveMessage() throws IOException {
        Message msg = null;
        if (server != null && server.isConnected()) {
                try  {
                    System.out.println("Слушаем сервер");
                    System.out.flush();
                    msg = ((Message) objectInputStream.readObject());
                    System.out.println("Print" + msg.type);
                    System.out.println("123");


                }
                catch (IOException e) {
                    System.out.println("Ошибка во время получения ответа от сервера");
                    throw e;
                } catch (ClassNotFoundException e) {
                    System.out.println("Пришло что-то не то");
                }
        }
        return msg;
    }

    public void disconnect() throws IOException {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (objectInputStream != null) {
                objectInputStream.close();
            }
            if (server != null) {
                server.close();
            }
        }
        catch (IOException e) {
            System.out.println("Ошибка во время закрытия соединения с сервером");
//            TODO: throw exception
        }
    }

}
