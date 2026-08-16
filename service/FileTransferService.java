package service;

import domain.NetworkEndpoint;
import domain.TransferDecision;
import service.interfaces.IClient;
import service.interfaces.IFileTransferService;
import service.interfaces.ITransferDecisionService;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileTransferService implements IFileTransferService
{
    private final IClient client;
    private final ITransferDecisionService transferDecisionService;
    private static final int BUFFER_SIZE = 8192;
    private static final int END_OF_FILE = -1;

    public FileTransferService(IClient client, ITransferDecisionService transferDecisionService)
    {
        this.client = client;
        this.transferDecisionService = transferDecisionService;
    }

    @Override
    public void send(NetworkEndpoint endpoint, Path file) throws IOException
    {
        if (endpoint == null)
            throw new IllegalArgumentException("endpoint cannot be null");
        if (file == null)
            throw new IllegalArgumentException("file cannot be null");
        if (!Files.isRegularFile(file)) throw new IllegalArgumentException("File does not exist");

        client.connect(endpoint);

        try
        {
            DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());

            String fileName = file.getFileName().toString();
            long fileSize = Files.size(file);

            outputStream.writeUTF(fileName);
            outputStream.writeLong(fileSize);
            outputStream.flush();

            TransferDecision decision = transferDecisionService.receiveDecision(client.getInputStream());

            if (decision == TransferDecision.REJECTED)
            {
                System.out.println("Transfer rejected");
                return;
            }

            try(FileInputStream fileInputStream = new FileInputStream(file.toFile()))
            {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = fileInputStream.read(buffer)) != END_OF_FILE)
                {
                    outputStream.write(buffer,0,bytesRead);
                }
            }
            outputStream.flush();
        }
        finally
        {
            if (client.isConnected())
            {
                client.disconnect();
            }
        }
    }

    @Override
    public void receive(InputStream inputStream, OutputStream outputStream, long fileSize) throws IOException
    {
        if(inputStream == null) throw new IllegalArgumentException("inputstream cannot be null");
        if(outputStream == null) throw new IllegalArgumentException("Outputstream cannot be null");
        if(fileSize < 0) throw new IllegalArgumentException("fileSize cannot be negative");

        byte[] buffer = new byte[BUFFER_SIZE];
        long remainingBytes = fileSize;

        while (remainingBytes > 0)
        {
            int bytesToRead = (int) Math.min(buffer.length, remainingBytes);
            int bytesRead = inputStream.read(buffer,0,bytesToRead);

            if (bytesRead == END_OF_FILE) throw new IOException("Connection closed before file transfer was completed");

            outputStream.write(buffer, 0,bytesRead);
            remainingBytes -= bytesRead;
        }
        outputStream.flush();
    }
}
