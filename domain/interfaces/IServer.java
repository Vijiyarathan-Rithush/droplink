package domain.interfaces;

public interface IServer
{
    void start(int port);

    void stop();

    void closeServerSocket();

    boolean isRunning();
}