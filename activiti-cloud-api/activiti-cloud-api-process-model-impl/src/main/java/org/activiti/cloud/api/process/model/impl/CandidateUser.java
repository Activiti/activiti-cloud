package org.activiti.cloud.api.process.model.impl;

public class CandidateUser {

    private String user;

    public CandidateUser(String user) {
        this.user = user;
    }

    public CandidateUser() {
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
