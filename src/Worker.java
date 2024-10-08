import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

public class Worker extends Thread {
    Socket socket;
    final BufferedWriter bw;
    static final String ROOT_DIR = "root/";
    static final String UPLOAD_DIR = "upload/";
    static final int CHUNK_SIZE = 4096;

    public Worker(Socket socket, BufferedWriter bw) {
        this.socket = socket;
        this.bw = bw;
    }

    @Override
    public void run() {
        try {
            handleRequest(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRequest(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        OutputStream out = socket.getOutputStream();
        PrintWriter pr = new PrintWriter(out);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = dateFormat.format(new Date());

        String requestLine = in.readLine();

        if (requestLine != null) {
            synchronized (bw) {
                bw.write(requestLine);
                bw.write("\r\n");
                bw.write("\r\n");
                bw.flush();
            }
        }
        else {
            sendErrorResponse(pr, "400 Bad Request", date);
            return;
        }

        String[] requestParts = requestLine.split(" ");
        String filePath = requestParts[1];

        if (requestLine.startsWith("GET")) {
            File file = new File(ROOT_DIR + filePath);

            if (file.isDirectory())
                sendDirectoryListing(pr, file, date, dateFormat);
            else if (file.exists())
                sendFileResponse(out, file, date, dateFormat);
            else
                sendErrorResponse(pr, "404 Not Found", date);
        }

        else if (requestLine.startsWith("UPLOAD")) {
            File uploadDir = new File(UPLOAD_DIR);

            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
            }

            File file = new File(UPLOAD_DIR + filePath);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesRead;
                int count = 0;

                InputStream inputStream = socket.getInputStream();

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    count += bytesRead;
                }

                fos.flush();

                System.out.println("Received " + count + " bytes of file: " + file.getName());

            } catch (IOException e) {
                e.printStackTrace();
                sendErrorResponse(pr, "500 Internal Server Error", date);
            }
        }

        else
            sendErrorResponse(pr, "501 Not Implemented", date);

        socket.close();

        String clientHost = socket.getInetAddress().getHostAddress();
        int clientPort = socket.getPort();
        System.out.println("Client closed from host: " + clientHost + " and port: " + clientPort);
    }


    private void sendFileResponse(OutputStream out, File file, String date, SimpleDateFormat dateFormat) throws IOException {
        String contentType = Files.probeContentType(file.toPath());

        if (contentType == null)
            contentType = "application/octet-stream";

        String lastModified = dateFormat.format(new Date(file.lastModified()));
        long contentLength = file.length();
        String eTag = "\"" + Integer.toHexString(file.hashCode()) + "-" + Long.toHexString(contentLength) + "\"";

        StringBuilder response = getStringBuilder(date, lastModified, contentLength, eTag, contentType);

        synchronized (bw) {
            bw.write(response.toString());
            bw.write("\r\n");
            bw.flush();
        }

        PrintWriter pr = new PrintWriter(out);
        pr.write(response.toString());
        pr.flush();

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1)
                out.write(buffer, 0, bytesRead);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendDirectoryListing(PrintWriter pr, File dir, String date, SimpleDateFormat dateFormat) throws IOException {
        StringBuilder body = new StringBuilder();

        body.append("<html>\r\n");
        body.append("\t<head>\r\n");
        body.append("\t\t<title>").append(dir.getName()).append("</title>\r\n");
        body.append("\t</head>\r\n");
        body.append("\t<body>\r\n");
        body.append("\t\t<ul>\r\n");

        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory())
                body.append("\t\t\t<li><b><i><a href=\"").append(file.getName()).append("/\">").append(file.getName()).append("</a></i></b></li>\r\n");
            else
                body.append("\t\t\t<li><a href=\"").append(file.getName()).append("\">").append(file.getName()).append("</a></li>\r\n");
        }

        body.append("\t\t</ul>\r\n");
        body.append("\t</body>\r\n");
        body.append("</html>\r\n");
        body.append("\r\n");

        String lastModified = dateFormat.format(new Date(dir.lastModified()));
        long contentLength = body.toString().length();
        String eTag = "\"" + Integer.toHexString(dir.hashCode()) + "-" + Long.toHexString(contentLength) + "\"";

        StringBuilder response = getStringBuilder(date, lastModified, contentLength, eTag, "text/html");
        response.append(body);

        synchronized (bw) {
            bw.write(response.toString());
            bw.write("\r\n");
            bw.flush();
        }

        pr.write(response.toString());
        pr.flush();
    }

    private static StringBuilder getStringBuilder(String date, String lastModified, long contentLength, String eTag, String contentType) {
        return new StringBuilder(
                            "HTTP/1.0 200 OK\r\n" +
                            "MIME-Version: 1.0\r\n" +
                            "Date: " + date + "\r\n" +
                            "Server: FileServer/1.0\r\n" +
                            "Last-Modified: " + lastModified + "\r\n" +
                            "ETag: " + eTag + "\r\n" +
                            "Accept-Ranges: bytes\r\n" +
                            "Content-Length: " + contentLength + "\r\n" +
                            "Content-Type: " + contentType + "\r\n" +
                            "\r\n"
        );
    }

    private void sendErrorResponse(PrintWriter pr, String status, String date) throws IOException {
        String response =   "HTTP/1.0 " + status + "\r\n" +
                            "MIME-Version: 1.0\r\n" +
                            "Date: " + date + "\r\n" +
                            "Server: FileServer/1.0\r\n" +
                            "Accept-Ranges: bytes\r\n" +
                            "Content-Type: text/html\r\n" +
                            "\r\n" +
                            "<html>\r\n" +
                            "\t<head>\r\n" +
                            "\t\t<title>" + status + "</title>\r\n" +
                            "\t</head>\r\n" +
                            "\t<body>\r\n" +
                            "\t\t<h1>" + status + "</h1>\r\n" +
                            "\t</body>\r\n" +
                            "</html>\r\n";

        synchronized (bw) {
            bw.write(response);
            bw.write("\r\n");
            bw.flush();
        }

        pr.write(response);
        pr.flush();
    }
}