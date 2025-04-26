package org.dialogs.access;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.dialogs.AbstractDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class RegisterDialog extends AbstractDialog {

    private JTextField userNameField;
    private JComboBox<String> userTypeComboBox;
    private final Vertx vertx;

    public RegisterDialog(JFrame parent, Vertx vertx) {
        super(parent, "Register");
        this.vertx = vertx;
        setupDialog();
    }

    private void setupDialog() {
        userNameField = new JTextField();
        userTypeComboBox = new JComboBox<>(new String[]{"USER", "ADMIN"});
        addField("Username:", userNameField);
        addField("Type:", userTypeComboBox);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (e.getSource() == confirmButton) {
            String username = userNameField.getText();
            String type = (String) userTypeComboBox.getSelectedItem();
            if (!username.isEmpty() && type != null) {
                JsonObject userDetails = new JsonObject()
                        .put("username", username)
                        .put("type", type);

                vertx.eventBus().request("user.register", userDetails, reply -> {
                    if (reply.succeeded()) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "Registration successful");
                            dispose();
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "Registration failed: " + reply.cause().getMessage());
                        });
                    }
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Please enter a username and select a type");
                });
            }
        }
    }
}