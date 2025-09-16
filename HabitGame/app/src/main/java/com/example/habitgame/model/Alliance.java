package com.example.habitgame.model;

import java.util.ArrayList;
import java.util.List;

public class Alliance {
    private String id;
    private String name;
    private String leader;
    private List<Account> members;

    public Alliance() {}
    public Alliance(String name, String leader) {
        this.name = name;
        this.leader = leader;
        members = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public List<Account> getMembers() {
        return members;
    }

    public void setMembers(List<Account> members) {
        this.members = members;
    }
}
