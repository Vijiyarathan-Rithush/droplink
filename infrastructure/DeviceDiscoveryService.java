package infrastructure;

import domain.DiscoveredDevice;
import domain.NetworkEndpoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HexFormat;

public final class DeviceDiscoveryService
{
    public static final int DISCOVERY_PORT = 50_501;
    private static final String REQUEST = "DROPLINK_DISCOVER_V3";
    private static final String RESPONSE = "DROPLINK_DEVICE_V3";
    private static final String DEVICE_ID = createDeviceId();
    private static final int RECEIVE_POLL_MILLIS = 200;

    public List<DiscoveredDevice> discover(long durationMillis) throws IOException
    {
        long deadline = System.currentTimeMillis() + durationMillis;
        Map<String, DiscoveredDevice> devices = new LinkedHashMap<>();

        try (DatagramSocket socket = new DatagramSocket())
        {
            socket.setBroadcast(true);
            socket.setSoTimeout(RECEIVE_POLL_MILLIS);
            sendRequest(socket, InetAddress.getByName("255.255.255.255"));
            sendToInterfaceBroadcasts(socket);

            byte[] buffer = new byte[1_024];
            while (System.currentTimeMillis() < deadline)
            {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try
                {
                    socket.receive(packet);
                    DiscoveredDevice device = readResponse(packet);
                    if (device != null)
                    {
                        String key = device.endpoint().host() + ":" + device.endpoint().port();
                        devices.put(key, device);
                    }
                }
                catch (SocketTimeoutException ignored)
                {
                    // Poll until the discovery window closes.
                }
            }
        }

        List<DiscoveredDevice> result = new ArrayList<>(devices.values());
        result.sort(Comparator.comparing(DiscoveredDevice::name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public static String localDeviceName()
    {
        String computerName = System.getenv("COMPUTERNAME");
        if (computerName != null && !computerName.isBlank()) return computerName.trim();
        try
        {
            String hostName = InetAddress.getLocalHost().getHostName();
            if (hostName != null && !hostName.isBlank()) return hostName.trim();
        }
        catch (IOException ignored)
        {
            // Use a friendly fallback if the host name is unavailable.
        }
        return "DropLink-Gerät";
    }

    static byte[] response(String deviceId, String deviceName, int transferPort) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes))
        {
            output.writeUTF(RESPONSE);
            output.writeUTF(deviceId);
            output.writeUTF(deviceName);
            output.writeInt(transferPort);
        }
        return bytes.toByteArray();
    }

    static String requesterId(DatagramPacket packet)
    {
        try (DataInputStream input = new DataInputStream(
                new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength())))
        {
            return REQUEST.equals(input.readUTF()) ? input.readUTF() : null;
        }
        catch (IOException ignored)
        {
            return null;
        }
    }

    static String localDeviceId()
    {
        return DEVICE_ID;
    }

    private void sendRequest(DatagramSocket socket, InetAddress address) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes))
        {
            output.writeUTF(REQUEST);
            output.writeUTF(DEVICE_ID);
        }
        byte[] request = bytes.toByteArray();
        socket.send(new DatagramPacket(request, request.length, address, DISCOVERY_PORT));
    }

    private void sendToInterfaceBroadcasts(DatagramSocket socket) throws SocketException
    {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces != null && interfaces.hasMoreElements())
        {
            NetworkInterface network = interfaces.nextElement();
            if (!network.isUp() || network.isLoopback()) continue;
            network.getInterfaceAddresses().stream()
                    .map(address -> address.getBroadcast())
                    .filter(address -> address instanceof Inet4Address)
                    .forEach(address ->
                    {
                        try
                        {
                            sendRequest(socket, address);
                        }
                        catch (IOException ignored)
                        {
                            // Another active interface can still discover devices.
                        }
                    });
        }
    }

    private DiscoveredDevice readResponse(DatagramPacket packet)
    {
        try (DataInputStream input = new DataInputStream(
                new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength())))
        {
            if (!RESPONSE.equals(input.readUTF())) return null;
            String responderId = input.readUTF();
            if (DEVICE_ID.equals(responderId)) return null;
            String deviceName = input.readUTF();
            int transferPort = input.readInt();
            String host = packet.getAddress().getHostAddress();
            return new DiscoveredDevice(deviceName, new NetworkEndpoint(host, transferPort));
        }
        catch (IOException | IllegalArgumentException ignored)
        {
            return null;
        }
    }

    private static String createDeviceId()
    {
        try
        {
            List<String> identifiers = new ArrayList<>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements())
            {
                NetworkInterface network = interfaces.nextElement();
                byte[] hardwareAddress = network.getHardwareAddress();
                if (hardwareAddress != null && hardwareAddress.length > 0)
                    identifiers.add(network.getName() + ":" + HexFormat.of().formatHex(hardwareAddress));
            }
            identifiers.sort(String.CASE_INSENSITIVE_ORDER);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(localDeviceName().getBytes(StandardCharsets.UTF_8));
            for (String identifier : identifiers)
                digest.update(identifier.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        }
        catch (SocketException | NoSuchAlgorithmException ignored)
        {
            return localDeviceName();
        }
    }
}
