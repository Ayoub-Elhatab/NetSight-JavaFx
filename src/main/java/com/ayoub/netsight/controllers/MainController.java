package com.ayoub.netsight.controllers;


import com.ayoub.netsight.model.HostInfo;
import com.ayoub.netsight.services.ScanService;
import com.ayoub.netsight.utils.NetworkUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import java.util.ArrayList;
import java.util.List;
import static com.ayoub.netsight.utils.JavaFxUtils.showInfo;

public class MainController {

    // Toolbar controls
    @FXML private TextField tfSubnet;
    @FXML private TextField tfStart;
    @FXML private TextField tfEnd;
    @FXML private TextField tfPortFrom;
    @FXML private TextField tfPortTo;
    @FXML private ComboBox<String> cbMode;
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

    private long scanStartTime;
    private int  totalScanned;
    private int  totalAlive;
    private int  totalWithPorts;

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

        cbMode.getItems().addAll("Fast", "Normal", "Deep");
        cbMode.setValue("Normal");

        btnStop.setDisable(true);

        showRightClickMenu();

    }

    @FXML
    private void onScan() {
        results.clear();
        pbProgress.setProgress(0);
        lblFound.setText("0 hosts");
        btnScan.setDisable(true);
        btnStop.setDisable(false);

        scanStartTime = System.currentTimeMillis();
        totalScanned  = 0;
        totalAlive    = 0;
        totalWithPorts = 0;

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

        int portFrom = tfPortFrom.getText().trim().isEmpty() ? 1    : Integer.parseInt(tfPortFrom.getText().trim());
        int portTo   = tfPortTo.getText().trim().isEmpty()   ? 1024 : Integer.parseInt(tfPortTo.getText().trim());

        List<Integer> portsToScan = new ArrayList<>();
        for (int p = portFrom; p <= portTo; p++) {
            portsToScan.add(p);
        }

        int threads, timeout;
        switch (cbMode.getValue()) {
            case "Fast" -> { threads = 100; timeout = 100; }
            case "Deep" -> { threads = 30;  timeout = 500; }
            default     -> { threads = 50;  timeout = 200; }
        }

        scanService.scan(subnet, start, end, threads, timeout,portsToScan,
                host -> {
                    results.add(host);
                    totalAlive++;
                    if (!host.getOpenPorts().isEmpty()) totalWithPorts++;
                    int n = results.size();
                    lblFound.setText(n + " host" + (n == 1 ? "" : "s") + " found");
                },
                (done, total) -> {
                    pbProgress.setProgress((double) done / total);
                    totalScanned = done;
                    if (done >= total) {
                        lblStatus.setText("Scan complete");
                        btnScan.setDisable(false);
                        btnStop.setDisable(true);
                        showScanStatistics(total);
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

    private void showScanStatistics(int totalHosts) {
        long elapsed = System.currentTimeMillis() - scanStartTime;
        double totalSec   = elapsed / 1000.0;
        double avgPerHost = totalSec / totalHosts;

        String subnetInput = tfSubnet.getText().trim();
        String startText   = tfStart.getText().trim();
        String endText     = tfEnd.getText().trim();

        String rangeText;
        if (startText.isEmpty() || endText.isEmpty()) {
            rangeText = subnetInput;
        } else {
            String subnet = subnetInput.endsWith(".") ? subnetInput : subnetInput + ".";
            rangeText = subnet + startText + " - " + subnet + endText;
        }

        String content = "Total time: "        + String.format("%.2f", totalSec)   + " sec\n" +
                "Average time/host: " + String.format("%.2f", avgPerHost) + " sec\n" +
                "IP Range: "          + rangeText                          + "\n" +
                "Hosts scanned: "     + totalHosts                         + "\n" +
                "Hosts alive: "       + totalAlive                         + "\n" +
                "With open ports: "   + totalWithPorts;

        showInfo("Scan Statistics",content);

    }

    private void showRightClickMenu(){
        ContextMenu contextMenu = new ContextMenu();

        MenuItem menuDetails = new MenuItem("Show details");
        MenuItem menuRescan  = new MenuItem("Rescan IP");
        MenuItem menuCopyIp  = new MenuItem("Copy IP");
        MenuItem menuCopyDetails = new MenuItem("Copy details");

        contextMenu.getItems().addAll(menuDetails, menuRescan, new SeparatorMenuItem(), menuCopyIp, menuCopyDetails);

        // only show when clicking on a row
        table.setRowFactory(tv -> {
            TableRow<HostInfo> row = new TableRow<>();
            row.setOnContextMenuRequested(e -> {
                if (!row.isEmpty()) {
                    table.getSelectionModel().select(row.getItem());
                    contextMenu.show(row, e.getScreenX(), e.getScreenY());
                }
            });
            return row;
        });

        // Show details
        menuDetails.setOnAction(e -> {
            HostInfo h = table.getSelectionModel().getSelectedItem();
            if (h == null) return;

            String content = "IP:        " + h.getIp() + "\n" +
                    "Ping:      " + h.getPingMs() + " ms\n" +
                    "Hostname:  " + h.hostnameProperty().get() + "\n" +
                    "Ports:     " + (h.getOpenPorts().isEmpty() ? "—" : h.getOpenPorts().toString().replaceAll("[\\[\\] ]", ""));

            showInfo("IP address details",content);

        });

        // Rescan IP
        menuRescan.setOnAction(e -> {
            HostInfo h = table.getSelectionModel().getSelectedItem();
            if (h == null) return;
            results.remove(h);
            tfSubnet.setText(h.getIp());
            tfStart.setText("");
            tfEnd.setText("");
            onScan();
        });

        // Copy IP
        menuCopyIp.setOnAction(e -> {
            HostInfo h = table.getSelectionModel().getSelectedItem();
            if (h == null) return;
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(
                    new javafx.scene.input.ClipboardContent() {{ putString(h.getIp()); }}
            );
        });

        // Copy details
        menuCopyDetails.setOnAction(e -> {
            HostInfo h = table.getSelectionModel().getSelectedItem();
            if (h == null) return;
            String details =
                    "IP:        " + h.getIp() + "\n" +
                            "Ping:      " + h.getPingMs() + " ms\n" +
                            "Hostname:  " + h.hostnameProperty().get() + "\n" +
                            "Ports:     " + (h.getOpenPorts().isEmpty() ? "—" : h.getOpenPorts().toString().replaceAll("[\\[\\] ]", ""));

            Clipboard.getSystemClipboard().setContent(new ClipboardContent() {{ putString(details); }});
        });
    }
}