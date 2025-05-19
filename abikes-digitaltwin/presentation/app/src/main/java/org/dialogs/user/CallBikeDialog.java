        package org.dialogs.user;

        import io.vertx.core.Vertx;
        import io.vertx.core.json.JsonObject;
        import org.models.CallABikeStatus;
        import org.models.UserViewModel;
        import org.views.UserView;

        import javax.swing.*;
        import java.awt.*;
        import java.awt.event.ActionEvent;

        public class CallBikeDialog extends JDialog {
            private final JTextField bikeIdField = new JTextField(10);
            private final JTextField xField = new JTextField(5);
            private final JTextField yField = new JTextField(5);
            private boolean confirmed = false;
            private final Vertx vertx;
            private final UserViewModel user;
            private final JFrame parent;

            public CallBikeDialog(JFrame parent, Vertx vertx, UserViewModel user) {
                super(parent, "Call a Bike", true);
                this.vertx = vertx;
                this.user = user;
                this.parent = parent;
                setLayout(new GridLayout(4, 2, 5, 5));

                add(new JLabel("Bike ID:"));
                add(bikeIdField);

                add(new JLabel("Position X:"));
                add(xField);

                add(new JLabel("Position Y:"));
                add(yField);

                JButton okButton = new JButton("Call");
                JButton cancelButton = new JButton("Cancel");

                okButton.addActionListener(this::onOk);
                cancelButton.addActionListener(e -> dispose());

                add(okButton);
                add(cancelButton);

                pack();
                setLocationRelativeTo(parent);
            }

            private void onOk(ActionEvent e) {
                confirmed = true;
                String bikeId = bikeIdField.getText();
                double posX = Double.parseDouble(xField.getText());
                double posY = Double.parseDouble(yField.getText());

                JsonObject callDetails = new JsonObject()
                        .put("user", user.username())
                        .put("bike", bikeId)
                        .put("posX", posX)
                        .put("posY", posY);

                vertx.eventBus().request("user.ride.callBike." + user.username(), callDetails, ar -> {
                    SwingUtilities.invokeLater(() -> {
                        if (ar.succeeded()) {
                            ((UserView) parent).setCallingABike(CallABikeStatus.STOP_CALL_ABIKE);
                            JOptionPane.showMessageDialog(this, "Bike called successfully");
                            dispose();
                        } else {
                            ((UserView) parent).setCallingABike(CallABikeStatus.CALL_ABIKE);
                            JOptionPane.showMessageDialog(this, "Error calling bike: " + ar.cause().getMessage());
                        }
                    });
                });
            }

            public boolean isConfirmed() {
                return confirmed;
            }

            public String getBikeId() {
                return bikeIdField.getText();
            }

            public double getPosX() {
                return Double.parseDouble(xField.getText());
            }

            public double getPosY() {
                return Double.parseDouble(yField.getText());
            }
        }