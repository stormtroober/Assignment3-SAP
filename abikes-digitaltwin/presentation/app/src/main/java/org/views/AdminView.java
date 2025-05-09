package org.views;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.dialogs.admin.AddABikeDialog;
import org.dialogs.admin.AddEBikeDialog;
import org.dialogs.admin.RechargeBikeDialog;
import org.models.*;
import org.verticles.AdminVerticle;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AdminView extends AbstractView {

    private final List<UserViewModel> userList = new CopyOnWriteArrayList<>();
    private final AdminVerticle verticle;
    private final Vertx vertx;

    public AdminView(UserViewModel user, Vertx vertx) {
        super("Admin View", user);
        this.vertx = vertx;
        this.verticle = new AdminVerticle(vertx);
        this.verticle.init();
        setupView();
        observeAllBikes();
        observeAllUsers();
        observeStations();
        refreshView();
    }

    private void setupView() {
        topPanel.setLayout(new FlowLayout());

        JButton addABikeButton = new JButton("Add A-Bike");
        addABikeButton.addActionListener(e -> {
            AddABikeDialog addABikeDialog = new AddABikeDialog(AdminView.this, vertx);
            addABikeDialog.setVisible(true);
        });
        topPanel.add(addABikeButton);

        JButton addBikeButton = new JButton("Add Bike");
        addBikeButton.addActionListener(e -> {
            AddEBikeDialog addEBikeDialog = new AddEBikeDialog(AdminView.this, vertx);
            addEBikeDialog.setVisible(true);
        });
        topPanel.add(addBikeButton);

        JButton rechargeBikeButton = new JButton("Recharge Bike");
        rechargeBikeButton.addActionListener(e -> {
            RechargeBikeDialog rechargeBikeDialog = new RechargeBikeDialog(AdminView.this, vertx);
            rechargeBikeDialog.setVisible(true);
        });
        topPanel.add(rechargeBikeButton);

    }

    private void observeAllBikes() {
        vertx.eventBus().consumer("admin.bike.update", message -> {
            JsonArray update = (JsonArray) message.body();
            eBikes.clear();
            for (int i = 0; i < update.size(); i++) {
                Object element = update.getValue(i);
                if (element instanceof String) {
                    JsonObject bikeObj = new JsonObject((String) element);
                    String id = bikeObj.getString("bikeName");
                    Integer batteryLevel = bikeObj.getInteger("batteryLevel");
                    String stateStr = bikeObj.getString("state");
                    String typeStr = bikeObj.getString("type");

                    JsonObject location = bikeObj.getJsonObject("position");
                    Double x = location.getDouble("x");
                    Double y = location.getDouble("y");

                    BikeType bikeType;
                    try {
                        bikeType = BikeType.valueOf(typeStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log("Invalid bike type: " + typeStr);
                        continue;
                    }

                    switch(bikeType) {
                        case NORMAL:
                            EBikeState stateEbike = EBikeState.valueOf(stateStr);
                            EBikeViewModel eBikeModel = new EBikeViewModel(id, x, y, batteryLevel, stateEbike, bikeType);
                            eBikes.add(eBikeModel);
                            break;
                        case AUTONOMOUS:
                            ABikeState stateAbike = ABikeState.valueOf(stateStr);
                            ABikeViewModel aBikeModel = new ABikeViewModel(id, x, y, batteryLevel, stateAbike, bikeType);
                            aBikes.add(aBikeModel);
                            break;
                        default:
                            log("Invalid bike state: " + stateStr);
                    }
                } else {
                    log("Invalid bike data: " + element);
                }
            }
            refreshView();
        });
    }

    private void observeStations() {
        observeStationsToList(vertx);
        refreshView();
    }

    private void observeAllUsers() {
        vertx.eventBus().consumer("admin.user.update", message -> {
            JsonObject update = (JsonObject) message.body();
            String username = update.getString("username");
            String type = update.getString("type");
            Integer credit = update.getInteger("credit");

            if (type.equals("USER") && userList.stream().noneMatch(user -> user.username().equals(username))) {
                UserViewModel user = new UserViewModel(username, credit , false);
                userList.add(user);
            } else if (type.equals("USER")) {
                System.out.println("Updating user: " + username);
                userList.stream()
                        .filter(u -> u.username().equals(username))
                        .findFirst()
                        .ifPresent(u -> {
                            userList.remove(u);
                            userList.add(new UserViewModel(username, credit, false));
                        });
            }
            System.out.println("Received user update: " + update);
            refreshView();
        });
    }

    @Override
    protected void paintAdminView(Graphics2D g2) {
        super.paintAdminView(g2);
        printAllUsers(g2);
    }

    private void printAllUsers(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        int dy = 20;
        g2.drawString("ALL USERS: ", 10, dy);
        dy += 15;
        for (UserViewModel user : userList) {
            g2.drawString("User ID: " + user.username() + " - Credit: " + user.credit(), 10, dy);
            dy += 15;
        }
    }

    public void refreshView() {
        updateVisualizerPanel();
    }

    private void log(String msg) {
        System.out.println("[AdminView] " + msg);
    }
}