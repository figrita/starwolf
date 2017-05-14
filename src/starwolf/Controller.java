package starwolf;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
import java.io.File;
import java.io.IOException;

public class Controller {
    private Connector serialLink;
    @FXML
    private VBox root;
    @FXML
    private HBox mainSpace;
    @FXML
    private VBox toolSpace;
    @FXML
    private TextArea termOut;
    @FXML
    private TextField terminal;
    @FXML
    private SWCanvas canvas;
    @FXML
    private Label statusLeft;
    @FXML
    private ComboBox portBox;

    @FXML
    protected void initialize() {
        double w, h;
        initSerialLink();

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        w = screenBounds.getWidth();
        h = screenBounds.getHeight() - 24.0;

        double toolSpaceWidth = 350.0, mainSpaceHeight = h - 48.0;

        root.setPrefSize(w, h);
        mainSpace.setPrefHeight(mainSpaceHeight);
        mainSpace.setPickOnBounds(false);
        toolSpace.setPrefWidth(toolSpaceWidth);
        canvas.setHeight((int) (mainSpaceHeight - 18));
        canvas.setWidth((int) (w - toolSpaceWidth - 2));
        toolSpace.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                new BorderWidths(0.0, 1.0, 0.0, 0.0))));

        statusLeft.setText("Initialized - OK");
        updatePortList();
    }

    @FXML
    protected void updatePortList() {
        portBox.getItems().clear();
        portBox.getItems().addAll(Connector.listPorts());
    }

    @FXML
    protected void cameraConnect() {
        serialLink.connectToCamera((String) portBox.getSelectionModel().getSelectedItem());
    }

    @FXML
    protected void grabMeta() {
        Connector.command = "xsize?";
        serialLink.write("xsize?");
    }


    @FXML
    protected void menuActionFileOpen(ActionEvent event) {
        statusLeft.setText("Opening File ... ");

        try {
            FileChooser fileChooser = new FileChooser();
            File file;
            String fileName, ext;

            fileChooser.setTitle("Open File");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Images",
                    "*.jpg", "*.JPG",
                    "*.png", "*.PNG",
                    "*.gif", "*.GIF",
                    "*.fits", "*.FITS",
                    "*.fit", "*.FIT",
                    "*.fts", "*.FTS"
            ));
            file = fileChooser.showOpenDialog(null);
            fileName = file.getName();
            ext = fileName.substring(fileName.lastIndexOf("."), fileName.length());

            if (ext.matches("^\\.[Ff][Ii]?[Tt][Ss]?$")) {
                Fits f = new Fits(file);
                ImageHDU hdu = (ImageHDU) f.getHDU(0);
                ImageData imageData = hdu.getData();

                hdu.info(System.out);

                int[] axes = hdu.getAxes();
                canvas.fillBuffer(imageData.getData());
            } else {
                Image image = new Image("file:" + file.getAbsolutePath());
                canvas.setXYSize((int) image.getWidth(), (int) image.getHeight());
                canvas.fillBuffer(image.getPixelReader());
            }

            statusLeft.setText("OK");
        } catch (FitsException | IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void menuActionFileSave(ActionEvent event) {

    }

    @FXML
    protected void menuActionFileQuit(ActionEvent event) {
        Platform.exit();
        System.exit(0);
    }

    @FXML
    protected void menuActionImageDeInterlace(ActionEvent event) {
        canvas.deinterlace();
    }

    @FXML
    protected void menuActionImageFHT(ActionEvent event) {
        canvas.fasthartley();
    }

    @FXML
    private void terminalAction(ActionEvent event) {
        String temp = terminal.getText();
        serialLink.write(temp);
        termOut.appendText(temp + "\n");
        terminal.clear();
    }

    @FXML

    private void initSerialLink() {
        serialLink = new Connector();
        serialLink.initialize(termOut);
        serialLink.setCurrentCanvas(canvas);
        Thread t = new Thread() {
            public void run() {
                //the following line will keep this app alive for 1000 seconds,
                //waiting for events to occur and responding to them (printing incoming messages to console).
                try {
                    Thread.sleep(1000000);
                } catch (InterruptedException ie) {
                }
            }
        };
        t.start();
    }
}