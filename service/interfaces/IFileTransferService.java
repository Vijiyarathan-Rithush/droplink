package service.interfaces;

import domain.NetworkEndpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;


public interface IFileTransferService
{
    public void send(NetworkEndpoint endpoint, Path file) throws IOException;
    public void receive(InputStream inputStream, OutputStream outputStream, long fileSize) throws IOException;
}
