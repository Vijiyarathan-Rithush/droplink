package service.interfaces;

import domain.NetworkEndpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IClient
{
    boolean canConnect(NetworkEndpoint endpoint);

    void connect(NetworkEndpoint endpoint);

    void disconnect();

    boolean isConnected();

    InputStream getInputStream() throws IOException;

    OutputStream getOutputStream()throws IOException;
}