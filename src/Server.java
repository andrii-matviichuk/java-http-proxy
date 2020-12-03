import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public void listenSocket() {
        ServerSocket server = null;
        Socket client = null;
        try {
            server = new ServerSocket(8080);
        }
        catch (IOException e) {
            System.out.println("Could not listen");
            System.exit(-1);
        }
        System.out.println("Server listens on port: " + server.getLocalPort());

        while(true) {
            try {
                client = server.accept();
            }
            catch (IOException e) {
                System.out.println("Accept failed");
                System.exit(-1);
            }
            ServerThread st = new ServerThread(client);
            st.start();
        }

    }

    public static void main(String[] args) {
        Server server = new Server();
        server.listenSocket();
    }
}
