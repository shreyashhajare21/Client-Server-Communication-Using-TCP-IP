import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import javax.swing.*;

@SuppressWarnings("unused")
public class ServerGUI extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton, fileButton, voiceCallButton;
    private JLabel statusLabel;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader br;
    private PrintStream ps;
    private String connectionId, connectionPassword;
    private OutputStream os;
    private InputStream is;

    public ServerGUI() {
        initializeGUI();
        startServer();
    }

    private void initializeGUI() {
        setTitle("Server Control Panel");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(230, 230, 250));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        statusLabel = new JLabel("Server starting...");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setForeground(new Color(25, 25, 112));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        voiceCallButton = createButton("Voice Call", new Color(30, 144, 255));

        JPanel callPanel = new JPanel();
        callPanel.setBackground(new Color(230, 230, 250));
        callPanel.add(voiceCallButton);
        statusPanel.add(callPanel, BorderLayout.EAST);

        add(statusPanel, BorderLayout.NORTH);

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setForeground(new Color(0, 51, 102));
        chatArea.setBackground(Color.WHITE);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        add(chatScroll, BorderLayout.CENTER);

        // Input and controls
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.addActionListener(e -> sendMessage());

        sendButton = createButton("Send", new Color(34, 139, 34));
        sendButton.addActionListener(e -> sendMessage());

        fileButton = createButton("Send File", new Color(255, 165, 0));
        fileButton.addActionListener(e -> sendFile());

        JPanel controlPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(230, 230, 250));
        buttonPanel.add(sendButton);
        buttonPanel.add(fileButton);

        controlPanel.add(inputField, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.EAST);
        add(controlPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        return button;
    }

    private void startServer() {
        new Thread(() -> {
            try {
                generateCredentials();
                serverSocket = new ServerSocket(2100);
                updateStatus("Server running on port 2100", new Color(34, 139, 34));
                displayMessage("Server started successfully\nConnection ID: " + connectionId + "\nPassword: " + connectionPassword);

                while (true) {
                    displayMessage("Waiting for client connection...");
                    clientSocket = serverSocket.accept();
                    String clientIP = clientSocket.getInetAddress().getHostAddress();
                    displayMessage("Client connected from IP: " + clientIP);
                    updateStatus("Client connected from IP: " + clientIP, new Color(76, 175, 80));

                    br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    ps = new PrintStream(clientSocket.getOutputStream(), true);
                    os = clientSocket.getOutputStream();
                    is = clientSocket.getInputStream();

                    handleClientConnection(clientIP);
                }
            } catch (IOException e) {
                displayMessage("Error: " + e.getMessage());
                updateStatus("Server error", Color.RED);
            }
        }).start();
    }

    private void generateCredentials() {
        Random random = new Random();
        connectionId = "ID" + (random.nextInt(9000) + 1000);
        connectionPassword = "PASS" + (random.nextInt(9000) + 1000);
    }

    private void handleClientConnection(String clientIP) {
        try {
            String id = br.readLine();
            String password = br.readLine();

            if (validateCredentials(id, password)) {
                ps.println("SUCCESS");
                displayMessage("Client authenticated from IP: " + clientIP + "\nWaiting for messages...");

                String message;
                while ((message = br.readLine()) != null) {
                    if ("[FILE_TRANSFER]".equals(message)) {
                        receiveFile();
                    } else {
                        String finalMessage = message;
                        SwingUtilities.invokeLater(() -> chatArea.append("Client: " + finalMessage + "\n"));
                    }
                }
            } else {
                ps.println("INVALID_CREDENTIALS");
                clientSocket.close();
                updateStatus("Authentication failed for IP: " + clientIP, new Color(244, 67, 54));
                displayMessage("Invalid credentials from IP: " + clientIP + "\nConnection closed");
            }
        } catch (IOException e) {
            displayMessage("Client disconnected");
        }
    }

    private boolean validateCredentials(String id, String password) {
        return id.equals(connectionId) && password.equals(connectionPassword);
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            try {
                ps.println(message);
                displayMessage("Server: " + message);
                inputField.setText("");
            } catch (Exception e) {
                displayMessage("Failed to send message");
            }
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            displayMessage("Sending file: " + file.getName());
            try {
                ps.println("[FILE_TRANSFER]");
                ps.println(file.getName());
                ps.println(file.length());

                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
                fis.close();

                displayMessage("File sent successfully.");
            } catch (IOException ex) {
                displayMessage("File transfer failed: " + ex.getMessage());
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

                while (totalBytesRead < fileSize && (bytesRead = is.read(buffer)) != -1) {
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

    private void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }

    private void updateStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            statusLabel.setForeground(color);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }
}