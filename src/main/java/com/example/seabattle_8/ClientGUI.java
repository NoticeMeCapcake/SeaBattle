package com.example.seabattle_8;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class ClientGUI implements Initializable {

    public FieldState[][] friendlyField = new FieldState[10][10];
    public FieldState[][] opponentField = new FieldState[10][10];

    public boolean placementStage;
    public boolean clickedState = false;

    private List<ImageView> friendlyShipImages = new ArrayList<>();

    Node clickedNode = null;
    @FXML
    GridPane friendlyPane;

    @FXML
    GridPane opponentPane;

    @FXML
    Label clickLabel;

    @FXML
    Label turnLabel;

    @FXML
    Button fireBtn;

    @FXML
    Label opponentClickLabel;

    public Image brokenShipImg = new Image("C:\\Users\\Danon\\IdeaProjects\\SeaBattle_8\\brokenShip.jpg");
    public Image redCross = new Image("C:\\Users\\Danon\\IdeaProjects\\SeaBattle_8\\redCross.png");

    public boolean gameOver = false;

    @FXML
    public Button remakeBtn;

    @FXML
    protected void remakeField() {
        if (!gameOver && placementStage) {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    friendlyField[i][j] = FieldState.EMPTY;
                }
            }
            for (int i = 1; i <= 4; i++) {
                friendlyFleet.put(i, 5 - i);
            }
            for (ImageView img : friendlyShipImages) {
                friendlyPane.getChildren().remove(img);
            }
        }
    }

    @FXML
    protected void fire() {
        if (turn && !placementStage && clickedNode != null && !gameOver) {
            int row = GridPane.getRowIndex(clickedNode);
            int col = GridPane.getColumnIndex(clickedNode);
            clickedNode = null;
            opponentClickLabel.setText("");
            Message msg = new Message(MsgType.MOVE, row, col, null);
            try {
                client.sendMessage(msg);

            }
            catch (IOException e) {
                System.out.println("Ошибка отправки или получения сообщения");
                gameOver = true;
                turnLabel.setText("Ошибка соединения");
                try {
                    client.disconnect();
                }
                catch (IOException er) {
                    System.out.println("Ошибка закрытия сокета");
                }
            }

        }
    }

    public void listen() {
        CompletableFuture.runAsync(this::getAndProcessMessages);
    }

    @FXML
    protected void endPlacement() {
        if (placementStage && !gameOver) {
            for (int i = 1; i <= 4; i++) {
                if (friendlyFleet.get(i) != 0) {
                    System.out.println("Не все корабли расставлены");
                    System.out.flush();
                    return;
                }
            }
            placementStage = false;
            clickedNode = null;
            clickLabel.setText("Размещение завершено");
            turnLabel.setText("Ожидание ответа сервера");
            System.out.flush();
            Message msg = new Message(MsgType.SET_FIELD, 0, 0, friendlyField);
            try {
                client.sendMessage(msg);
            }
            catch (IOException e) {
                System.out.println("Ошибка отправки сообщения");
            }
        }
    }

    public void changeTurn() {
        if (turn) {
            Platform.runLater(() -> turnLabel.setText("Ход соперника"));
        }
        else {
            Platform.runLater(() -> turnLabel.setText("Ваш ход"));
        }
        turn = !turn;
    }

    public void breakFriendlyShip(int row, int col) {
        friendlyField[row][col] = FieldState.BROKEN;
        Platform.runLater(() -> friendlyPane.add(new ImageView(brokenShipImg), col, row));
    }

    public void breakOpponentShip(int row, int col) {
        opponentField[row][col] = FieldState.BROKEN;
        Platform.runLater(() -> opponentPane.add(new ImageView(brokenShipImg), col, row));
    }

    public void blockOpponentFieldsNearBrokenShip(int row, int col, int prevRow, int prevCol) {
        if (opponentField[row][col] != FieldState.BROKEN) {
            return;
        }
        int leftUpperRow = row - 1;
        int leftUpperCol = col - 1;
        for (int i = 0; i <= 8; i++) {
            int _row = leftUpperRow + (i / 3);
            int _col = leftUpperCol + (i % 3);
            boolean isNotThisOrPrevShip = (_row == row && _col == col) || (_row == prevRow && _col == prevCol);
            if (_col >= 0 && _col <= 9 && _row >= 0 && _row <= 9 && (!isNotThisOrPrevShip)) {
                if (opponentField[_row][_col] == FieldState.BROKEN) {
                    blockOpponentFieldsNearBrokenShip(_row, _col, row, col);
                }
                else if (opponentField[_row][_col] == FieldState.EMPTY) {
                    addOpponentMiss(_row, _col);
                }
            }
        }
    }

    public void blockFriendlyFieldsNearBrokenShip(int row, int col, int prevRow, int prevCol) {
        if (friendlyField[row][col] != FieldState.BROKEN) {
            return;
        }
        int leftUpperRow = row - 1;
        int leftUpperCol = col - 1;
        for (int i = 0; i <= 8; i++) {
            int _row = leftUpperRow + (i / 3);
            int _col = leftUpperCol + (i % 3);
            boolean isNotThisOrPrevShip = (_row == row && _col == col) || (_row == prevRow && _col == prevCol);
            if (_col >= 0 && _col <= 9 && _row >= 0 && _row <= 9 && (!isNotThisOrPrevShip)) {
                if (friendlyField[_row][_col] == FieldState.BROKEN) {
                    blockFriendlyFieldsNearBrokenShip(_row, _col, row, col);
                }
                else if (friendlyField[_row][_col] == FieldState.EMPTY) {
                    addFriendlyMiss(_row, _col);
                }
            }
        }
    }

    public void lostSituation(int row, int col) {
        Platform.runLater(() -> turnLabel.setText("Соперник победил"));
        breakFriendlyShip(row, col);
        gameOver = true;
    }

    public void winningSituation(int row, int col) {
        Platform.runLater(() -> turnLabel.setText("Вы победили"));
        breakOpponentShip(row, col);
        gameOver = true;
    }

    public void addFriendlyMiss(int row, int col) {
        friendlyField[row][col] = FieldState.BLOCKED;
        Platform.runLater(() -> friendlyPane.add(new ImageView(redCross), col, row));
    }

    public void addOpponentMiss(int row, int col) {
        opponentField[row][col] = FieldState.BLOCKED;
        Platform.runLater(() -> opponentPane.add(new ImageView(redCross), col, row));
    }


    public void getAndProcessMessages() {
        System.out.println(Thread.currentThread());
        System.out.flush();
        Message msg;
        try {
            while (true) {
                if (client == null) {
                    System.out.println("WTF");
                    System.out.flush();
                }
                System.out.println("До получения");
                msg = client.receiveMessage();


                System.out.println("Получили");
                if (msg.type == MsgType.CHANGE_TURN) {
                    if (msg.field == null) {
                        if (turn) {
                            addOpponentMiss(msg.row, msg.column);
                        } else {
                            addFriendlyMiss(msg.row, msg.column);
                        }
                    }
                    changeTurn();
                } else if (msg.type == MsgType.SET_FIELD) {

                    Platform.runLater(() -> turnLabel.setText("Ход соперника"));
                } else if (msg.type == MsgType.LOSE) {
                    lostSituation(msg.row, msg.column);
                    blockFriendlyFieldsNearBrokenShip(msg.row, msg.column, -1, -1);
                    break;
                } else if (msg.type == MsgType.MOVE) {
                    if (turn) {
                        breakOpponentShip(msg.row, msg.column);
                    } else {
                        breakFriendlyShip(msg.row, msg.column);
                    }
                } else if (msg.type == MsgType.SUCCESS) {
                    if (turn) {
                        breakOpponentShip(msg.row, msg.column);
                        blockOpponentFieldsNearBrokenShip(msg.row, msg.column, -1, -1);
                    } else {
                        breakFriendlyShip(msg.row, msg.column);
                        blockFriendlyFieldsNearBrokenShip(msg.row, msg.column, -1, -1);
                    }
                } else if (msg.type == MsgType.WIN) {
                    winningSituation(msg.row, msg.column);
                    blockOpponentFieldsNearBrokenShip(msg.row, msg.column, -1, -1);
                    break;
                }

            }
        }
        catch (IOException e) {
            System.out.println("Ошибка получения сообщения");
            Platform.runLater(() -> turnLabel.setText("Ошибка соединения"));
            gameOver = true;
            try {
                client.disconnect();
            }
            catch (IOException er) {
                System.out.println("Ошибка закрытия сокета");
            }
        }
    }

    HashMap<Integer, Integer> friendlyFleet = new HashMap<>();

    public Image shipImage = new Image("C:\\Users\\Danon\\IdeaProjects\\SeaBattle_8\\ship.jpg");

    private ObservableList<Node> friendlyFieldNodes;

    private ObservableList<Node> opponentFieldNodes;

    public Client client;

    private boolean turn = false;


    public void setClient(Client _client){
        client = _client;
        listen();
    }

    private void blockHorizontalFields(Integer row, Integer left, Integer right) {
        for (int i = Math.max(left - 1, 0); i <= Math.min(right + 1, 9); i++) {
            if (row - 1 >= 0) {
                friendlyField[row - 1][i] = FieldState.BLOCKED;
            }
            if (row + 1 <= 9) {
                friendlyField[row + 1][i] = FieldState.BLOCKED;
            }
        }
        if (left - 1 >= 0) {
            friendlyField[row][left - 1] = FieldState.BLOCKED;
        }
        if (right + 1 <= 9) {
            friendlyField[row][right + 1] = FieldState.BLOCKED;
        }
    }

    private void blockVerticalFields(Integer col, Integer lower, Integer upper) {
        for (int i = Math.max(lower - 1, 0); i <= Math.min(upper + 1, 9); i++) {
            if (col - 1 >= 0) {
                friendlyField[i][col - 1] = FieldState.BLOCKED;
            }
            if (col + 1 <= 9) {
                friendlyField[i][col + 1] = FieldState.BLOCKED;
            }
        }
        if (lower - 1 >= 0) {
            friendlyField[lower - 1][col] = FieldState.BLOCKED;
        }
        if (upper + 1 <= 9) {
            friendlyField[upper + 1][col] = FieldState.BLOCKED;
        }
    }

    private boolean checkFreeShip(int shipLength) {
        if (friendlyFleet.containsKey(shipLength)) {
            return friendlyFleet.get(shipLength) > 0;
        }
        return false;
    }

    private boolean checkHorizontalValidPlace(int row, int left, int right) {
        for (int i = left; i <= right; i++) {
            if (friendlyField[row][i] == FieldState.BLOCKED || friendlyField[row][i] == FieldState.SHIP) {
                return false;
            }
        }
        return true;
    }

    private boolean checkVerticalValidPlace(int column, int lower, int upper) {
        for (int i = lower; i <= upper; i++) {
            if (friendlyField[i][column] == FieldState.BLOCKED || friendlyField[i][column] == FieldState.SHIP) {
                return false;
            }
        }
        return true;
    }

    public void setFriendlyField() {
        int numCols = 10;
        int numRows = 10;

        turnLabel.setText("Стадия расстановки");

        for (int i = 0 ; i < numCols ; i++) {
            for (int j = 0; j < numRows; j++) {
                Pane newFriendlyPane = new Pane();
                friendlyPane.add(newFriendlyPane, i, j);
                friendlyField[i][j] = FieldState.EMPTY;
            }
        }
        friendlyPane.setGridLinesVisible(true);
        friendlyFieldNodes = friendlyPane.getChildren();

        for (int i = 1; i <= 4; i++) {
            friendlyFleet.put(i, 5 - i);
        }

        for (Node node : friendlyFieldNodes) {
            node.onMouseClickedProperty().set(e -> {

                if (placementStage && !gameOver) {
                    if (!clickedState) {
                        clickedState = true;
                        clickedNode = node;
                        int row = GridPane.getRowIndex(clickedNode);
                        int col = GridPane.getColumnIndex(clickedNode);
                        if (friendlyField[row][col] == FieldState.SHIP || friendlyField[row][col] == FieldState.BLOCKED) {
                            clickedState = false;
                            return;
                        }
                        clickLabel.setText("Второй клик");
                    }
                    else {
                        clickedState = false;
                        clickLabel.setText("Первый клик");
                        int firstClickedRow = GridPane.getRowIndex(clickedNode);
                        int firstClickedColumn = GridPane.getColumnIndex(clickedNode);

                        int secondClickedRow = GridPane.getRowIndex(node);
                        int secondClickedColumn = GridPane.getColumnIndex(node);

                        if (firstClickedRow == secondClickedRow) {
                            int minCol = Math.min(firstClickedColumn, secondClickedColumn);
                            int maxCol = Math.max(firstClickedColumn, secondClickedColumn);
                            int shipLength = maxCol - minCol + 1;
                            if (checkFreeShip(shipLength) && checkHorizontalValidPlace(firstClickedRow, minCol, maxCol)) {
                                friendlyFleet.put(shipLength, friendlyFleet.get(shipLength) - 1);
                                for (int i = minCol; i <= maxCol; i++) {
                                    friendlyField[firstClickedRow][i] = FieldState.SHIP;
                                    ImageView imgView = new ImageView(shipImage);
                                    friendlyShipImages.add(imgView);
                                    friendlyPane.add(imgView, i, firstClickedRow);
                                }

                                blockHorizontalFields(firstClickedRow, minCol, maxCol);
                            }
                        }
                        else if (firstClickedColumn == secondClickedColumn) {
                            int minRow = Math.min(firstClickedRow, secondClickedRow);
                            int maxRow = Math.max(firstClickedRow, secondClickedRow);
                            int shipLength = maxRow - minRow + 1;
                            if (checkFreeShip(shipLength) && checkVerticalValidPlace(firstClickedColumn, minRow, maxRow)) {
                                friendlyFleet.put(shipLength, friendlyFleet.get(shipLength) - 1);
                                for (int i = minRow; i <= maxRow; i++) {
                                    friendlyField[i][firstClickedColumn] = FieldState.SHIP;
                                    ImageView imgView = new ImageView(shipImage);
                                    friendlyShipImages.add(imgView);
                                    friendlyPane.add(imgView, firstClickedColumn, i);
                                }

                                blockVerticalFields(firstClickedColumn, minRow, maxRow);
                            }
                        }
                    }
                }
            });
        }
    }

    private void setOpponentField() {
        int numCols = 10;
        int numRows = 10;

        for (int i = 0 ; i < numCols ; i++) {
            for (int j = 0; j < numRows; j++) {
                Pane newOpponentPane = new Pane();
                opponentPane.add(newOpponentPane, i, j);
                opponentField[i][j] = FieldState.EMPTY;
            }
        }
        opponentFieldNodes = opponentPane.getChildren();

        for (Node node : opponentFieldNodes) {
            node.onMouseClickedProperty().set(e -> {
                if (!placementStage && turn && !gameOver) {
                    if (opponentField[GridPane.getRowIndex(node)][GridPane.getColumnIndex(node)] != FieldState.EMPTY) {
                        return;
                    }
                    clickedNode = node;
                    opponentClickLabel.setText("Clicked - " + GridPane.getRowIndex(clickedNode) + " : " + GridPane.getColumnIndex(clickedNode));
                }
            });
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb){
        turnLabel.setText("Стадия расстановки");
        placementStage = true;
        setFriendlyField();
        setOpponentField();
    }

}