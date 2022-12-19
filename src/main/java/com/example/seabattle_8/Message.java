package com.example.seabattle_8;

import java.io.Serializable;

public class Message implements Serializable {

    MsgType type;
    public int row;
    public int column;
    FieldState[][] field;
    Message(MsgType type_, int row_, int column_, FieldState[][] field_) {
        type = type_;
        row = row_;
        column = column_;
        if (field_ != null) {
            field = new FieldState[10][10];
            for (int i = 0; i < 10; i++) {
                System.arraycopy(field_[i], 0, field[i], 0, 10);
            }
        }
        else {
            field = null;
        }
    }
}
