package com.WAT.airbnb.util;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

/**
 *  Simple Vector representation class that provides
 *  basic vector operations (i.e. Dot product and cosine similarity)
 *  @author Kostas Kolivas
 *  @version 1.0
 */
public class Vector {
    private double[] values;
    private boolean[] bValues;
    private int id;
    private int length;

    public Vector(int id, int length, boolean zero) {
        this.bValues = null;
        this.id = id;
        this.length = length;
        this.values = new double[length];
        if (zero) {
            Arrays.fill(values, 0d);
        }
    }

    public Vector(@NotNull double[] v, int id) {
        this.bValues = null;
        this.values = v;
        this.id = id;
        this.length = values.length;
    }

    public Vector(Double[] v, int id) {
        this.bValues = null;
        this.length = v.length;
        this.values = new double[this.length];
        for (int i = 0; i < v.length; i++) {
            this.values[i] = v[i];
        }
        this.id = id;
    }

    public Vector(ArrayList<Double> v, int id) {
        this.bValues = null;
        this.length = v.size();
        this.values = new double[this.length];
        for (int i = 0; i < this.length; i++) {
            this.values[i] = v.get(i);
        }
        this.id = id;
    }

    public Vector(boolean[] v, int id) {
        this.values = null;
        this.bValues = v;
        this.id = id;
        this.length = bValues.length;
    }

    public Vector(int id, int length, boolean zero, int ph) {
        this.id = id;
        this.length = length;
        this.bValues = new boolean[length];
        this.values = null;
        for (int i = 0; i < length; i++)
            this.bValues[i] = false;
    }

    public Vector(@NotNull double[] v) {
        this.bValues = null;
        this.values = v;
        this.length = values.length;
    }

    public Double dot(@NotNull Vector other) {
        if (this.values == null) return null;
        if (this.length != other.length) return null;
        double sum = 0d;
        for (int i = 0; i < this.length; i++) {
            sum += this.values[i] * other.values[i];
        }
        return sum;
    }

    public int size() { return this.length; }

    public Double cosineSim(@NotNull Vector other, int ph) {
        if (this.bValues == null) return null;
        if (this.length != other.length) return null;
        Double sim = this.dot(other, 0);
        if (sim == null) return null;
        double sum1 = 0;
        double sum2 = 0;
        for (int i = 0; i < this.length; i++) {
            sum1 += Math.pow(this.bValues[i] ? 1 : 0, 2);
            sum2 += Math.pow(other.bValues[i] ? 1 : 0, 2);
        }

        double sqrt1 = Math.sqrt(sum1);
        double sqrt2 = Math.sqrt(sum2);
        return sim / (sqrt1 * sqrt2);
    }

    public Double dot(@NotNull Vector other, int ph) {
        if (this.bValues == null) return null;
        if (this.length != other.length) return null;
        double sum = 0d;
        for (int i = 0; i < this.length; i++) {
            sum += (this.bValues[i] ? 1d : 0d) * (other.bValues[i] ? 1d : 0d);
        }
        return sum;
    }

    public Double cosineSim(@NotNull Vector other) {
        // A * B / (sqrt(sum(Ai ^ 2)) * sqrt(sum(Bi ^ 2)))
//        System.out.println("Computing cosine sim");
        if (this.values == null) return null;
        if (this.length != other.length) return null;
        Double sim = (double) this.dot(other);
//        System.out.println("Dot product: " + sim);
        if (sim == 0d) return 0d;
        double sum1 = 0d;
        double sum2 = 0d;
        for (int i = 0; i < this.length; i++) {
            sum1 += Math.pow(this.values[i], 2);
            sum2 += Math.pow(other.values[i], 2);
        }

        double sqrt1 = Math.sqrt(sum1);
        double sqrt2 = Math.sqrt(sum2);
//        System.out.println("sqrt1 = " + sqrt1);
//        System.out.println("sqrt2 = " + sqrt2);
        return (sim / (sqrt1 * sqrt2));
    }

    public int getId() { return this.id; }

    public Double get(int index) throws IllegalArgumentException {
        if (this.values == null) return null;
        if (index >= length) throw new IllegalArgumentException("Index out of range");
        return this.values[index];
    }

    public Boolean getB(int index) throws IllegalArgumentException {
        if (this.bValues == null) return null;
        if (index >= length) throw new IllegalArgumentException("Index out of range");
        return this.bValues[index];
    }

    public double[] get() {
        return this.values;
    }

    public boolean set(int index, double value) throws IllegalArgumentException {
        if (this.values == null) return false;
        if (index >= this.length) throw new IllegalArgumentException("Index out of range");
        this.values[index] = value;
        return true;
    }

    public static Comparator<Map.Entry<Integer, Double>> valueComparator = new Comparator<Map.Entry<Integer, Double>>() {
        @Override
        public int compare(Map.Entry<Integer, Double> t1, Map.Entry<Integer, Double> t2) {
            Double v1 = t1.getValue();
            Double v2 = t2.getValue();
            if (v1 == null || v2 == null) return -1;
            if (v1 > v2) {
                return -1;
            } else if (v2 > v1) {
                return 1;
            }
            return 0;
        }
    };
}
