package com.example.seabattle_8;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler implements Runnable {

    private ClientHandler opponent;

    private final Socket client;
    private final Server server;
    private String name;

    private final AtomicInteger turn;
    private final ObjectInputStream in; // поток чтения из сокета
    private final ObjectOutputStream out; // поток записи в сокет

    private final int playerNumber;  // 1 or 0

    private int shipsToBreak = 10;

    public FieldState[][] field = new FieldState[10][10];

    public ClientHandler(Socket client, Server server, AtomicInteger turn_, int playerNumber_) throws IOException {
        turn = turn_;
        this.client = client;
        this.server = server;
        playerNumber = playerNumber_;
        in = new ObjectInputStream(client.getInputStream());
        out = new ObjectOutputStream(client.getOutputStream());
    }

    public void setOpponent(ClientHandler opp) {
        opponent = opp;
    }

    private boolean isBroken(int row, int col, int prevRow, int prevCol) {
        if (opponent.field[row][col] != FieldState.BROKEN) {
            return false;
        }
        boolean result = true;

        int leftUpperRow = row - 1;
        int leftUpperCol = col - 1;
        for (int i = 0; i <= 8; i++) {
            int _row = leftUpperRow + (i / 3);
            int _col = leftUpperCol + (i % 3);
            boolean isThisOrPrevShip = (_row == row && _col == col) || (_row == prevRow && _col == prevCol);
            if (_col >= 0 && _col <= 9 && _row >= 0 && _row <= 9 && (!isThisOrPrevShip)) {
                if (opponent.field[_row][_col] == FieldState.SHIP) {
                    return false;
                }
                else if (opponent.field[_row][_col] == FieldState.BROKEN) {
                    result = result && isBroken(_row, _col, row, col);
                }
            }
        }
        return result;
    }

    @Override
    public void run() {
        try {
            while (client.isConnected()) {
                if (turn.get() == 3) {  // game over condition
                    break;
                }
                else if (turn.get() == playerNumber) {
                    boolean change = false;
                    boolean notSend = false;
                    System.out.println("Слушаем игрока " + playerNumber);
                    Message msg = (Message) in.readObject();

                    if (msg.type == MsgType.SET_FIELD) {
                        for (int i = 0; i < 10; i++) {
                            System.arraycopy(msg.field[i], 0, field[i], 0, 10);
                        }
                        if (playerNumber == 1) {
                            msg.type = MsgType.CHANGE_TURN;
                            opponent.sendMessage(msg);
                        }
                        else {
                            notSend = true;
                        }
                        msg.type = MsgType.SET_FIELD;
                        System.out.println("Игрок номер " + playerNumber + " задал поле");
                        change = true;
                    } else if (msg.type == MsgType.MOVE) {
                        System.out.println("Игрок номер " + playerNumber + " ходит");
                        int row = msg.row;
                        int col = msg.column;
                        if (opponent.field[row][col] == FieldState.SHIP) { // Если попал, но не уничтожил, то MOVE, если уничтожил, то SUCCESS, если уничтожил всё, то WIN / LOSE
                            opponent.field[row][col] = FieldState.BROKEN;
                            boolean isShipBroken = isBroken(row, col, -1, -1);
                            if (isShipBroken) {
                                shipsToBreak--;
                                if (shipsToBreak == 0) {
                                    msg.type = MsgType.WIN;
                                    sendMessage(msg);
                                    msg.type = MsgType.LOSE;
                                    opponent.sendMessage(msg);
                                    turn.set(3);
                                    break;
                                }
                                else {
                                    msg.type = MsgType.SUCCESS;
                                }
                            }
                        }
                        else {
                            msg.type = MsgType.CHANGE_TURN;
                            change = true;
                        }
                        System.out.println("Отправляем игроку "+": " + msg.row + " + " + msg.column);
                        opponent.sendMessage(msg);
                    }
                    System.out.println("Отправляем сообщение игроку " + playerNumber);
                    if (!notSend) {
                        sendMessage(msg);
                    }
                    if (change && turn.get() != 3) {
                        turn.set((playerNumber == 0) ? 1 : 0);
                    }
                }
            }
        } catch (IOException e) {
            if (client.isClosed()) {
                System.out.println("Closed");
            }
            else {
                System.out.println("open");
            }
            System.err.println("Клиент вышел");

        } catch (ClassNotFoundException e) {
            System.out.println("Клиент прислал что-то не то");
        }
        finally {
            turn.set(3);
            if (in!= null) {
                try {
                    in.close();
                } catch (IOException e) {
                    System.out.println("Клиент вышел");
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    System.out.println("Клиент вышел");
                }
            }

            if (client != null) {
                try {
                    client.close();
                } catch (IOException ex) {
                    System.out.println("Ошибка при закрытии сокета");
                }
            }
        }
        System.out.println("Завершились");
    }

    public void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("Проблема при записи сообщения в поток клиента: " + client.toString() + e);
        }
    }

    public String getName() {
        return name;
    }
}
