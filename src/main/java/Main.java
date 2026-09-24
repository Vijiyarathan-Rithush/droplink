import application.Client;
import application.Server;
import domain.NetworkEndpoint;

import java.io.IOException;
import java.lang.reflect.GenericDeclaration;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public final class Main {

    public static void main(String[] args) throws Exception {
        NetworkEndpoint endpoint =
                new NetworkEndpoint("127.0.0.1", 5000);

        Server server = new Server();

        Thread serverThread = new Thread(
                () -> {
                    try
                    {
                        server.start(endpoint.port());
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
        );

        serverThread.start();

        Thread.sleep(100);

        Client client = new Client();

        if (!client.isConnected()) {
            client.connect(endpoint);
        }
    }
}