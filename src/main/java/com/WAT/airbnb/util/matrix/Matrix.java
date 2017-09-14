package com.WAT.airbnb.util.matrix;

public class Matrix {
    private MatrixRow[] rows;
    private int n;
    private int m;

    public Matrix(int n, int m) {
        rows = new MatrixRow[m];
        for (int i = 0; i < n; i++) {
            rows[i] = new MatrixRow(n);
        }
    }

    public void set(int i, int j, int val) {
        rows[i].set(j, val);
    }

    public int get(int i, int j) {
        return rows[i].get(j);
    }

    public MatrixRow getRow(int index) throws IllegalArgumentException {
        if (index >= n) throw new IllegalArgumentException("Index out of range");
        return rows[index];
    }

    public int n() { return this.n; }

    public int m() { return this.m; }
}
