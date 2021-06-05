package com.example.dsceconnect;

public class FindFriends
{
    public String profileimage,fullname,status,designation;

    public  FindFriends()
    {

    }

    public FindFriends(String profileimage, String fullname, String status, String designation) {
        this.profileimage = profileimage;
        this.fullname = fullname;
        this.status = status;
        this.designation = designation;
    }

    public String getProfileimage() {
        return profileimage;
    }

    public void setProfileimage(String profileimage) {
        this.profileimage = profileimage;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}
