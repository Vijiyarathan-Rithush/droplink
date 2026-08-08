package infrastructure;

import domain.interfaces.IServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public final class TcpServer implements IServer
{
    private ServerSocket serverSocket;
    private TcpClient tcpClient;
    private PrintWriter output;
    private BufferedReader input;



    @Override
    public void start(int port)
    {
        if (isRunning()) throw new IllegalStateException("Server is already running");
        try
        {
            serverSocket = new ServerSocket(port);
            Socket clientSocket = serverSocket.accept();
            tcpClient = new TcpClient(clientSocket);
        }
        catch (IOException | SecurityException  | IllegalArgumentException e)
        {
            closeServerSocket();
            throw new RuntimeException("Failed to start server: " + e.getMessage(), e);
        }
    }

    @Override
    public void stop()
    {
        if (!isRunning()) throw new IllegalStateException("Server has already stopped");
        try
        {
            if (tcpClient != null && tcpClient.isConnected())
            {
                tcpClient.disconnect();
            }
            serverSocket.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to stop server" + e.getMessage(), e);
        }
        finally
        {
            tcpClient = null;
            serverSocket = null;
        }
    }

    @Override
    public boolean isRunning()
    {
        return serverSocket != null && !serverSocket.isClosed();
    }

    @Override
    public void closeServerSocket()
    {
        if (serverSocket == null) return;

        try
        {
            serverSocket.close();
        } catch (IOException e)
        {
            throw new RuntimeException("Failed to close Server: " + e.getMessage(), e);
        }
        finally
        {
            serverSocket = null;
        }
    }
}
