package infrastructure;

import domain.NetworkEndpoint;
import domain.interfaces.IClient;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class TcpClient implements IClient
{
    private static final int DEFAULT_TIMEOUT = 5_000;
    private Socket socket;

    public TcpClient(Socket socket)
    {
        this.socket = socket;
    }

    @Override
    public boolean canConnect(NetworkEndpoint endpoint) 
    {
        if (endpoint == null) throw new IllegalArgumentException("Endpoint cannot be null");

        InetSocketAddress socketAddress = new InetSocketAddress(endpoint.ip(), endpoint.port());

        try (Socket testSocket = new Socket())
        {
            testSocket.connect(socketAddress, DEFAULT_TIMEOUT);
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    @Override
    public void connect(NetworkEndpoint endpoint) 
    {
        if (endpoint == null) throw new IllegalArgumentException("Endpoint cannot be null");
        if (isConnected()) throw new IllegalStateException("Already connected to an endpoint");

        InetSocketAddress socketAddress = new InetSocketAddress(endpoint.ip(), endpoint.port());
        Socket clientSocket = new Socket();
        
        try
        {
            clientSocket.connect(socketAddress, DEFAULT_TIMEOUT);
            this.socket = clientSocket;
        }
        catch (IOException e)
        {
            try 
            {
                clientSocket.close();
            } 
            catch (IOException closeException) 
            {
                System.out.println("Connection failed and failed to close the socket: " + closeException.getMessage());
            }
            socket = null;
            throw new RuntimeException("Failed to connect to the endpoint: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() 
    {
        if (socket == null) throw new IllegalStateException("Not connected to any endpoint");
            try 
            {
                if (!socket.isClosed()) socket.close();
            } 
            catch (IOException e) 
            {
                throw new RuntimeException("Failed to disconnect from the endpoint: " + e.getMessage(), e);
            }
            finally 
            {
                socket = null;
            }
    }

    @Override
    public boolean isConnected() 
    {
        return !(socket == null || socket.isClosed() || !socket.isConnected());
    }
}
