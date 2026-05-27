package com.example.board_test.chess.model;

public enum PlayerColor {
    WHITE,
    BLACK;

    public PlayerColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    public String koreanName() {
        return this == WHITE ? "백" : "흑";
    }
}
