import domain.NetworkEndpoint;
import infrastructure.TcpClient;
import infrastructure.TcpServer;

import java.net.Socket;

public final class Main
{
    public static void main(String[] args) throws InterruptedException
    {
        final int port = 5000;
        TcpServer server = new TcpServer();

        Thread serverThread = new Thread(() ->{
            server.start(port);
            System.out.println("Client connected");
        });

        serverThread.start();

        Thread.sleep(500);

        TcpClient client = new TcpClient(new Socket());

        NetworkEndpoint enpoint = new NetworkEndpoint("127.0.0.1",port);

        client.connect(enpoint);

        System.out.println("Client: " + client.isConnected());
        System.out.println("Server: " + server.isRunning());

        client.disconnect();
        server.stop();
    }

}
