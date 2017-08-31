package com.WAT.airbnb.rest.entities;

public class MessageEntity {
    private String token;
    private String subject;
    private String message;
    private int messageId;
    private int senderId;
    private int receiverId;
    private String name;

    public MessageEntity() {}

    public String getSubject() { return this.subject; }

    public void setSubject(String subject) { this.subject = subject; }

    public String getName() { return this.name; }

    public void setName(String name) { this.name = name; }

    public String getToken() { return this.token; }

    public void setToken(String token) { this.token = token; }

    public String getMessage() { return this.message; }

    public void setMessage(String message) { this.message = message; }

    public int getMessageId() { return this.messageId; }

    public void setMessageId(int messageId) { this.messageId = messageId; }

    public int getSenderId() { return this.senderId; }

    public void setSenderId(int senderId) { this.senderId = senderId; }

    public int getReceiverId() { return this.receiverId; }

    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }
}
