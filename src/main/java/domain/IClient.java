package domain;

public interface IClient
{
    void connect(NetworkEndpoint endpoint) throws Exception;

    void disconnect() throws Exception;

    boolean isConnected();
}
