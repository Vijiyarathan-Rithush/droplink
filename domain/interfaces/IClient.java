package domain.interfaces;

import domain.NetworkEndpoint;

public interface IClient
{
    boolean canConnect(NetworkEndpoint endpoint);

    void connect(NetworkEndpoint endpoint);

    void disconnect();

    boolean isConnected();
}