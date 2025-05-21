package org.views;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.dialogs.user.RechargeCreditDialog;
import org.dialogs.user.StartRideDialog;
import org.models.*;
import org.verticles.UserVerticle;

import javax.swing.*;
import java.awt.*;

public class UserView extends AbstractView {

    private final UserVerticle verticle;
    private final Vertx vertx;
    private JButton rideButton;
    private JButton callABike;
    private boolean isRiding = false;
    private CallABikeStatus isCallingABike = CallABikeStatus.CALL_ABIKE;

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
        observeDispatches();
        observeCallABikeStatus();
        refreshView();
    }

    private void setupView() {
        topPanel.setLayout(new FlowLayout());

        rideButton = new JButton("Start Ride");
        rideButton.addActionListener(e -> toggleRide());
        buttonPanel.add(rideButton);

        callABike = new JButton("Call ABike");
        callABike.addActionListener(e -> toggleCallABike());
        buttonPanel.add(callABike);

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

    private void toggleCallABike() {
        SwingUtilities.invokeLater(() -> {
            switch (isCallingABike) {
                case CALL_ABIKE:
                    // Open the call bike dialog
                    org.dialogs.user.CallBikeDialog dialog = new org.dialogs.user.CallBikeDialog(UserView.this, vertx, actualUser);
                    dialog.setVisible(true);
                    break;

                case STOP_CALL_ABIKE:
                    // Cancel the call
                    JsonObject cancelDetails = new JsonObject().put("username", actualUser.username());
                    vertx.eventBus().request("user.ride.cancelCall." + actualUser.username(), cancelDetails, ar -> {
                        if (ar.succeeded()) {
                            JOptionPane.showMessageDialog(this, "Call canceled");
                            //setCallingABike(CallABikeStatus.CALL_ABIKE);
                        } else {
                            JOptionPane.showMessageDialog(this, "Error canceling call: " +
                                (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                        }
                    });
                    break;

                case STOP_RIDE_ABIKE:
                    // Stop the autonomous ride
                    JsonObject stopRideDetails = new JsonObject().put("username", actualUser.username());
                    vertx.eventBus().request("user.ride.stopARide." + actualUser.username(), stopRideDetails, ar -> {
                        if (ar.succeeded()) {
                            JOptionPane.showMessageDialog(this, "Autonomous ride stopped");
                            //setCallingABike(CallABikeStatus.CALL_ABIKE);
                        } else {
                            JOptionPane.showMessageDialog(this, "Error stopping autonomous ride: " +
                                (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                        }
                    });
                    break;
            }
            refreshView();
        });
    }

    public void setRiding(boolean isRiding) {
        this.isRiding = isRiding;
        updateRideButtonState();
    }

    public void setCallingABike(CallABikeStatus status) {
        this.isCallingABike = status;
        updateCallABikeButtonState();
    }

    private void updateCallABikeButtonState() {
        switch (isCallingABike) {
            case CALL_ABIKE:
                callABike.setText("Call ABike");
                break;
            case STOP_CALL_ABIKE:
                callABike.setText("Cancel Call");
                break;
            case STOP_RIDE_ABIKE:
                callABike.setText("Stop A-Ride");
                break;
        }
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
            System.out.println("Ride update: " + message.body());
            JsonObject update = (JsonObject) message.body();
            if (update.containsKey("rideStatus")) {
                String status = update.getString("rideStatus");
                if(status.equals("stopped")){
                    setRiding(false);
                }
                refreshView();
            }
            if(update.containsKey("autonomousRideStatus")){
                String status = update.getString("autonomousRideStatus");
                if(status.equals("stopped")){
                    setCallingABike(CallABikeStatus.CALL_ABIKE);
                }
                refreshView();
            }
        });
    }

    private void observeAvailableBikes() {
        vertx.eventBus().consumer("user.bike.update." + actualUser.username(), message -> {
            JsonArray update = (JsonArray) message.body();

            // Process updates without clearing existing bikes first
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

                    BikeType bikeType = BikeType.valueOf(typeStr.toUpperCase());

                    // Remove existing bike with same ID if present
                    removeExistingBike(id);

                    // Add updated or new bike
                    switch(bikeType) {
                        case NORMAL:
                            EBikeState state = EBikeState.valueOf(stateStr);
                            eBikes.add(new EBikeViewModel(id, x, y, batteryLevel, state, bikeType));
                            break;
                        case AUTONOMOUS:
                            ABikeState stateAbike = ABikeState.valueOf(stateStr);
                            aBikes.add(new ABikeViewModel(id, x, y, batteryLevel, stateAbike, bikeType));
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

    private void removeExistingBike(String bikeId) {
        eBikes.removeIf(bike -> bike.id().equals(bikeId));
        aBikes.removeIf(bike -> bike.id().equals(bikeId));
    }

    private void observeStations() {
        observeStationsToList(vertx);
        refreshView();
    }

    private void observeDispatches(){
        observeDispatchesToList(vertx);
        refreshView();
    }

    private void observeCallABikeStatus() {
        vertx.eventBus().consumer("user.bike.statusUpdate." + actualUser.username(), message -> {
            JsonObject update = (JsonObject) message.body();
            String status = update.getString("callABikeStatus");
            System.out.println("CallABikeStatus: " + status);
            if (status != null) {
                SwingUtilities.invokeLater(() -> setCallingABike(CallABikeStatus.valueOf(status)));
            }
        });
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