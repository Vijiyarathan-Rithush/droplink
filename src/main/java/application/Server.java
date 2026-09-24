package application;

import domain.IServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

public class Server implements IServer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private ServerSocket serverSocket;
    private Client client;
    @Override
    public void start(int port) throws Exception
    {
        if (isRunning()) throw new IllegalStateException("Server is already running");
        try
        {
            LOGGER.info("Starting server on port {}", port);
            serverSocket = new ServerSocket(port);
            client = new Client(serverSocket.accept());
            LOGGER.info("Server started on port {}", port);
        }
        catch (IOException e)
        {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public void stop() throws Exception
    {
        if(!isRunning()) throw new IllegalStateException("Server is not running");

        try
        {
            LOGGER.info("Stopping server on port {}", serverSocket.getLocalPort());
            serverSocket.close();
            LOGGER.info("Server stopped");
        }
        catch (IOException e)
        {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public boolean isRunning()
    {
        return serverSocket != null && !serverSocket.isClosed();
    }
}
