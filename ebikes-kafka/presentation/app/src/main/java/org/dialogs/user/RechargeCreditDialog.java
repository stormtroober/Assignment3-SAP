package org.dialogs.user;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.dialogs.AbstractDialog;
import org.models.UserViewModel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class RechargeCreditDialog extends AbstractDialog {

    private JTextField creditAmountField;
    private final Vertx vertx;
    private final UserViewModel user;

    public RechargeCreditDialog(JFrame parent, Vertx vertx, UserViewModel user) {
        super(parent, "Recharge Credit");
        this.vertx = vertx;
        this.user = user;
        setupDialog();
    }

    private void setupDialog() {
        creditAmountField = new JTextField();
        addField("Credit Amount:", creditAmountField);
        confirmButton.setText("Recharge");
        cancelButton.setText("Cancel");
        pack();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (e.getSource() == confirmButton) {
            String creditToAdd = creditAmountField.getText();

            JsonObject creditDetails = new JsonObject()
                    .put("username", user.username())
                    .put("creditToAdd", Integer.parseInt(creditToAdd));

            vertx.eventBus().request("user.update.recharge" + user.username(), creditDetails, reply -> {
                if (reply.succeeded()) {
                    JOptionPane.showMessageDialog(this, "Credit recharged successfully");
                    vertx.eventBus().publish("user.update.recharge", reply.result().body());
                } else {
                    JOptionPane.showMessageDialog(this, "Error recharging Credit: " + reply.cause().getMessage());
                    System.out.println("Error recharging Credit: " + reply.cause().getMessage());
                }
                dispose();
            });
        }
    }
}