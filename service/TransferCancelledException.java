package service;

import java.io.IOException;

public final class TransferCancelledException extends IOException
{
    public TransferCancelledException()
    {
        super("Transfer was cancelled");
    }

    public TransferCancelledException(Throwable cause)
    {
        super("Transfer was cancelled", cause);
    }
}
