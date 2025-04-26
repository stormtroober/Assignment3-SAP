package org.dialogs.user;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.dialogs.AbstractDialog;
import org.models.UserViewModel;
import org.views.UserView;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class StartRideDialog extends AbstractDialog {

    private JTextField idEBikeField;
    private final Vertx vertx;
    private final UserViewModel user;
    private final JFrame parent;

    public StartRideDialog(JFrame parent, Vertx vertx, UserViewModel user) {
        super(parent, "Start Riding an E-Bike");
        this.vertx = vertx;
        this.user = user;
        this.parent = parent;
        setupDialog();
    }

    private void setupDialog() {
        idEBikeField = new JTextField();
        addField("E-Bike ID:", idEBikeField);
        confirmButton.setText("Start Ride");
        cancelButton.setText("Cancel");
        pack();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (e.getSource() == confirmButton) {
            if (user.credit() == 0) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "You need to recharge your credit before starting a ride");
                    ((UserView) parent).setRiding(false);
                    dispose();
                });
            } else {
                String bikeId = idEBikeField.getText();
                JsonObject rideDetails = new JsonObject()
                        .put("user", user.username())
                        .put("bike", bikeId);
                vertx.eventBus().request("user.ride.start." + user.username(), rideDetails, ar -> {
                    SwingUtilities.invokeLater(() -> {
                        if (ar.succeeded()) {
                            ((UserView) parent).setRiding(true);
                            JOptionPane.showMessageDialog(this, "Ride started");
                            dispose();
                        } else {
                            ((UserView) parent).setRiding(false);
                            JOptionPane.showMessageDialog(this, "Error starting ride: " + ar.cause().getMessage());
                        }
                    });
                });
            }
        }
    }
}