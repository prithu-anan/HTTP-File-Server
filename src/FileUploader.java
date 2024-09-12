import java.io.*;
import java.net.Socket;

public class FileUploader extends Thread {
    private final String fileName;
    private final String host;
    private final int port;

    public FileUploader(String fileName, String host, int port) {
        this.fileName = fileName;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        File file = new File(fileName);

        if (!file.exists() || !file.isFile()) {
            System.out.println("Invalid file: " + fileName);
            return;
        }

        try (Socket socket = new Socket(host, port)) {
            // Send the upload request to the server
            try (PrintWriter pr = new PrintWriter(socket.getOutputStream())) {
                pr.println("UPLOAD " + fileName);
                pr.flush();
            }

            // Upload the file data
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                OutputStream out = socket.getOutputStream();
                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            }

            System.out.println("Uploaded file: " + fileName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
