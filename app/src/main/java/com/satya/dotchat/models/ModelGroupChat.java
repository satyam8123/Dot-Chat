package com.satya.dotchat.models;

public class ModelGroupChat {
    String message,sender,timestamp,type,name;

    public ModelGroupChat() {
    }

    public ModelGroupChat(String message, String sender, String timestamp, String type, String name) {
        this.message = message;
        this.sender = sender;
        this.timestamp = timestamp;
        this.type = type;
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
