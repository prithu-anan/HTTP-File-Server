import java.io.*;
import java.net.Socket;

import static java.lang.Thread.sleep;

public class Client {
    private static final String HOST = "localhost";
    private static final int PORT = 5045;
    private static final int CHUNK_SIZE = 4096;

    public static void main(String[] args) {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter space-separated file names to upload:");

        try {
            String input = consoleReader.readLine();
            String [] fileNames = input.split(" ");

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
                pr.println("UPLOAD " + file.getName());
                pr.println();
                pr.flush();

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[CHUNK_SIZE];
                    int bytesRead;
                    int count = 0;
                    OutputStream out = socket.getOutputStream();

                    sleep(1000);

                    while ((bytesRead = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        count += bytesRead;
                    }

                    out.flush();
                    System.out.println("Uploaded " + count + " bytes of file: " + fileName);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
