package com.WAT.airbnb.etc;

public class QueryBuilder {
    private StringBuilder builder;

    public QueryBuilder() {
        builder = new StringBuilder();
    }

    public QueryBuilder(String startingQuery) {
        builder = new StringBuilder(startingQuery);
    }

    public QueryBuilder and(String str) {
        builder.append("and ").append(str).append(" ");
        return this;
    }

    public QueryBuilder or(String str) {
        builder.append("or ").append(str).append(" ");
        return this;
    }

    public QueryBuilder append(String str) {
        builder.append(str);
        return this;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
