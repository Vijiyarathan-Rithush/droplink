package service.interfaces;

import domain.TransferDecision;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ITransferDecisionService
{
    public void sendDecision(OutputStream outputStream, TransferDecision decision) throws IOException;
    TransferDecision receiveDecision(InputStream inputStream) throws IOException;
}