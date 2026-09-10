package service.interfaces;

@FunctionalInterface
public interface TransferProgressListener
{
    TransferProgressListener NONE = (bytesTransferred, totalBytes) -> { };

    void onProgress(long bytesTransferred, long totalBytes);
}
