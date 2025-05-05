package org.views;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.dialogs.user.RechargeCreditDialog;
import org.dialogs.user.StartRideDialog;
import org.models.BikeViewModel;
import org.models.UserViewModel;
import org.verticles.UserVerticle;

import javax.swing.*;
import java.awt.*;

public class UserView extends AbstractView {

    private final UserVerticle verticle;
    private final Vertx vertx;
    private JButton rideButton;
    private JButton callABike;
    private boolean isRiding = false;


    public UserView(UserViewModel user, Vertx vertx) {
        super("User View", user);
        this.vertx = vertx;
        this.verticle = new UserVerticle(vertx, user.username());
        setupView();
        this.verticle.init();
        observeAvailableBikes();
        observeUser();
        observeRideUpdate();
        observeStations();
        refreshView();
    }

    private void setupView() {
        topPanel.setLayout(new FlowLayout());

        rideButton = new JButton("Start Ride");
        rideButton.addActionListener(e -> toggleRide());
        buttonPanel.add(rideButton);

        callABike = new JButton("Call ABike");
        buttonPanel.add(callABike);

        callABike.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                org.dialogs.user.CallBikeDialog dialog = new org.dialogs.user.CallBikeDialog(UserView.this, vertx, actualUser);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    String bikeId = dialog.getBikeId();
                    double x = dialog.getPosX();
                    double y = dialog.getPosY();
                    // TODO: Handle the call bike logic here, e.g., send a request to the backend
                    JOptionPane.showMessageDialog(UserView.this, "Requested bike " + bikeId + " to position (" + x + ", " + y + ")");
                }
            });
        });

        addTopPanelButton("Recharge Credit", e -> {
            SwingUtilities.invokeLater(() -> {
                RechargeCreditDialog rechargeCreditDialog = new RechargeCreditDialog(UserView.this, vertx, actualUser);
                rechargeCreditDialog.setVisible(true);
            });
        });
        updateRideButtonState();
    }

    private void toggleRide() {
        if (isRiding) {
            stopRide();
        } else {
            startRide();
        }
    }

    public void setRiding(boolean isRiding) {
        this.isRiding = isRiding;
        updateRideButtonState();
    }

    private void updateRideButtonState() {
        rideButton.setText(isRiding ? "Stop Ride" : "Start Ride");
    }

    private void startRide() {
        SwingUtilities.invokeLater(() -> {
            StartRideDialog startRideDialog = new StartRideDialog(UserView.this, vertx, actualUser);
            startRideDialog.setVisible(true);
            refreshView();
        });
    }

    private void stopRide() {
        SwingUtilities.invokeLater(() -> {
            JsonObject rideDetails = new JsonObject().put("username", actualUser.username());
            vertx.eventBus().request("user.ride.stop." + actualUser.username(), rideDetails, ar -> {
                SwingUtilities.invokeLater(() -> {
                    if (ar.succeeded()) {
                        JOptionPane.showMessageDialog(this, "Ride stopped");
                        setRiding(false);
                    } else {
                        JOptionPane.showMessageDialog(this, "Error stopping ride: " + ar.cause().getMessage());
                    }
                });
            });
            refreshView();
        });
    }

    private void observeRideUpdate() {
        vertx.eventBus().consumer("user.ride.update." + actualUser.username(), message -> {
            JsonObject update = (JsonObject) message.body();
            if (update.containsKey("rideStatus")) {
                String status = update.getString("rideStatus");
                if(status.equals("stopped")){
                    setRiding(false);
                }
                refreshView();
            }
        });
    }

    private void observeAvailableBikes() {
        vertx.eventBus().consumer("user.bike.update." + actualUser.username(), message -> {
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
                    BikeViewModel.EBikeState state = BikeViewModel.EBikeState.valueOf(stateStr);
                    BikeViewModel.BikeType type = BikeViewModel.BikeType.valueOf(typeStr);

                    BikeViewModel bikeModel = new BikeViewModel(id, x, y, batteryLevel, state, type);
                    eBikes.add(bikeModel);
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

    public void observeUser(){
        vertx.eventBus().consumer("user.update." + actualUser.username(), message -> {
            JsonObject update = (JsonObject) message.body();

            String username = update.getString("username");
            int credit = update.getInteger("credit");
            if (username.equals(actualUser.username())) {
                actualUser = actualUser.updateCredit(credit);
            }
            refreshView();

        });
    }

    private void refreshView() {
        updateVisualizerPanel();
    }

    private void log(String msg) {
        System.out.println("[UserView-"+actualUser.username()+"] " + msg);
    }

}