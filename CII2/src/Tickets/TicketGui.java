package Tickets;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TicketGui extends JFrame {
    private Tickets myAgent;
    JTextField count;

    TicketGui(Tickets q) {
        super(q.getLocalName());

        JPanel p = new JPanel();
        JLabel txt = new JLabel("Кол-во билетов:");
        count = new JTextField(12);
        p.setLayout(new GridLayout(1, 2));
        p.add(txt);
        p.add(count);
        getContentPane().add(p, BorderLayout.CENTER);
        myAgent = q;
        JButton addButton = new JButton("Ok");
        addButton.addActionListener(this::onClick);
        p = new JPanel();
        p.add(addButton);
        getContentPane().add(p, BorderLayout.SOUTH);

        setResizable(true);
    }

    void onClick(ActionEvent e) {
        myAgent.getTickets(Integer.parseInt(count.getText()));
        super.setVisible(false);
    }

    public void showGui() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int) screenSize.getWidth() / 2;
        int centerY = (int) screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.setVisible(true);
    }
}
