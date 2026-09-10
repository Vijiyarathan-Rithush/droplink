package service;

import domain.NetworkEndpoint;
import domain.TransferDecision;
import service.interfaces.IClient;
import service.interfaces.IFileTransferService;
import service.interfaces.ITransferDecisionService;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class FileTransferService implements IFileTransferService
{
    private final IClient client;
    private final ITransferDecisionService transferDecisionService;
    private volatile boolean cancelled;
    private static final int BUFFER_SIZE = 8192;
    private static final int END_OF_FILE = -1;

    public FileTransferService(IClient client, ITransferDecisionService transferDecisionService)
    {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.transferDecisionService = Objects.requireNonNull(transferDecisionService, "transferDecisionService cannot be null");
    }

    @Override
    public TransferDecision send(NetworkEndpoint endpoint, Path file) throws IOException
    {
        cancelled = false;
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
                return decision;
            }

            try(FileInputStream fileInputStream = new FileInputStream(file.toFile()))
            {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = fileInputStream.read(buffer)) != END_OF_FILE)
                {
                    throwIfCancelled();
                    outputStream.write(buffer,0,bytesRead);
                }
            }
            throwIfCancelled();
            outputStream.flush();
            return decision;
        }
        catch (IOException e)
        {
            if (cancelled) throw new TransferCancelledException(e);
            throw e;
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

        try
        {
            while (remainingBytes > 0)
            {
                throwIfCancelled();
                int bytesToRead = (int) Math.min(buffer.length, remainingBytes);
                int bytesRead = inputStream.read(buffer,0,bytesToRead);

                if (bytesRead == END_OF_FILE) throw new IOException("Connection closed before file transfer was completed");

                outputStream.write(buffer, 0,bytesRead);
                remainingBytes -= bytesRead;
            }
            throwIfCancelled();
            outputStream.flush();
        }
        catch (IOException e)
        {
            if (cancelled) throw new TransferCancelledException(e);
            throw e;
        }
    }

    @Override
    public void cancel()
    {
        cancelled = true;

        if (client.isConnected())
        {
            try
            {
                client.disconnect();
            }
            catch (RuntimeException ignored)
            {
                // The connection may already have been closed by the transfer thread.
            }
        }
    }

    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    private void throwIfCancelled() throws TransferCancelledException
    {
        if (cancelled) throw new TransferCancelledException();
    }
}
