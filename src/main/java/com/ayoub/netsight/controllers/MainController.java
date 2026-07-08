package com.ayoub.netsight.controllers;


import com.ayoub.netsight.model.HostInfo;
import com.ayoub.netsight.services.ScanService;
import com.ayoub.netsight.utils.NetworkUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;

public class MainController {

    // Toolbar controls
    @FXML private TextField tfSubnet;
    @FXML private TextField tfStart;
    @FXML private TextField tfEnd;
    @FXML private TextField tfPortFrom;
    @FXML private TextField tfPortTo;
    @FXML private Slider slThreads;
    @FXML private Label lblThreads;
    @FXML private Slider slTimeout;
    @FXML private Label lblTimeout;
    @FXML private Button btnScan;
    @FXML private Button btnStop;

    // Results table
    @FXML private TableView<HostInfo> table;
    @FXML private TableColumn<HostInfo, String> colIp;
    @FXML private TableColumn<HostInfo, String> colHost;
    @FXML private TableColumn<HostInfo, String> colStatus;
    @FXML private TableColumn<HostInfo, Long>   colPing;
    @FXML private TableColumn<HostInfo, String> colPorts;

    // Status bar
    @FXML private ProgressBar pbProgress;
    @FXML private Label lblStatus;
    @FXML private Label lblFound;

    private final ObservableList<HostInfo> results = FXCollections.observableArrayList();
    private final ScanService scanService = new ScanService();

    @FXML
    public void initialize() {
        // Prefill subnet from local network interface
        String localIp = NetworkUtil.getLocalIp();
        tfSubnet.setText(NetworkUtil.toSubnet(localIp));
        tfStart.setText("1");
        tfEnd.setText("254");

        // Bind table columns to HostInfo Observable properties
        colIp    .setCellValueFactory(new PropertyValueFactory<>("ip"));
        colHost  .setCellValueFactory(new PropertyValueFactory<>("hostname"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPing  .setCellValueFactory(new PropertyValueFactory<>("ping"));
        colPorts .setCellValueFactory(new PropertyValueFactory<>("ports"));
        table.setItems(results);

        // Slider live-update labels
        slThreads.valueProperty().addListener((o, ov, nv) ->
                lblThreads.setText(String.valueOf(nv.intValue())));
        slTimeout.valueProperty().addListener((o, ov, nv) ->
                lblTimeout.setText(nv.intValue() + " ms"));

        slThreads.setValue(50);
        slTimeout.setValue(500);
        btnStop.setDisable(true);
    }

    @FXML
    private void onScan() {
        results.clear();
        pbProgress.setProgress(0);
        lblFound.setText("0 hosts");
        btnScan.setDisable(true);
        btnStop.setDisable(false);

        String subnetInput = tfSubnet.getText().trim();
        String startText = tfStart.getText().trim();
        String endText = tfEnd.getText().trim();

        String subnet;
        int start, end;

        // Single IP mode — if From/To are empty
        if (startText.isEmpty() || endText.isEmpty()) {
            //  "192.168.110.200" → subnet="192.168.110." start=200 end=200
            int lastDot = subnetInput.lastIndexOf('.');
            if (lastDot == -1) {
                lblStatus.setText("Invalid IP");
                btnScan.setDisable(false);
                btnStop.setDisable(true);
                return;
            }
            subnet = subnetInput.substring(0, lastDot + 1);
            start  = Integer.parseInt(subnetInput.substring(lastDot + 1));
            end    = start;
        } else {
            // Range mode — subnet field should be "192.168.1." with From/To filled
            subnet = subnetInput.endsWith(".") ? subnetInput : subnetInput + ".";
            start  = Integer.parseInt(startText);
            end    = Integer.parseInt(endText);
        }

        lblStatus.setText("Scanning…");
        int threads = (int) slThreads.getValue();
        int timeout = (int) slTimeout.getValue();

        int portFrom = tfPortFrom.getText().trim().isEmpty() ? 1    : Integer.parseInt(tfPortFrom.getText().trim());
        int portTo   = tfPortTo.getText().trim().isEmpty()   ? 9999 : Integer.parseInt(tfPortTo.getText().trim());

        List<Integer> portsToScan = new ArrayList<>();
        for (int p = portFrom; p <= portTo; p++) {
            portsToScan.add(p);
        }

        scanService.scan(subnet, start, end, threads, timeout,portsToScan,
                host -> {
                    results.add(host);
                    int n = results.size();
                    lblFound.setText(n + " host" + (n == 1 ? "" : "s") + " found");
                },
                (done, total) -> {
                    pbProgress.setProgress((double) done / total);
                    if (done >= total) {
                        lblStatus.setText("Scan complete");
                        btnScan.setDisable(false);
                        btnStop.setDisable(true);
                    }
                }
        );
    }

    @FXML
    private void onStop() {
        scanService.stop();
        lblStatus.setText("Stopped");
        btnScan.setDisable(false);
        btnStop.setDisable(true);
    }
}
