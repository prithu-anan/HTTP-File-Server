import java.io.*;
import java.net.Socket;
import java.util.Date;

public class Worker extends Thread {
    Socket socket;
    String content;

    public Worker(Socket socket, String content) {
        this.socket = socket;
        this.content = content;
    }

    public void run() {
        // buffers
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter pr = new PrintWriter(socket.getOutputStream());
            String input = in.readLine();
            System.out.println("input : " + input);

            // String content = "<html>Hello</html>";
            if (input == null) return;
            if (!input.isEmpty()) {
                if (input.startsWith("GET")) {
                    pr.write("HTTP/1.1 200 OK\r\n");
                    pr.write("Server: Java HTTP Server: 1.0\r\n");
                    pr.write("Date: " + new Date() + "\r\n");
                    pr.write("Content-Type: text/html\r\n");
                    pr.write("Content-Length: " + content.length() + "\r\n");
                    pr.write("\r\n");
                    pr.write(content);
                    pr.flush();
                }

                else {

                }
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}