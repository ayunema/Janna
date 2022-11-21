package com.project610;

public class BingoSquare {
    public String name, description;
    public int id, difficulty, state;

    public BingoSquare(String name) {
        this.name = name;
        this.description = "";
        this.difficulty = 0;
        this.id = -1;
        this.state = 0;
    }

    public BingoSquare(int id, String name, String description, int difficulty, int state) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.difficulty = difficulty;
        this.state = state;
    }
}
