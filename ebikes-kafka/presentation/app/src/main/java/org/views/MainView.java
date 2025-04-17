package org.views;

import io.vertx.core.Vertx;
import org.dialogs.access.LoginDialog;
import org.dialogs.access.RegisterDialog;
import org.verticles.UserManagementVerticle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainView extends JFrame implements ActionListener {

    private JButton loginButton, registerButton;
    private final Vertx vertx;
    private final UserManagementVerticle userManagementVerticle;

    public MainView(Vertx vertx) {
        this.vertx = vertx;
        this.userManagementVerticle = new UserManagementVerticle(vertx);
        this.userManagementVerticle.init();
        setupView();
    }

    protected void setupView() {
        setTitle("Welcome");
        setSize(300, 100);
        setResizable(false);
        setLayout(new BorderLayout());

        loginButton = new JButton("Login");
        loginButton.addActionListener(this);

        registerButton = new JButton("Register");
        registerButton.addActionListener(this);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        add(buttonPanel, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void display() {
        SwingUtilities.invokeLater(() -> this.setVisible(true));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loginButton) {
            new Thread(() -> {
                LoginDialog loginDialog = new LoginDialog(this, this.vertx);
                loginDialog.setVisible(true);
            }).start();
        } else if (e.getSource() == registerButton) {
            new Thread(() -> {
                RegisterDialog registerDialog = new RegisterDialog(this, this.vertx);
                registerDialog.setVisible(true);
            }).start();
        }
    }
}