package com.project610;

public class Bingo {

    public static boolean check(BingoSheet sheet) {
        boolean line;

        // Debug output
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                System.out.print(Janna.bingoSquares.get(sheet.squares[x][y].id).state == 1 ? "X" : ".");
            }
            System.out.println();
        }

        // Check columns
        for (int x = 0; x < 5; x++) {
            line = true;
            for (int y = 0; y < 5 && line; y++) {
                if (Janna.bingoSquares.get(sheet.squares[x][y].id).state == 0) {
                    line = false;
                }
            }
            if (line) return true;
        }

        // Check rows
        for (int y = 0; y < 5; y++) {
            line = true;
            for (int x = 0; x < 5 && line; x++) {
                if (Janna.bingoSquares.get(sheet.squares[x][y].id).state == 0) {
                    line = false;
                }
            }
            if (line) return true;
        }

        // Check diags
        line = true;
        for (int i = 0; i < 5 && line; i++) {
            if (Janna.bingoSquares.get(sheet.squares[i][i].id).state == 0) {
                line = false;
            }
        }
        if (line) return true;

        line = true;
        for (int i = 0; i < 5 && line; i++) {
            if (Janna.bingoSquares.get(sheet.squares[i][4-i].id).state == 0) {
                line = false;
            }
        }
        if (line) return true;

        // Nothin'
        return false;
    }
}
