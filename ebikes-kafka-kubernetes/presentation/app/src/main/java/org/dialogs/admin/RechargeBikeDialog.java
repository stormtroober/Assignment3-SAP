package org.dialogs.admin;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.dialogs.AbstractDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class RechargeBikeDialog extends AbstractDialog {

    private JTextField idField;
    private final Vertx vertx;

    public RechargeBikeDialog(JFrame parent, Vertx vertx) {
        super(parent, "Recharge Bike");
        this.vertx = vertx;
        setupDialog();
    }

    private void setupDialog() {
        idField = new JTextField();
        addField("E-Bike ID to recharge:", idField);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (e.getSource() == confirmButton) {
            String id = idField.getText();

            JsonObject rechargeDetails = new JsonObject()
                    .put("bikeId", id);

            vertx.eventBus().request("admin.bike.recharge", rechargeDetails, reply -> {
                if (reply.succeeded()) {
                    JOptionPane.showMessageDialog(this, "E-Bike recharged successfully");
                } else {
                    JOptionPane.showMessageDialog(this, "Error recharging E-Bike: " + reply.cause().getMessage());
                }
                dispose();
            });
        }
    }
}