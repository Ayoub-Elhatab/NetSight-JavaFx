package com.ayoub.netsight.controllers;


import com.ayoub.netsight.NetSightApp;
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
import javafx.stage.FileChooser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.ayoub.netsight.utils.JavaFxUtils.showInfo;

/**
 * Main controller for the NetSight UI.
 * Handles user interactions, orchestrates subnet scanning via {@link ScanService},
 * and updates the results table, progress bar, and status labels accordingly.
 *
 * @author Ayoub Elhatab
 * LinkedIn: <a href="https://www.linkedin.com/in/ayoub-elhatab/">Ayoub Elhatab</a>
 */
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
    @FXML private Button btnClear;
    @FXML private Button btnExport;

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

    /**
     * Initializes the controller after the FXML is loaded.
     * Pre-fills the subnet field from the local network interface,
     * binds table columns to {@link HostInfo} properties, and sets up the right-click menu.
     */
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
        btnClear.setDisable(true);
        btnExport.setDisable(true);

        showRightClickMenu();
    }

    /**
     * Triggered when the user clicks the Scan button.
     * Resolves the scan mode, builds the port list and IP range,
     * then delegates to {@link ScanService} to run the concurrent scan.
     * Supports both single IP mode and range mode.
     */
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
                        btnClear.setDisable(false);
                        btnExport.setDisable(false);
                        showScanStatistics(total);
                    }
                }
        );
    }

    /**
     * Triggered when the user clicks the Stop button.
     * Immediately cancels all pending scan tasks via {@link ScanService#stop()}.
     */
    @FXML
    private void onStop() {
        scanService.stop();
        lblStatus.setText("Stopped");
        btnScan.setDisable(false);
        btnStop.setDisable(true);
    }

    /**
     * Triggered when the user clicks the Clear button.
     * Clears the results table, resets the progress bar and host counter,
     * and disables the Export and Clear buttons.
     */
    @FXML
    private void onClear() {
        results.clear();
        pbProgress.setProgress(0);
        lblFound.setText("0 hosts");
        btnExport.setDisable(true);
        btnClear.setDisable(true);
    }

    /**
     * Triggered when the user clicks the Export button.
     * Opens a save dialog and writes all current scan results to a {@code .txt} file.
     * Each host entry includes IP, hostname, ping, and open ports.
     */
    @FXML
    private void onExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export results");
        fileChooser.setInitialFileName("scan-results.txt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File file = fileChooser.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.newLine();
            writer.newLine();

            for (HostInfo h : results) {
                writer.write("IP:       " + h.getIp());
                writer.newLine();
                writer.write("Hostname: " + h.hostnameProperty().get());
                writer.newLine();
                writer.write("Ping:     " + h.getPingMs() + " ms");
                writer.newLine();
                writer.write("Ports:    " + (h.getOpenPorts().isEmpty() ? "—"
                        : h.getOpenPorts().toString().replaceAll("[\\[\\] ]", "")));
                writer.newLine();
                writer.write("----------------------------------------------------------");
                writer.newLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Displays a statistics dialog after a scan completes.
     * Shows total time, average time per host, IP range scanned,
     * total hosts scanned, alive hosts, and hosts with open ports.
     *
     * @param totalHosts the total number of IP addresses that were probed
     */
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

        String content = "Total time: "        + String.format("%.2f", totalSec)   + " sec\n\n" +
                "Average time/host: " + String.format("%.2f", avgPerHost) + " sec\n\n" +
                "IP Range: "          + rangeText                          + "\n\n" +
                "Hosts scanned: "     + totalHosts                         + "\n\n" +
                "Hosts alive: "       + totalAlive                         + "\n\n" +
                "With open ports: "   + totalWithPorts;

        showInfo("Scan Statistics",content);

    }

    /**
     * Builds and attaches a right-click context menu to the results table.
     * Available actions: Show details, Rescan IP, Copy IP, Copy details.
     * Menu is only shown when right-clicking on a non-empty row.
     */
    private void showRightClickMenu(){
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add(Objects.requireNonNull(NetSightApp.class.getResource("/css/style.css")).toExternalForm());

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
            Clipboard.getSystemClipboard().setContent(
                    new ClipboardContent() {{ putString(h.getIp()); }}
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