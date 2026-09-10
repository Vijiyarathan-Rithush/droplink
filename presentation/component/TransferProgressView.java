package presentation.component;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import presentation.util.FileSizeFormatter;

public final class TransferProgressView extends VBox
{
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label percentage = new Label("0 %");
    private final Label transferred = new Label("0 B von 0 B");

    public TransferProgressView()
    {
        setSpacing(9);
        getStyleClass().add("transfer-progress");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        percentage.getStyleClass().add("progress-percentage");
        transferred.getStyleClass().add("muted-label");
        getChildren().addAll(new HBox(12, progressBar, percentage), transferred);
    }

    public void update(long bytesTransferred, long totalBytes)
    {
        double progress = totalBytes == 0 ? 1 : (double) bytesTransferred / totalBytes;
        progressBar.setProgress(progress);
        percentage.setText(Math.round(progress * 100) + " %");
        transferred.setText(FileSizeFormatter.format(bytesTransferred) + " von " + FileSizeFormatter.format(totalBytes));
    }

    public void reset()
    {
        progressBar.setProgress(0);
        percentage.setText("0 %");
        transferred.setText("0 B von 0 B");
    }
}
