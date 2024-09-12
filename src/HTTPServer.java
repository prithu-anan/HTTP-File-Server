import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class HTTPServer {
    static final int PORT = 5045;

    public static void main(String[] args) {
        File file = new File("log.txt");

        try (ServerSocket serverConnect = new ServerSocket(PORT);
             FileOutputStream fos = new FileOutputStream(file);
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos))) {

            System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

            while (true) {
                Socket socket = serverConnect.accept();
                Thread worker = new Worker(socket, bw);
                worker.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
