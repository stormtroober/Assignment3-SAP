package org.dialogs.admin;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.dialogs.AbstractDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class AddABikeDialog extends AbstractDialog {

private JTextField idField;
private final Vertx vertx;

public AddABikeDialog(JFrame parent, Vertx vertx) {
    super(parent, "Adding Bike");
    this.vertx = vertx;
    setupDialog();
}

private void setupDialog() {
    idField = new JTextField();

    addField("A-Bike ID:", idField);
}

@Override
public void actionPerformed(ActionEvent e) {
    super.actionPerformed(e);
    if (e.getSource() == confirmButton) {
        String id = idField.getText();

        JsonObject bikeDetails = new JsonObject()
                .put("id", id);

        vertx.eventBus().request("admin.abike.create", bikeDetails, reply -> {
            if (reply.succeeded()) {
                JOptionPane.showMessageDialog(this, "Bike added successfully");
            } else {
                JOptionPane.showMessageDialog(this, "Error adding Bike: " + reply.cause().getMessage());
                System.out.println("Error adding Bike: " + reply.cause().getMessage());
            }
            dispose();
        });
    }
}
}