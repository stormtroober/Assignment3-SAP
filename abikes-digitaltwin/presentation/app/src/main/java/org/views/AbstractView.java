package org.views;


import org.models.EBikeViewModel;
import org.models.StationViewModel;
import org.models.UserViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractView extends JFrame {

    protected JPanel topPanel;
    protected JPanel centralPanel;
    protected JButton logoutButton;
    protected JPanel buttonPanel;

    protected List<EBikeViewModel> eBikes;
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

    protected void paintAdminView(Graphics2D g2) {
        int centerX = centralPanel.getWidth() / 2;
        int centerY = centralPanel.getHeight() / 2;
        for (EBikeViewModel bike : eBikes) {
            int x = centerX + (int) bike.x();
            int y = centerY - (int) bike.y();
            g2.setColor(bike.color());
            g2.fillOval(x, y, 20, 20);
            g2.setColor(Color.BLACK);
            g2.drawString("E-Bike: " + bike.id() + " - battery: " + bike.batteryLevel(), x, y + 35);
            g2.drawString(
                String.format("(x: %.2f, y: %.2f)", bike.x(), bike.y()), x, y + 50
            );
            g2.drawString("STATUS: " + bike.state(), x, y + 65);
        }
    }

    private void paintUserView(Graphics2D g2) {
        int centerX = centralPanel.getWidth() / 2;
        int centerY = centralPanel.getHeight() / 2;
        int dy = 20;
        for (EBikeViewModel bike : eBikes) {
            int x = centerX + (int) bike.x();
            int y = centerY - (int) bike.y();
            g2.setColor(bike.color());
            g2.fillOval(x, y, 20, 20);
            g2.setColor(Color.BLACK);
            g2.drawString("E-Bike: " + bike.id() + " - battery: " + bike.batteryLevel(), x, y + 35);
            g2.drawString("E-Bike: " + bike.id() + " - battery: " + bike.batteryLevel(), 10, dy + 35);
            g2.drawString(
                String.format("(x: %.2f, y: %.2f)", bike.x(), bike.y()), x, y + 50
            );
            dy += 15;
        }

        // Draw stations as squares with slots around them
        int stationSize = 20;
        int slotSize = 8;
        int slotRadius = 18;
        for (StationViewModel station : stations) {
            int sx = centerX + (int) station.getX();
            int sy = centerY - (int) station.getY();

            // Draw station as a square
            g2.setColor(Color.BLUE);
            g2.fillRect(sx - stationSize / 2, sy - stationSize / 2, stationSize, stationSize);

            // Draw slots as small squares around the station
            int slotCount = station.getMaxSlots();
            for (int i = 0; i < slotCount; i++) {
                double angle = 2 * Math.PI * i / slotCount;
                int slotX = sx + (int) (slotRadius * Math.cos(angle)) - slotSize / 2;
                int slotY = sy + (int) (slotRadius * Math.sin(angle)) - slotSize / 2;
                // Filled slots are light gray, empty are white
                g2.setColor(i < station.getSlots().size() ? Color.LIGHT_GRAY : Color.WHITE);
                g2.fillRect(slotX, slotY, slotSize, slotSize);
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(slotX, slotY, slotSize, slotSize);
            }
        }

        String credit = "Credit: " + actualUser.credit();
        g2.drawString(credit, 10, 20);
        g2.drawString("AVAILABLE EBIKES: ", 10, 35);
    }

    public void display() {
        SwingUtilities.invokeLater(() -> this.setVisible(true));
    }
}