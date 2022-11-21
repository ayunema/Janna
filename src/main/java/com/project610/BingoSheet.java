package com.project610;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

public class BingoSheet {
    public BingoSquare[][] squares;
    public boolean valid = true;

    public BingoSheet() {
        this.squares = new BingoSquare[5][5];
        ArrayList<BingoSquare> tempSquares = new ArrayList<>();

        // Sort bingo squares by difficulty
        TreeMap<Integer, ArrayList<BingoSquare>> difficultyMap = new TreeMap<>();
        for (BingoSquare square : Janna.bingoSquares.values()) {
            if (difficultyMap.get(square.difficulty) == null) {
                difficultyMap.put(square.difficulty, new ArrayList<>());
            }
            difficultyMap.get(square.difficulty).add(square);
        }

        for (int i = 0; i < 3; i++) {
            tempSquares.add(getSquareByDifficulty(difficultyMap, 3));
        }
        for (int i = 0; i < 15; i++) {
            tempSquares.add(getSquareByDifficulty(difficultyMap, 2));
        }
        for (int i = 0; i < 6; i++) {
            tempSquares.add(getSquareByDifficulty(difficultyMap, 1));
        }
        Collections.shuffle(tempSquares);

        // Add a free square, if it exists?
        BingoSquare freeSquare = getSquareByDifficulty(difficultyMap, 0);
        if (freeSquare == null) {
            for (ArrayList<BingoSquare> list : difficultyMap.values()) {
                if (list.size() > 0)
                    freeSquare = list.get((int)(Math.random()*list.size()));
            }
        }
        squares[2][2] = freeSquare;
        if (freeSquare == null) {
            valid = false;
            return;
        }

        // Only 5x5 for now
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                if (!(x == 2 && y == 2)) {
                    int rand = (int) (Math.random() * tempSquares.size());
                    squares[x][y] = tempSquares.get(rand);
                    tempSquares.remove(rand);
                }
                System.out.print(squares[x][y].name + " - ");
            }
            System.out.println();
        }

    }

    public BingoSheet(String squaresText) {
        this.squares = new BingoSquare[5][5];
        String[] split = squaresText.split(",");

        for (int i = 0, x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++, i++) {
                squares[x][y] = Janna.bingoSquares.get(Integer.parseInt(split[i]));
            }
        }
    }

    BingoSquare getSquareByDifficulty(TreeMap<Integer, ArrayList<BingoSquare>> difficultyMap, int difficulty) {
        if (difficulty == -1) return null;
        if (difficultyMap.get(difficulty).size() == 0) {
            return getSquareByDifficulty(difficultyMap, difficulty-1);
        }
        int rand = (int)(Math.random() * difficultyMap.get(difficulty).size());
        BingoSquare square = difficultyMap.get(difficulty).get(rand);
        difficultyMap.get(difficulty).remove(rand);
        return square;
    }
}
