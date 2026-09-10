package service;

import service.interfaces.ITransferDecisionService;
import domain.TransferDecision;

import java.io.*;

public class TransferDecisionService implements ITransferDecisionService
{
    @Override
    public void sendDecision(OutputStream outputStream, TransferDecision decision) throws IOException
    {
        if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null");
        if (decision == null) throw new IllegalArgumentException("decision cannot be null");

        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        dataOutputStream.writeUTF(decision.name());
        dataOutputStream.flush();
    }

    @Override
    public TransferDecision receiveDecision(InputStream inputStream) throws IOException 
    {
        if (inputStream == null) throw new IllegalArgumentException("inputStream cannot be null");

        DataInputStream dataInputStream = new DataInputStream(inputStream);
        String value = dataInputStream.readUTF();

        try
        {
            return TransferDecision.valueOf(value);
        }
        catch (IllegalArgumentException e)
        {
            throw new IOException("Invalid transfer decision: " + value, e);
        }
    }
}
