package com.WAT.airbnb.util;

/**
 * @author Kostas Kolivas
 * @version 1.0
 * @param <X>
 * @param <Y>
 */
public class Tuple<X, Y> {
    public X left;
    public Y right;

    public Tuple(X left, Y right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Tuple<?, ?>)) return false;
        return this.left.equals(((Tuple<?, ?>) other).left);
    }

    @Override
    public int hashCode() {
        int result = left != null ? left.hashCode() : 0;
        result = 31 * result + (right != null ? right.hashCode() : 0);
        return result;
    }
}
