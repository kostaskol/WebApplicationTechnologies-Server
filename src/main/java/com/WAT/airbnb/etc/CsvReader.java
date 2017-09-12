package com.WAT.airbnb.etc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class CsvReader {
    private BufferedReader bufferedReader = null;
    private String splitter;
    private HashMap<String, Integer> hashMap = null;
    private boolean started = false;

    public CsvReader(String path, String splitter, boolean map) throws IOException {
        this.bufferedReader = new BufferedReader(new FileReader(path));
        this.splitter = splitter;
        this.started = false;
        if (map)
            createMap();
    }

    public CsvReader(String path, boolean map) throws IOException {
        this.bufferedReader = new BufferedReader(new FileReader(path));
        this.splitter = ",";
        this.started = false;
        if (map)
            createMap();
    }

    private void createMap() throws IOException, IllegalStateException {
        if (this.started)
            throw new IllegalStateException("Csv Reading has already started");

        String[] firstLine = this.next();
        if (firstLine == null)
            throw new IllegalStateException("Empty ");
        this.hashMap = new HashMap<>();

        for (int i = 0; i < firstLine.length; i++) {
            this.hashMap.put(firstLine[i], i);
        }
    }

    public CsvRow nextRow() throws IOException {
        this.started = true;
        String line;

        if ((line = this.bufferedReader.readLine()) != null) {
            String[] tmp = line.split(splitter);
            return new CsvRow(tmp, this.hashMap);
        }

        return null;
    }

    public String[] next() throws IOException {
        this.started = true;
        String line;
        if ((line = this.bufferedReader.readLine()) != null) {
            return line.split(splitter);
        }

        return null;
    }


}
