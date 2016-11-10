package dev2dev.textclient;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class TextClient
        extends JFrame
        implements MessageProcessor {
    private SipLayer sipLayer;

    private JTextField fromAddress;
    private JLabel receivedLbl;
    private JTextArea receivedMessages;
    private JLabel myServersLbl;
    private JLabel myClientsLbl;
    private JScrollPane receivedScrollPane;
    private JTextArea myServers;
    private JScrollPane myServersScrollPane;
    private JTextArea myClients;
    private JScrollPane myClientsScrollPane;
    private JButton sendBtn;
    private JLabel sendLbl;
    private JTextField sendMessages;
    private JTextField toAddress;
    private JLabel toLbl;
    private JButton deRegisterBtn;

    @SuppressWarnings("deprecation")
    public static void main(String[] args) {
        if (args.length != 2) {
            printUsage();
            System.exit(-1);
        }

        try {
            String username = args[0];
            int port = Integer.parseInt(args[1]);
            String ip = InetAddress.getLocalHost().getHostAddress();

            SipLayer sipLayer = new SipLayer(username, ip, port);
            TextClient tc = new TextClient(sipLayer);
            sipLayer.setMessageProcessor(tc);
            tc.setVisible(true);
//            tc.show();
        } catch (Throwable e) {
            System.out.println("Problem initializing the SIP stack.");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void printUsage() {
        System.out.println("Syntax:");
        System.out.println("  java -jar textclient.jar <username> <port>");
        System.out.println("where <username> is the nickname of this user");
        System.out.println("and <port> is the port number to use. Usually 5060 if not used by another process.");
        System.out.println("Example:");
        System.out.println("  java -jar textclient.jar snoopy71 5061");
    }

    public TextClient(SipLayer sip) {
        super();
        sipLayer = sip;
        initWindow();
        String from = "sip:" + sip.getUsername() + "@" + sip.getHost() + ":" + sip.getPort();
        this.fromAddress.setText(from);
    }

    private void initWindow() {
        receivedLbl = new JLabel();
        sendLbl = new JLabel();
        sendMessages = new JTextField();
        receivedScrollPane = new JScrollPane();
        receivedMessages = new JTextArea();
        JLabel fromLbl = new JLabel();
        fromAddress = new JTextField();
        toLbl = new JLabel();
        toAddress = new JTextField();
        sendBtn = new JButton();
        deRegisterBtn = new JButton();
        myServersScrollPane = new JScrollPane();
        myServers = new JTextArea();
        myClientsScrollPane = new JScrollPane();
        myClients = new JTextArea();
        myClientsLbl = new JLabel();
        myServersLbl = new JLabel();

        getContentPane().setLayout(null);

        setTitle(sipLayer.getUsername());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                System.exit(0);
            }
        });

        receivedLbl.setText("Server Log:");
        receivedLbl.setAlignmentY(0.0F);
        receivedLbl.setPreferredSize(new java.awt.Dimension(25, 100));
        getContentPane().add(receivedLbl);
        receivedLbl.setBounds(5, 0, 150, 20);

        sendLbl.setText("Send Message:");
        getContentPane().add(sendLbl);
        sendLbl.setBounds(5, 150, 120, 20);

        getContentPane().add(sendMessages);
        sendMessages.setBounds(5, 170, 270, 20);

        receivedMessages.setAlignmentX(0.0F);
        receivedMessages.setEditable(false);
        receivedMessages.setLineWrap(true);
        receivedMessages.setWrapStyleWord(true);
        receivedScrollPane.setViewportView(receivedMessages);
        receivedScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        getContentPane().add(receivedScrollPane);
        receivedScrollPane.setBounds(5, 20, 290, 170);

        fromLbl.setText("I am:");
        getContentPane().add(fromLbl);
        fromLbl.setBounds(5, 200, 55, 15);

        getContentPane().add(fromAddress);
        fromAddress.setBounds(60, 200, 235, 20);
        fromAddress.setEditable(false);

        toLbl.setText("To:");
        getContentPane().add(toLbl);
        toLbl.setBounds(5, 225, 55, 15);

        getContentPane().add(toAddress);
        toAddress.setBounds(60, 225, 235, 21);

        sendBtn.addActionListener(evt -> {
            registerBtnActionPerformed();

        });

        getContentPane().add(sendBtn);
        sendBtn.setBounds(170, 255, 100, 25);
        sendBtn.setBackground(Color.green);
        deRegisterBtn.addActionListener(evt -> {
            deRegisterBtnActionPerformed();
        });
        deRegisterBtn.setText("DeReg");
        getContentPane().add(deRegisterBtn);
        deRegisterBtn.setBounds(50, 255, 100, 25);
        deRegisterBtn.setBackground(Color.RED);

        myServersLbl.setText("Servers:");
        getContentPane().add(myServersLbl);
        myServersLbl.setBounds(5, 290, 70, 15);
        myServers.setAlignmentX(0.0F);
        myServers.setEditable(false);
        myServers.setLineWrap(true);
        myServers.setWrapStyleWord(true);
        myServersScrollPane.setViewportView(myServers);
        myServersScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        getContentPane().add(myServersScrollPane);
        myServersScrollPane.setBounds(5, 305, 140, 100);

        myClientsLbl.setText("Clients:");
        getContentPane().add(myClientsLbl);
        myClientsLbl.setBounds(150, 290, 55, 15);
        myClients.setAlignmentX(0.0F);
        myClients.setEditable(false);
        myClients.setLineWrap(true);
        myClients.setWrapStyleWord(true);
        myClientsScrollPane.setViewportView(myClients);
        myClientsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        getContentPane().add(myClientsScrollPane);
        myClientsScrollPane.setBounds(150, 305, 140, 100);
        
        
        sendBtn.setText("Register");
        toLbl.setText("Server:");
        sendMessages.setVisible(false);
        sendLbl.setVisible(false);
        toAddress.setText("IP:PORT");

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width - 300) / 2, (screenSize.height - 450) / 2, 300, 450);

    }

    private void deRegisterBtnActionPerformed() {

        try {
            String serverAddress = this.toAddress.getText();
            sipLayer.CallDeRegisterRequest(serverAddress);
        } catch (Exception e) {
            e.printStackTrace();
            this.receivedMessages.append("ERROR register" + e.getMessage() + "\n");
        }
    }

    private void registerBtnActionPerformed() {

        try {
            String serverAddress = this.toAddress.getText();
            sipLayer.CallregisterRequest(serverAddress);
        } catch (Exception e) {
            e.printStackTrace();
            this.receivedMessages.append("ERROR register" + e.getMessage() + "\n");
        }

    }

    @Override
    public void processMessage(String sender, String message) {
        this.receivedMessages.append("From " +
                sender + ": " + message + "\n");
    }

    @Override
    public void processError(String errorMessage) {
        this.receivedMessages.append("ERROR: " +
                errorMessage + "\n");
    }

    @Override
    public void processInfo(String infoMessage) {
        this.receivedMessages.append(
                infoMessage + "\n");
    }

    @Override
    public void processClientReg(String client){
        this.myClients.append(
                client + "\n"
        );
    }

    @Override
    public void processClientDeReg(Set<String> clients){
        this.myClients.setText("");
        clients.forEach(this::processClientReg);
    }

    @Override
    public void processServerReg(String server) {
        this.myServers.append(
                server + "\n"
        );
    }

    @Override
    public void processServerDeReg(Set<String> servers) {
        this.myServers.setText("");
        servers.forEach(this::processServerReg);
    }
}
