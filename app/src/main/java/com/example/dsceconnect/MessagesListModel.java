package com.example.dsceconnect;


public class MessagesListModel {

    public String readstatus;

    public MessagesListModel()
    {

    }

    public MessagesListModel(String readstatus) {
        this.readstatus = readstatus;
    }

    public String getReadstatus() {
        return readstatus;
    }

    public void setReadstatus(String readstatus) {
        this.readstatus = readstatus;
    }
}
