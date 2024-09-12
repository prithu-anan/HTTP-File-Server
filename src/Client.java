import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client {
    private static final String HOST = "localhost";
    private static final int PORT = 5045;

    public static void main(String[] args) {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter file names to upload:");

        while (true) {
            try {
                String fileName = consoleReader.readLine();
                if (fileName == null || fileName.trim().isEmpty()) {
                    System.out.println("Please enter a valid file name.");
                    continue;
                }

                Thread fileUploader = new FileUploader(fileName.trim(), HOST, PORT);
                fileUploader.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
