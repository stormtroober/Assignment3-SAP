package org.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class AbstractDialog extends JDialog implements ActionListener {

    protected JPanel panel;
    protected JPanel buttonPanel;
    protected JButton confirmButton;
    protected JButton cancelButton;

    public AbstractDialog(JFrame parent, String title) {
        super(parent, title, true);
        setupDialog();
    }

    private void setupDialog() {
        setLayout(new BorderLayout());

        panel = new JPanel(new GridLayout(3, 2));
        add(panel, BorderLayout.CENTER);

        buttonPanel = new JPanel();
        confirmButton = new JButton("Confirm");
        confirmButton.addActionListener(this);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);


        setLocationRelativeTo(getParent());
        pack();
    }

    protected void addField(String label, JComponent field) {
        panel.add(new JLabel(label));
        panel.add(field);
        pack();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == cancelButton) {
            dispose();
        }
    }
}