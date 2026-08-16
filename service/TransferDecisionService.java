package service;

import service.interfaces.ITransferDecisionService;
import domain.TransferDecision;

import java.io.*;

public class TransferDecisionService implements ITransferDecisionService
{
    @Override
    public void sendDecision(OutputStream outputStream, TransferDecision decision) throws IOException
    {
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        dataOutputStream.writeUTF(decision.name());
        dataOutputStream.flush();
    }

    @Override
    public TransferDecision receiveDecision(InputStream inputStream) throws IOException 
    {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        return TransferDecision.valueOf(dataInputStream.readUTF());
    }
}
