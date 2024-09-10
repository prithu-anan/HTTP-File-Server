import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class HTTPServer {
    static final int PORT = 5045;

    public static String readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            int read = fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        return Arrays.toString(fileData);
    }

    public static void main(String[] args) throws IOException {

        try (ServerSocket serverConnect = new ServerSocket(PORT)) {
            System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

            File file = new File("root/dir1/file2.txt");
            FileInputStream fis = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }

            String content = sb.toString();

            while (true) {
                Socket s = serverConnect.accept();

                Thread worker = new Worker(s, content);
                worker.start();


            }
        }

    }

}
