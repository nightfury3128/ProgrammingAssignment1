package webserver;

/**
* Assignment 1
* Nipun Chandra 
**/
/*NOTE: THE TXT FILE MUST BE PRESENT IN SRV DIRECTORY IN THE FTP SERVER AND NOT THE ROOT.. I RAN INTO ISSUE WITH PERMISSIONS FOR THAT 
   IF A DEMOSTRATION IS NEEDED LET ME KNOW 
 */
import java.io.*;
import java.net.*;
import java.util.*;

public final class WebServer {
    public static void main(String argv[]) throws Exception {
        int port = 6789;
        ServerSocket x = new ServerSocket(port); // I am geting a resource leak here for this socket "x" but professor
                                                 // said not to worry about it.

        while (true) {
            // Listen for a TCP connection request.
            Socket connection = x.accept();
            // Create a new thread to handle the client request.
            HttpRequest request = new HttpRequest(connection);
            Thread thread = new Thread(request);
            // Start the thread.
            thread.start();
        }
    }

    final static class HttpRequest implements Runnable { // https://stackoverflow.com/questions/9560600/what-causes-error-no-enclosing-instance-of-type-foo-is-accessible-and-how-do-i
        final static String CRLF = "\r\n";
        Socket socket;

        public HttpRequest(Socket socket) throws Exception {
            this.socket = socket;
        }

        // Implement the run() method of the Runnable interface.
        public void run() {
            try {
                processRequest();
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        private void processRequest() throws Exception {
            InputStream is = socket.getInputStream();
            DataOutputStream os = new DataOutputStream(socket.getOutputStream());

            // Set up input stream filters.
            InputStreamReader isf = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Get and display the header lines.
            String requestLine = br.readLine();
            System.out.println("Request line: " + requestLine);

            String headerLine = null;

            // WEBSERVER PART B START
            StringTokenizer tokens = new StringTokenizer(requestLine);
            tokens.nextToken();
            String fileName = tokens.nextToken();
            boolean fileExists = true;
            fileName = "." + fileName;
            FileInputStream fis = null; // WEBSERVER PART B END (FOR MY OWN REFERENCE)

            while ((headerLine = br.readLine()).length() != 0) { // PART B COULD BE A LOOP OF IT"S OWN BUT I DID NOT
                                                                 // WANT TO MAKE A DIFFERENT LOOP AND CONFUSE MYSELF
                                                                 // WITH MULTIPLE LOOPS AND STUFF
                System.out.println("Header: " + headerLine);
                // Open the requested file.
                try {
                    fis = new FileInputStream(fileName);
                } catch (FileNotFoundException e) {
                    fileExists = false;
                }
            }

            // Construct the response message.
            String statusLine = null;
            String contentTypeLine = null;
            String entityBody = null;
            if (fileExists) {
                statusLine = "HTTP/1.1 200 OK";
                contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
            } else {
                if (!contentType(fileName).equalsIgnoreCase("text/plain")) {
                    statusLine = "HTTP/1.0 404 Not Found";
                    contentTypeLine = "Content-type: text/html";
                    entityBody = "<HTML>" +
                            "<HEAD><TITLE>Not Found</TITLE></HEAD>" +
                            "<BODY>Not Found</BODY></HTML>";
                } else {
                    statusLine = "HTTP/1.1 200 OK";
                    contentTypeLine = "Content-type: text/plain";
                    // create an instance of ftp client
                    FtpClient ftpClient = new FtpClient();
                    // connect to the ftp server
                    ftpClient.connect("myuser", "mypassword");
                    // retrieve the file from the ftp server, remember you need to
                    // first upload this file to the ftp server under your user
                    // ftp directory
                    ftpClient.getFile(fileName);
                    // disconnect from ftp server
                    ftpClient.disconnect();
                    // assign input stream to read the recently ftp-downloaded file
                    fis = new FileInputStream(fileName);
                }
            }

            // Send the status line.
            os.writeBytes(statusLine);

            // Send the content type line.
            os.writeBytes(contentTypeLine);

            // Send a blank line to indicate the end of the header lines.
            os.writeBytes(CRLF);

            // Send the entity body
            if (fileExists) {
                sendBytes(fis, os);
                isf.close();
            } else {
                os.writeBytes(entityBody);
            }

            os.close();
            isf.close();
            socket.close();
        }

        // COPIED DIRECTLY FROM THE PDF
        private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {
            // Construct a 1K buffer to hold bytes on their way to the socket.
            byte[] buffer = new byte[1024];
            int bytes = 0;
            // Copy requested file into the socket's output stream.
            while ((bytes = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytes);
            }
        }
    }

    private static String contentType(String fileName) {
        if (fileName.endsWith(".html") || fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain"; // Add handling for .txt files
        }
        return "application/octet-stream"; // Default for unknown file types
    }
}
