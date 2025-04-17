package org.views;


import org.models.EBikeViewModel;
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
            g2.drawString("(x: " + bike.x() + ", y: " + bike.y() + ")", x, y + 50);
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
            g2.drawString("(x: " + bike.x() + ", y: " + bike.y() + ")", x, y + 50);
            dy += 15;
        }
        String credit = "Credit: " + actualUser.credit();
        g2.drawString(credit, 10, 20);
        g2.drawString("AVAILABLE EBIKES: ", 10, 35);
    }

    public void display() {
        SwingUtilities.invokeLater(() -> this.setVisible(true));
    }
}