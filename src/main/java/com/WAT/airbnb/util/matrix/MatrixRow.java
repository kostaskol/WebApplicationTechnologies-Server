package com.WAT.airbnb.util.matrix;

public class MatrixRow {
    private int length;
    private int[] attributes;

    public MatrixRow(int[] attrs) {
        this.attributes = attrs;
        this.length = attrs.length;
    }

    public MatrixRow(int length) {
        this.attributes = new int[length];
        this.length = length;
    }

    public void set(int index, int item) throws IllegalArgumentException {
        if (index >= this.length) throw new IllegalArgumentException("Index out of range");
        this.attributes[index] = item;
    }

    public int get(int index) throws IllegalArgumentException {
        if (index >= this.length) throw new IllegalArgumentException("Index out of range");
        return this.attributes[index];
    }

    public int[] getAttributes() {
        return this.attributes;
    }

    public int size() {
        return this.length;
    }
}
