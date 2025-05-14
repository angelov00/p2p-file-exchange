package com.angelov00.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class User {

    private String username;
    private String ip;
    private int port;
    private Set<String> files;

    public User(String username, String ip, int port) {
        this.username = username;
        this.ip = ip;
        this.port = port;
        this.files = ConcurrentHashMap.newKeySet();
    }

    public String getUsername() {
        return username;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public Set<String> getFiles() {
        return files;
    }
}
