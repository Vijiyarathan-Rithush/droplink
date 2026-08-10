import domain.NetworkEndpoint;
import service.interfaces.IClient;
import infrastructure.TcpClient;
import infrastructure.TcpServer;
import service.FileTransferService;

import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Main
{
    public static void main() throws Exception
    {
        final int port = 5000;

        TcpServer server = new TcpServer();

        Thread serverThread = new Thread(() ->
        {
            server.start(port);
            System.out.println("Client connected");
        });

        serverThread.start();

        Thread.sleep(500);

        Path file = Path.of("test.txt");

        Files.writeString(
                file,
                "Hello File Transfer!"
        );

        IClient client = new TcpClient(new Socket());

        FileTransferService fileTransferService =
                new FileTransferService(client);

        NetworkEndpoint endpoint =
                new NetworkEndpoint("127.0.0.1", port);

        fileTransferService.send(
                endpoint,
                file
        );

        System.out.println("File sent");
    }
}