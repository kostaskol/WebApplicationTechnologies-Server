package com.WAT.airbnb.rest.entities;

public class CommentEntity {
    private String token;
    private int commentId;
    private int userId;
    private String userFName;
    private String userLName;
    private int houseId;
    private String comment;
    private float rating;

    public CommentEntity() {}

    public String getToken() { return this.token; }

    public void setToken(String token) { this.token = token; }

    public int getCommentId() { return this.commentId; }

    public void setCommentId(int commentId) { this.commentId = commentId; }

    public int getUserId() { return this.userId; }

    public void setUserId(int userId) { this.userId = userId; }

    public String getUserFName() { return this.userFName; }

    public void setUserFName(String userFName) { this.userFName = userFName; }

    public String getUserLName() { return this.userLName; }

    public void setUserLName(String userLName) { this.userLName = userLName; }

    public int getHouseId() { return this.houseId; }

    public void setHouseId(int houseId) { this.houseId = houseId; }

    public String getComment() { return this.comment; }

    public void setComment(String comment) { this.comment = comment; }

    public float getRating() { return this.rating; }

    public void setRating(float rating) { this.rating = rating; }
}
