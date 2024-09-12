import java.io.*;
import java.net.*;

public class Client {
    private static final String HOST = "localhost";
    private static final int PORT = 5045;  // Same port as the server

    public static void main(String[] args) {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter file names to upload (comma separated):");

        try {
            String input = consoleReader.readLine();
            String[] fileNames = input.split(",");

            for (String fileName : fileNames)
                new Thread(new FileUploader(fileName.trim())).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class FileUploader implements Runnable {
        private final String fileName;

        public FileUploader(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void run() {
            try (Socket socket = new Socket(HOST, PORT)) {
                File file = new File(fileName);
                if (!file.exists() || !file.isFile()) {
                    System.out.println("Invalid file: " + fileName);
                    return;
                }

                PrintWriter pr = new PrintWriter(socket.getOutputStream());
                pr.println("UPLOAD " + fileName);
                pr.flush();

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
}
