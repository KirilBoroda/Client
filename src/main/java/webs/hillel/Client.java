package webs.hillel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;

    private String clientName;
    public Socket socket;
    private InputStream input;
    private OutputStream output;
    private boolean running;
    private Logger logger;

    public Client() {
        running = true;
        logger = Logger.getLogger(Client.class.getName());
    }

    public void connectToServer() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            input = socket.getInputStream();
            output = socket.getOutputStream();

            logger.info("Connected to the server.");

            Thread thread = new Thread(new ServerMessageListener());
            thread.start();

            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter your name: ");
            clientName = scanner.nextLine().trim();

            sendMessage(clientName);

            while (running) {
                String message = scanner.nextLine();

                if (message.equals("-exit")) {
                    sendMessage("-exit");
                    running = false;
                } else if (message.startsWith("-file")) {
                    String filePath = message.substring(6).trim();
                    sendFile(filePath);
                } else {
                    sendMessage(message);
                }
            }

            socket.close();
            input.close();
            output.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception in client: ", e);
        }
    }

    public void sendMessage(String message) throws IOException {
        output.write(message.getBytes());
        output.flush();
    }

    public void sendFile(String filePath) {
        try {
            String fileName = Paths.get(filePath).getFileName().toString();

            fileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "");

            String cFilePath = "C:\\" + fileName;

            Path file = Paths.get(filePath);
            byte[] fileContent = Files.readAllBytes(file);

            String message = "-file " + fileName;

            sendMessage(message);

            output.write(fileContent);
            output.flush();

            logger.info("File transfer complete: " + cFilePath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception during file transfer: ", e);
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.connectToServer();
    }

    class ServerMessageListener implements Runnable {
        @Override
        public void run() {
            try {
                byte[] buffer = new byte[1024];

                while (running) {
                    int bytesRead = input.read(buffer);

                    if (bytesRead == -1) {
                        break;
                    }

                    String message = new String(buffer, 0, bytesRead);
                    logger.info(message);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Exception in server message listener: ", e);
            }
        }
    }
}
