import domain.NetworkEndpoint;
import domain.TransferDecision;
import infrastructure.TcpClient;
import infrastructure.TcpServer;
import service.FileTransferService;
import service.IncomingTransferService;
import service.TransferDecisionService;
import service.interfaces.IClient;

import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Main
{
    public static void main() throws Exception
    {
        final int port = 5000;
        TcpServer server = new TcpServer();
        TransferDecisionService decisionService = new TransferDecisionService();

        Thread serverThread = new Thread(() ->
        {
            try
            {
                server.start(port);
                IClient receivingClient = server.getClient();
                FileTransferService receivingFileService =
                        new FileTransferService(receivingClient, decisionService);
                IncomingTransferService incomingTransferService =
                        new IncomingTransferService(receivingFileService, decisionService);

                var request = incomingTransferService.receiveRequest(receivingClient);
                System.out.println("Incoming file: " + request.fileName() + " (" + request.fileSize() + " bytes)");

                Path receivedFile = incomingTransferService.accept(
                        receivingClient,
                        request,
                        Path.of("received")
                );

                if (receivedFile == null)
                    System.out.println("Transfer rejected: target file already exists or request is invalid");
                else
                    System.out.println("File received: " + receivedFile.toAbsolutePath());
            }
            catch (Exception e)
            {
                System.err.println("Receiver failed: " + e.getMessage());
            }
            finally
            {
                if (server.isRunning()) server.stop();
            }
        });

        serverThread.start();
        Thread.sleep(500);

        Path file = Files.createTempFile("droplink-demo-", ".txt");
        Files.writeString(file, "Hello File Transfer!");

        IClient client = new TcpClient(new Socket());
        FileTransferService fileTransferService =
                new FileTransferService(client, decisionService);
        NetworkEndpoint endpoint = new NetworkEndpoint("127.0.0.1", port);

        try
        {
            TransferDecision decision = fileTransferService.send(endpoint, file);
            System.out.println(decision == TransferDecision.ACCEPTED
                    ? "File sent"
                    : "Transfer rejected by receiver");
        }
        finally
        {
            Files.deleteIfExists(file);
        }

        serverThread.join();
    }
}
