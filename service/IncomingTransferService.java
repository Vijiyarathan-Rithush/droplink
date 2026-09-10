package service;

import domain.TransferDecision;
import domain.TransferRequest;
import service.interfaces.IClient;
import service.interfaces.IFileTransferService;
import service.interfaces.ITransferDecisionService;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

public final class IncomingTransferService
{
    private final IFileTransferService fileTransferService;
    private final ITransferDecisionService transferDecisionService;

    public IncomingTransferService(
            IFileTransferService fileTransferService,
            ITransferDecisionService transferDecisionService)
    {
        this.fileTransferService = Objects.requireNonNull(fileTransferService, "fileTransferService cannot be null");
        this.transferDecisionService = Objects.requireNonNull(transferDecisionService, "transferDecisionService cannot be null");
    }

    public TransferRequest receiveRequest(IClient client) throws IOException
    {
        Objects.requireNonNull(client, "client cannot be null");
        DataInputStream inputStream = new DataInputStream(client.getInputStream());
        return new TransferRequest(inputStream.readUTF(), inputStream.readLong());
    }

    public Path accept(IClient client, TransferRequest request, Path outputDirectory) throws IOException
    {
        Objects.requireNonNull(client, "client cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory cannot be null");

        Path directory = outputDirectory.toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path target = resolveTarget(directory, request);

        if (target == null || Files.exists(target))
        {
            reject(client);
            return null;
        }

        transferDecisionService.sendDecision(client.getOutputStream(), TransferDecision.ACCEPTED);
        Path temporaryFile = Files.createTempFile(directory, ".droplink-", ".part");

        try
        {
            try (OutputStream outputStream = Files.newOutputStream(temporaryFile))
            {
                fileTransferService.receive(client.getInputStream(), outputStream, request.fileSize());
            }

            Files.move(temporaryFile, target);
            return target;
        }
        finally
        {
            Files.deleteIfExists(temporaryFile);
        }
    }

    public void reject(IClient client) throws IOException
    {
        Objects.requireNonNull(client, "client cannot be null");
        transferDecisionService.sendDecision(client.getOutputStream(), TransferDecision.REJECTED);
    }

    public void cancel()
    {
        fileTransferService.cancel();
    }

    public boolean isCancelled()
    {
        return fileTransferService.isCancelled();
    }

    private Path resolveTarget(Path outputDirectory, TransferRequest request)
    {
        if (request.fileSize() < 0 || request.fileName() == null || request.fileName().isBlank())
            return null;

        try
        {
            Path fileName = Path.of(request.fileName()).getFileName();
            if (fileName == null || !fileName.toString().equals(request.fileName())) return null;

            Path target = outputDirectory.resolve(fileName).normalize();
            return target.getParent().equals(outputDirectory) ? target : null;
        }
        catch (InvalidPathException e)
        {
            return null;
        }
    }
}
