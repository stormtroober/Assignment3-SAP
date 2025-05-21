package org.views;


import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.models.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public abstract class AbstractView extends JFrame {

    protected JPanel topPanel;
    protected JPanel centralPanel;
    protected JButton logoutButton;
    protected JPanel buttonPanel;

    protected List<EBikeViewModel> eBikes;
    protected List<ABikeViewModel> aBikes;
    protected List<StationViewModel> stations;
    protected List<DispatchViewModel> pendingDispatches;
    protected UserViewModel actualUser;


    public AbstractView(String title, UserViewModel actualUser) {
        setTitle(title);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        topPanel = new JPanel();
        add(topPanel, BorderLayout.NORTH);

        buttonPanel = new JPanel();
        topPanel.add(buttonPanel, BorderLayout.CENTER);

        centralPanel = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintCentralPanel(g);
            }
        };
        add(centralPanel, BorderLayout.CENTER);

        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> dispose());
        topPanel.add(logoutButton, BorderLayout.EAST);

        this.actualUser = actualUser;
        this.eBikes = new CopyOnWriteArrayList<>();
        this.aBikes = new CopyOnWriteArrayList<>();
        this.stations = new CopyOnWriteArrayList<>();
        this.pendingDispatches = new CopyOnWriteArrayList<>();
    }

    protected void addTopPanelButton(String text, ActionListener actionListener) {
        JButton button = new JButton(text);
        button.addActionListener(actionListener);
        topPanel.add(button);
    }

    protected void updateVisualizerPanel() {
        centralPanel.repaint();
    }

    protected void paintCentralPanel(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.clearRect(0, 0, this.getWidth(), this.getHeight());
        if (actualUser.admin()) {
            paintAdminView(g2);
        } else {
            paintUserView(g2);
        }
        paintDispatches(g2);
    }

    protected void observeDispatchesToList(Vertx vertx) {
        vertx.eventBus().consumer(
                "user.bike.dispatch." + actualUser.username(),
                message -> {
                    System.out.println("Received dispatch: " + message.body());
                    JsonObject json = (JsonObject) message.body();
                    if(json.getString("status").equals("dispatch")){
                        DispatchViewModel d = DispatchViewModel.fromJson(json);
                        pendingDispatches.add(d);
                    }
                    else if (json.getString("status").equals("arrived")
                            || json.getString("status").equals("notArrived")) {
                        String bikeId = json.getString("bikeId");
                        pendingDispatches.removeIf(d -> d.getBikeId().equals(bikeId));
                    }
                    else{
                        System.out.println("[AbstractView] Invalid dispatch data: " + message.body());
                    }
                    updateVisualizerPanel();
                }
        );
    }


    protected void observeStationsToList(Vertx vertx) {
        vertx.eventBus().consumer("stations.update", message -> {
            System.out.println("Received stations update: " + message.body());
            JsonArray update = (JsonArray) message.body();
            stations.clear();
            for (int i = 0; i < update.size(); i++) {
                Object element = update.getValue(i);
                if (element instanceof String) {
                    JsonObject stationObj = new JsonObject((String) element);
                    String id = stationObj.getString("id");
                    JsonObject location = stationObj.getJsonObject("location");
                    double x = location.getDouble("x");
                    double y = location.getDouble("y");
                    List<String> slots = stationObj.getJsonArray("slots")
                            .stream()
                            .map(slotRaw -> {
                                JsonObject slotObj = (JsonObject) slotRaw;
                                return slotObj.getString("abikeId"); // May be null
                            })
                            .collect(Collectors.toList());


                    int maxSlots = stationObj.getInteger("maxSlots", 0);
                    stations.add(new StationViewModel(id, x, y, slots, maxSlots));
                } else {
                    System.out.println("[AbstractView] Invalid station data: " + element);
                }
            }
        });
    }

    /**
     * Draw every pending dispatch as a red dot + label.
     */
    private void paintDispatches(Graphics2D g2) {
        int centerX = centralPanel.getWidth() / 2;
        int centerY = centralPanel.getHeight() / 2;

        for (DispatchViewModel d : pendingDispatches) {
            int x = centerX + (int) d.getX();
            int y = centerY - (int) d.getY();

            g2.setColor(d.getColor());
            g2.fillOval(x - 5, y - 5, 10, 10);
            g2.drawString("Waiting for bike " + d.getBikeId(), x + 8, y);
        }
    }

    /**
     * Draws all e‑bikes (shared by admin and user views).
     */
    private void paintEBikes(Graphics2D g2) {
        int centerX = centralPanel.getWidth() / 2;
        int centerY = centralPanel.getHeight() / 2;

        g2.setColor(Color.BLACK);
        for (EBikeViewModel bike : eBikes) {

            int x = centerX + (int) bike.x();
            int y = centerY - (int) bike.y();

            g2.fillOval(x, y, 20, 20);
            g2.drawString("STATUS: " + bike.state(), x, y + 65);

            g2.drawString("E-Bike: " + bike.id() + " - battery: " + bike.batteryLevel(), x, y + 35);
            g2.drawString(String.format("(x: %.2f, y: %.2f)", bike.x(), bike.y()), x, y + 50);
        }

        for(ABikeViewModel bike : aBikes) {
            boolean isBikeInStation = false;
            for(StationViewModel station : stations) {
                if(station.getSlots().contains(bike.id())) {
                    isBikeInStation = true;
                    break;
                }
            }
            if (!isBikeInStation) {
                int x = centerX + (int) bike.x();
                int y = centerY - (int) bike.y();

                g2.fillOval(x, y, 20, 20);
                g2.drawString("STATUS: " + bike.state(), x, y + 65);

                g2.drawString("ABike: " + bike.id() + " - battery: " + bike.batteryLevel(), x, y + 35);
                g2.drawString(String.format("(x: %.2f, y: %.2f)", bike.x(), bike.y()), x, y + 50);
            }

        }
    }

    private void paintStations(Graphics2D g2) {
        int centerX    = centralPanel.getWidth()  / 2;
        int centerY    = centralPanel.getHeight() / 2;
        int stationSize = 20;
        int slotSize    = 8;
        int slotRadius  = 18;

        for (StationViewModel station : stations) {
            int sx = centerX + (int) station.getX();
            int sy = centerY - (int) station.getY();

            // Draw station square
            g2.setColor(Color.BLUE);
            g2.fillRect(sx - stationSize/2, sy - stationSize/2, stationSize, stationSize);

            List<String> slots = station.getSlots();
            int slotCount = station.getMaxSlots();

            for (int i = 0; i < slotCount; i++) {
                double angle = 2 * Math.PI * i / slotCount;
                int slotX = sx + (int)(slotRadius * Math.cos(angle)) - slotSize/2;
                int slotY = sy + (int)(slotRadius * Math.sin(angle)) - slotSize/2;

                String bikeId = i < slots.size() ? slots.get(i) : null;
                boolean hasBike = bikeId != null && !bikeId.equals("null");

                if (hasBike) {
                    ABikeViewModel bike = aBikes.stream()
                            .filter(b -> b.id().equals(bikeId))
                            .findFirst()
                            .orElse(null);

                    if (bike != null) {
                        g2.setColor(Color.BLUE);
                        g2.fillOval(slotX - 6, slotY - 6, 20, 20);
                        g2.setColor(Color.RED);
                        g2.drawString(bike.id(), slotX + 2, slotY + 10);
                    }
                }
                else {
                    g2.setColor(Color.WHITE);
                    g2.fillRect(slotX, slotY, slotSize, slotSize);
                }
            }
        }
    }


    protected void paintAdminView(Graphics2D g2) {
        // draw all bikes
        paintEBikes(g2);
        // draw all stations
        paintStations(g2);
    }

    private void paintUserView(Graphics2D g2) {
        // draw all bikes
        paintEBikes(g2);
        // draw all stations
        paintStations(g2);

        paintUserInfo(g2);
        paintAvailableBikes(g2);
    }

    private void paintUserInfo(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        String credit = "Credit: " + actualUser.credit();
        g2.drawString(credit, 10, 20);
    }

    private void paintAvailableBikes(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        List<EBikeViewModel> availableBikes = eBikes.stream()
                .filter(bike -> bike.state() == EBikeState.AVAILABLE)
                .toList();

        List<ABikeViewModel> availableABikes = aBikes.stream()
                .filter(bike -> bike.state() == ABikeState.AVAILABLE)
                .toList();

        g2.drawString("AVAILABLE EBIKES: " + (availableBikes.size() + availableABikes.size()), 10, 35);

        int startX = 10;
        int startY = 45;
        int lineHeight = 14;

        int y = startY + lineHeight;
        g2.setColor(Color.BLACK);
        for (EBikeViewModel bike : availableBikes) {
            String bikeInfo = String.format(
                    "%s - Battery: %d%% - Type: %s",
                    bike.id(), bike.batteryLevel(), bike.type()
            );
            g2.drawString(bikeInfo, startX, y);
            y += lineHeight;
        }
        g2.setColor(Color.BLUE);
        for(ABikeViewModel bike : availableABikes) {
            String bikeInfo = String.format(
                    "%s - Battery: %d%% - Type: %s",
                    bike.id(), bike.batteryLevel(), bike.type()
            );
            g2.drawString(bikeInfo, startX, y);
            y += lineHeight;
        }
    }

    public void display() {
        SwingUtilities.invokeLater(() -> this.setVisible(true));
    }
}