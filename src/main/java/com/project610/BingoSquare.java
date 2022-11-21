package com.project610;

public class BingoSquare {
    public String name, description;
    public int difficulty;

    public BingoSquare(String name) {
        this.name = name;
        this.description = "";
        this.difficulty = 0;
    }

    public BingoSquare(String name, String description, int difficulty) {
        this.name = name;
        this.description = description;
        this.difficulty = difficulty;
    }
}
