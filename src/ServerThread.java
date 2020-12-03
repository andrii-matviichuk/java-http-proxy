import java.io.*;
import java.net.Socket;
import java.net.URL;

class ServerThread extends Thread {

    private Socket clientSocket;
    private Socket connection;


    public ServerThread(Socket socket) {
        super();
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream clientToProxy = clientSocket.getInputStream();
            String request = "";
            StringBuilder fullRequest = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            request = br.readLine();
            fullRequest.append(request);
            fullRequest.append("\r\n");
            String line = "";
            while (!(line = br.readLine()).equals("")) {
                fullRequest.append(line);
                fullRequest.append("\r\n");
            }

            String[] arr = request.split(" ");
            String urlAdress = arr[1];

            String url = "";
            if (arr[0].equals("CONNECT")) {
                String[] tmp = arr[1].split(":");
                url = tmp[0];
            }

            System.out.println(("Client connected: " + urlAdress));

            int port = -1;
            boolean result = arr[1].matches(":\\d*");
            if (result) {
                int ind = arr[1].indexOf(":", 6);
                String strPort = arr[1].substring(ind);
                port = Integer.parseInt(strPort);
            }

            connection = null;
            boolean isHttps = false;

            if (!arr[1].substring(0, 7).equals("http://")) {
                isHttps = true;
            }

            if (port > 0) {
                if (isHttps) {
                    connection = new Socket(url, port);
                } else {
                    connection = new Socket(new URL(urlAdress).getHost(), port);
                }
            } else {
                if (isHttps) {
                    connection = new Socket(url, 443);
                } else {
                    connection = new Socket(new URL(urlAdress).getHost(), 80);
                }
            }


            BufferedWriter proxyToclientBW = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            if (!isHttps) {
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(connection.getOutputStream())));
                out.println();
                out.print(fullRequest);
                out.println();
                out.flush();

                InputStream connectionToProxy = connection.getInputStream();

                int k;
                while ((k = connectionToProxy.read()) != -1) {
                    clientSocket.getOutputStream().write((char) k);
                }

                clientSocket.getOutputStream().flush();

                connectionToProxy.close();
                out.close();
            } else {
                proxyToclientBW.write("HTTP/1.0 200 Connection established\r\n" +
                        "Proxy-Agent: ProxyServer/1.0\r\n" +
                        "\r\n");
                proxyToclientBW.flush();


                new Thread(() -> makeHttpsConnection(clientSocket, connection)).start();

                makeHttpsConnection(connection, clientSocket);

                clientSocket.close();
                proxyToclientBW.close();
                br.close();
            }

            if (connection != null) {
                connection.close();
            }

            clientToProxy.close();
        } catch (
                IOException e) {
            e.printStackTrace();
        }

    }

    private void makeHttpsConnection(Socket clientSocket, Socket connection) {
        byte[] buffer = new byte[4096];
        int read;
        try {
            do {
                read = clientSocket.getInputStream().read(buffer);
                if (read > 0) {
                    connection.getOutputStream().write(buffer, 0, read);
                    if (clientSocket.getInputStream().available() < 1) {
                        connection.getOutputStream().flush();
                    }
                }
            } while (read >= 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}