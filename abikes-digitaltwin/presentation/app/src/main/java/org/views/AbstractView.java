package org.views;


import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.models.BikeViewModel;
import org.models.StationViewModel;
import org.models.UserViewModel;

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

    protected List<BikeViewModel> eBikes;
    protected List<StationViewModel> stations;
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
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        topPanel.add(logoutButton, BorderLayout.EAST);

        this.actualUser = actualUser;
        this.eBikes = new CopyOnWriteArrayList<>();
        this.stations = new CopyOnWriteArrayList<>();
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
                            .map(Object::toString)
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
     * Draws all e‑bikes (shared by admin and user views).
     */
    private void paintEBikes(Graphics2D g2) {
        int centerX = centralPanel.getWidth() / 2;
        int centerY = centralPanel.getHeight() / 2;
        for (BikeViewModel bike : eBikes) {
            int x = centerX + (int) bike.x();
            int y = centerY - (int) bike.y();
            g2.setColor(bike.color());
            g2.fillOval(x, y, 20, 20);
            g2.drawString("STATUS: " + bike.state(), x, y + 65);

            if (bike.type() == BikeViewModel.BikeType.AUTONOMOUS) {
                g2.drawString("A-Bike: " + bike.id() + " - battery: " + bike.batteryLevel(), x, y + 35);
                g2.drawString(String.format("(x: %.2f, y: %.2f)", bike.x(), bike.y()), x, y + 50);
                g2.setColor(Color.RED);
                g2.drawString("A", x + 6, y + 15);
            } else {
                g2.setColor(Color.BLACK);
                g2.drawString("E-Bike: " + bike.id() + " - battery: " + bike.batteryLevel(), x, y + 35);
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

            // station square
            g2.setColor(Color.BLUE);
            g2.fillRect(sx - stationSize/2, sy - stationSize/2, stationSize, stationSize);

            // station slots
            int slotCount = station.getMaxSlots();
            for (int i = 0; i < slotCount; i++) {
                double angle = 2 * Math.PI * i / slotCount;
                int slotX = sx + (int)(slotRadius * Math.cos(angle)) - slotSize/2;
                int slotY = sy + (int)(slotRadius * Math.sin(angle)) - slotSize/2;
                boolean filled = i < station.getSlots().size();
                g2.setColor(filled ? Color.LIGHT_GRAY : Color.WHITE);
                g2.fillRect(slotX, slotY, slotSize, slotSize);
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(slotX, slotY, slotSize, slotSize);
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

        String credit = "Credit: " + actualUser.credit();
        g2.drawString(credit, 10, 20);
        g2.drawString("AVAILABLE EBIKES: ", 10, 35);
    }

    public void display() {
        SwingUtilities.invokeLater(() -> this.setVisible(true));
    }
}