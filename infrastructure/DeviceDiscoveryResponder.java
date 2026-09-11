package infrastructure;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public final class DeviceDiscoveryResponder implements AutoCloseable
{
    private final String deviceName;
    private final String deviceId;
    private final int transferPort;
    private volatile DatagramSocket socket;
    private volatile boolean running;

    public DeviceDiscoveryResponder(String deviceName, int transferPort)
    {
        this(deviceName, transferPort, DeviceDiscoveryService.localDeviceId());
    }

    DeviceDiscoveryResponder(String deviceName, int transferPort, String deviceId)
    {
        this.deviceName = deviceName;
        this.transferPort = transferPort;
        this.deviceId = deviceId;
    }

    public void start() throws SocketException
    {
        if (running) return;
        DatagramSocket discoverySocket = new DatagramSocket(null);
        try
        {
            discoverySocket.setReuseAddress(true);
            discoverySocket.setBroadcast(true);
            discoverySocket.bind(new InetSocketAddress(DeviceDiscoveryService.DISCOVERY_PORT));
        }
        catch (SocketException error)
        {
            discoverySocket.close();
            throw error;
        }
        socket = discoverySocket;
        running = true;

        Thread thread = new Thread(this::listen, "droplink-discovery-responder");
        thread.setDaemon(true);
        thread.start();
    }

    private void listen()
    {
        byte[] buffer = new byte[1_024];
        while (running)
        {
            try
            {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                String requesterId = DeviceDiscoveryService.requesterId(request);
                if (requesterId == null || deviceId.equals(requesterId)) continue;

                byte[] response = DeviceDiscoveryService.response(deviceId, deviceName, transferPort);
                socket.send(new DatagramPacket(
                        response, response.length, request.getAddress(), request.getPort()));
            }
            catch (IOException ignored)
            {
                if (!running) return;
            }
        }
    }

    @Override
    public void close()
    {
        running = false;
        DatagramSocket activeSocket = socket;
        socket = null;
        if (activeSocket != null) activeSocket.close();
    }
}
