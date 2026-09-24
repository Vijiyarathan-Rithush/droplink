package application;

import domain.IClient;
import domain.NetworkEndpoint;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.IllegalBlockingModeException;
import java.util.Objects;

public class Client implements IClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Client.class);

    private static final int CONNECTION_TIMEOUT = 5_000;

    private Socket socket;

    public Client() {
    }

    public Client(Socket socket) {
        this.socket = Objects.requireNonNull(socket);
    }

    @Override
    public void connect(final NetworkEndpoint endpoint) throws IOException {
        Objects.requireNonNull(endpoint, "Endpoint must not be null");

        if (isConnected()) {
            throw new IllegalStateException("Already connected");
        }

        InetSocketAddress socketAddress =
                new InetSocketAddress(endpoint.host(), endpoint.port());

        Socket newSocket = new Socket();

        try {
            LOGGER.info(
                    "Connecting to {}:{}",
                    endpoint.host(),
                    endpoint.port()
            );

            newSocket.connect(socketAddress, CONNECTION_TIMEOUT);
            this.socket = newSocket;

            LOGGER.info(
                    "Connected to {}:{}",
                    endpoint.host(),
                    endpoint.port()
            );
        } catch (IOException | IllegalBlockingModeException | IllegalArgumentException exception) {
            try {
                newSocket.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }

            LOGGER.error(
                    "Could not connect to {}:{}",
                    endpoint.host(),
                    endpoint.port(),
                    exception
            );

            throw exception;
        }
    }

    @Override
    public void disconnect() throws IOException
    {
        if (!isConnected()) {
            return;
        }

        LOGGER.info( "Disconnecting from {}:{}",
                socket.getInetAddress(),
                socket.getPort()
        );

        socket.close();

        LOGGER.info("Disconnected");
    }

    @Override
    public boolean isConnected()
    {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}