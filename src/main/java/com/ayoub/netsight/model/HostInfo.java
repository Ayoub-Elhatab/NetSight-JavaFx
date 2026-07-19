package com.ayoub.netsight.model;

import javafx.beans.property.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one discovered host on the LAN.
 * Uses JavaFX StringProperty / LongProperty so TableView columns bind and update automatically.
 *
 * @author Ayoub Elhatab
 * LinkedIn: <a href="https://www.linkedin.com/in/ayoub-elhatab/">Ayoub Elhatab</a>
 */
public class HostInfo {

    private final StringProperty ip = new SimpleStringProperty();
    private final StringProperty hostname = new SimpleStringProperty("Resolving…");
    private final StringProperty status = new SimpleStringProperty("UP");
    private final LongProperty   ping = new SimpleLongProperty();
    private final StringProperty ports = new SimpleStringProperty("—");

    private boolean alive;
    private long pingMs;
    private List<Integer> openPorts = new ArrayList<>();

    public HostInfo(String ip) {
        this.ip.set(ip);
    }

    public StringProperty ipProperty() { return ip; }
    public StringProperty hostnameProperty() { return hostname; }
    public StringProperty statusProperty() { return status; }
    public LongProperty   pingProperty() { return ping; }
    public StringProperty portsProperty() { return ports; }

    public String getIp() { return ip.get(); }
    public boolean isAlive() { return alive; }
    public long getPingMs() { return pingMs; }
    public List<Integer> getOpenPorts() { return openPorts; }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public void setHostname(String h) {
        hostname.set(h);
    }

    public void setPingMs(long ms) {
        pingMs = ms;
        ping.set(ms);
    }

    public void setOpenPorts(List<Integer> p) {
        openPorts = p;
        // "80,443,22" : strip brackets and spaces from List.toString()
        ports.set(p.isEmpty() ? "—"
                : p.toString().replaceAll("[\\[\\] ]", ""));
    }
}
