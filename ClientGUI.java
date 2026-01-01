import java.awt.*;
import java.io.*;
import java.net.Socket;
import javax.sound.sampled.*;
import javax.swing.*;

public class ClientGUI extends JFrame {
    private JTextField serverIdField;
    private JPasswordField passwordField;
    private JTextField inputField;
    private JTextArea chatArea;
    private JButton sendButton, attachButton, voiceCallButton;
    private JCheckBox showPasswordCheckbox;
    private Socket socket;
    private BufferedReader br;
    private PrintStream ps;
    private boolean isConnected;
    private JLabel statusLabel;
    private InputStream is;

    public ClientGUI() {
        createLoginUI();
    }

    @SuppressWarnings("unused")
    private void createLoginUI() {
        setTitle("Chat Application - Login");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(new Color(240, 248, 255));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Add Server ID field
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel serverLabel = createLabel("Server ID:");
        loginPanel.add(serverLabel, gbc);

        gbc.gridx = 1;
        serverIdField = new JTextField(15);
        loginPanel.add(serverIdField, gbc);

        // Add Password field
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel passwordLabel = createLabel("Password:");
        loginPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        loginPanel.add(passwordField, gbc);

        // Add show password checkbox
        gbc.gridy = 2;
        showPasswordCheckbox = new JCheckBox("Show Password");
        showPasswordCheckbox.setBackground(new Color(240, 248, 255));
        showPasswordCheckbox.addActionListener(e -> passwordField.setEchoChar(
                showPasswordCheckbox.isSelected() ? '\0' : '●'));
        loginPanel.add(showPasswordCheckbox, gbc);

        // Add Login button
        gbc.gridy = 3;
        JButton loginButton = createButton("Login", new Color(70, 130, 180));
        loginButton.addActionListener(e -> attemptLogin());
        loginPanel.add(loginButton, gbc);

        add(loginPanel, BorderLayout.CENTER);
        setVisible(true);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(25, 25, 112));
        return label;
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        return button;
    }

    private void attemptLogin() {
        String serverId = serverIdField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if (serverId.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill out all fields.");
            return;
        }
        try {
            socket = new Socket("localhost", 2100);
            ps = new PrintStream(socket.getOutputStream());
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            is = socket.getInputStream();

            ps.println(serverId);
            ps.println(password);
            String response = br.readLine();
            if ("SUCCESS".equals(response)) {
                isConnected = true;
                JOptionPane.showMessageDialog(this, "Server connected successfully!");
                createMainUI();
            } else {
                JOptionPane.showMessageDialog(this, "Login failed.");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Unable to connect to server.");
        }
    }

    @SuppressWarnings("unused")
    private void createMainUI() {
        setTitle("Chat Application");
        setSize(800, 600);
        getContentPane().removeAll();
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setBackground(new Color(255, 255, 255));
        chatArea.setForeground(new Color(0, 51, 102));
        JScrollPane chatScroll = new JScrollPane(chatArea);

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.addActionListener(e -> sendMessage());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout());

        sendButton = createButton("Send", new Color(34, 139, 34));
        sendButton.addActionListener(e -> sendMessage());

        attachButton = createButton("Attach", new Color(255, 165, 0));
        attachButton.addActionListener(e -> sendFile());

        voiceCallButton = createButton("Voice Call", new Color(30, 144, 255));
        voiceCallButton.addActionListener(e -> initiateVoiceCall());

        buttonPanel.add(sendButton);
        buttonPanel.add(attachButton);
        buttonPanel.add(voiceCallButton);

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(chatScroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        new Thread(this::receiveMessages).start();
        setVisible(true);
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty() && isConnected) {
            try {
                ps.println(msg);
                chatArea.append("You: " + msg + "\n");
                inputField.setText("");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Message sending failed.");
            }
        }
    }

    private void receiveMessages() {
    try {
        String line;
        while (isConnected && (line = br.readLine()) != null) {
            if ("[FILE_TRANSFER]".equals(line)) {
                receiveFile();  // ✅ Call file receiver
            } else {
                String msg = line;
                SwingUtilities.invokeLater(() -> chatArea.append("Server: " + msg + "\n"));
            }
        }
    } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "Connection lost.");
    }
}


    @SuppressWarnings("UseSpecificCatch")
    private void initiateVoiceCall() {
        chatArea.append("Starting voice call...\n");
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
                TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
                SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));

                microphone.open(format);
                speakers.open(format);
                microphone.start();
                speakers.start();

                byte[] buffer = new byte[4096];
                while (true) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    speakers.write(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                chatArea.append("Voice call failed: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            chatArea.append("Sending file: " + file.getName() + "\n");
            try {
                ps.println("[FILE_TRANSFER]");
                ps.println(file.getName());
                ps.println(file.length());

                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    socket.getOutputStream().write(buffer, 0, bytesRead);
                }
                socket.getOutputStream().flush();
                fis.close();

                chatArea.append("File sent successfully.\n");
            } catch (IOException ex) {
                chatArea.append("File transfer failed: " + ex.getMessage() + "\n");
            }
        }
    }

    private void receiveFile() {
    try {
        String fileName = br.readLine();
        long fileSize = Long.parseLong(br.readLine());

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fileName));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            chatArea.append("Receiving file: " + file.getName() + "\n");

            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;

            while (totalBytesRead < fileSize &&
                   (bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
            fos.close();

            chatArea.append("File received successfully.\n");
        }
    } catch (IOException ex) {
        chatArea.append("File reception failed: " + ex.getMessage() + "\n");
    }
}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}