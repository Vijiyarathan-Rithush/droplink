package domain;

public interface IServer
{
    void start(int port) throws Exception;

    void stop() throws Exception;

    boolean isRunning();
}
