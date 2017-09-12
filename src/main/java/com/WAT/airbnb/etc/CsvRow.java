package com.WAT.airbnb.etc;

import java.util.HashMap;

public class CsvRow {
    private String[] cols;
    private HashMap<String, Integer> hashMap;

    protected CsvRow(String[] columns, HashMap<String, Integer> map) {
        this.cols = columns;
        this.hashMap = map;
    }

    public String get(int i) throws IllegalArgumentException {
        if (i >= this.cols.length) {
            throw new IllegalArgumentException("Array index out of range");
        }
        return this.cols[i];
    }

    public String get(String col) throws IllegalArgumentException, IllegalStateException {
        if (this.hashMap == null) {
            throw new IllegalStateException("Map not created");
        }

        if (!this.hashMap.containsKey(col)) {
            throw new IllegalArgumentException("Unknown key");
        }

        return this.cols[this.hashMap.get(col)];
    }
}
