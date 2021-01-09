package sample;
import eu.hansolo.tilesfx.Tile.TextSize;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.TreeMap;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import eu.hansolo.tilesfx.Section;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.Tile.TileColor;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.colors.Bright;
import eu.hansolo.tilesfx.colors.Dark;
import eu.hansolo.tilesfx.tools.Helper;
import javafx.util.Duration;
import javax.imageio.ImageTranscoder;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.swing.JButton;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderOutput;
import javafx.embed.swing.SwingFXUtils;
public class Main extends Application {

  Stage window;
  private Tile speedometer;
  private Tile tachometer;
  private Tile topTile;
  private Tile fluidTile;
  private Tile clockTile;
  private Tile numberTile;
  private Tile engineTemp;
  private static final double TILE_WIDTH = 300;
  private static final double TILE_HEIGHT = 325;
  private static final double TOP_TILE_WIDTH = TILE_WIDTH *1.5;
  private static final double TOP_TILE_HEIGHT = 150;
  private long lastTimerCall;
  private AnimationTimer timer;
  private static final Random RND = new Random();

  private TreeMap<Integer, Double> speedCoefficients = new TreeMap<>();
  private TreeMap<Integer, Double> roundsReductions = new TreeMap<>();
  private boolean isBlinkerOn = false;
  private int currentGear = 1;
  private JButton btn;
  public void updateMeter(Tile tile) {
    if (tile.getValue() < tile.getMaxValue() * 0.4) {
      tile.setBarColor(Bright.GREEN);
    }
    if (tile.getValue() > tile.getMaxValue() * 0.4 && tile.getValue() < tile.getMaxValue() * 0.8) {
      tile.setBarColor(Bright.BLUE);
    }
    if (tile.getValue() > tile.getMaxValue() * 0.8) {
      tile.setBarColor(Bright.RED);
    }
    tile.setNeedleColor(tile.getBarColor());
    tile.setThreshold(tile.getValue());
    tile.setNeedleColor(tile.getBarColor());
  }

  @Override
  public void init() {
    speedometer = TileBuilder.create()
        .skinType(SkinType.GAUGE)
        .prefSize(TILE_WIDTH, TILE_HEIGHT)
        .unit("Kmh")
        .maxValue(200)
        .startFromZero(true)
        .decimals(0)
        .barColor(Color.WHITE)
        .borderWidth(0)
        .thresholdColor(Color.WHITE)
        .roundedCorners(false)
        .build();
    tachometer = TileBuilder.create()
        .skinType(SkinType.GAUGE)
        .prefSize(TILE_WIDTH, TILE_HEIGHT)
        .unit("Rpm\nx100")
        .text("x100")
        .maxValue(80)
        .startFromZero(true)
        .decimals(0)
        .textSize(TextSize.SMALLER)
        .barColor(Color.WHITE)
        .thresholdColor(Color.WHITE)
        .roundedCorners(false)
        .build();
    clockTile = TileBuilder.create()
        .skinType(SkinType.CLOCK)
        .prefSize(TOP_TILE_WIDTH, TOP_TILE_HEIGHT)
        .locale(Locale.UK)
        .dateVisible(false)
        .roundedCorners(false)
        .running(true)
        .build();
    topTile = TileBuilder.create()
        .skinType(SkinType.GAUGE)
        .prefSize(TOP_TILE_WIDTH, TOP_TILE_HEIGHT)
        .title("")
        .unit("Kmh")
        .fillWithGradient(false)
        .roundedCorners(false)
        .build();
    engineTemp = TileBuilder.create().
        skinType(SkinType.FIRE_SMOKE)
        .prefSize(TOP_TILE_WIDTH, TOP_TILE_HEIGHT)
        .description("Engine temp")
        .unit("\u00b0C")
        .roundedCorners(false)
        .decimals(0)
        .build();
    fluidTile = TileBuilder.create().skinType(SkinType.FLUID)
        .prefSize(TILE_WIDTH, 150)
        .title("50l")
        .unit("\u0025")
        .decimals(0)
        .text("0l")
        .tickLabelColor(Helper.getColorWithOpacity(Tile.FOREGROUND, 0.5))
        .sections(new Section(0, 20, "Low", Helper.getColorWithOpacity(Dark.RED, 0.6)),
            new Section(20, 40, "Ok", Helper.getColorWithOpacity(Bright.ORANGE, 0.6)),
            new Section(40, 100, "High", Helper.getColorWithOpacity(Bright.GREEN, 0.6)))
        .highlightSections(true)
        .sectionsVisible(true)
        .animated(true)
        .roundedCorners(false)
        .build();
    numberTile = TileBuilder.create()
        .skinType(SkinType.NUMBER)
        .prefSize(TILE_WIDTH, 80)
        .textSize(TextSize.BIGGER)
        .value(999999)
        .unit("")
        .description("Spalanie")
        .textVisible(true)
        .roundedCorners(false)
        .build();

    /* speed = rpm/100 / gears.get(gear) */
    speedCoefficients.put(1, 1.22304);
    speedCoefficients.put(2, 0.73532);
    speedCoefficients.put(3, 0.5187);
    speedCoefficients.put(4, 0.39);
    speedCoefficients.put(5, 0.31746);
    roundsReductions.put(2, 15d);
    roundsReductions.put(3, 17.5);
    roundsReductions.put(4, 18.5);
    roundsReductions.put(5, 20d);
    tachometer.setValue(5);

//    lastTimerCall = System.nanoTime();
//    timer = new AnimationTimer() {
//      @Override
//      public void handle(long now) {
//        if (now > lastTimerCall + 3_500_000_000L) {
//          speedometer.setValue(
//              RND.nextDouble() * speedometer.getRange() * 1.0 + speedometer.getMinValue());
//          tachometer
//              .setValue(RND.nextDouble() * tachometer.getRange() * 1.0 + tachometer.getMinValue());
//          updateMeter(speedometer);
//          updateMeter(tachometer);
//          engineTemp.setValue(RND.nextDouble() * 100);
//          topTile.setValue(RND.nextDouble() * topTile.getRange() * 1.5 + topTile.getMinValue());
//          fluidTile.setValue(RND.nextDouble() * 100);
//          lastTimerCall = now;
//        }
//      }
//    };
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    window = primaryStage;
    primaryStage.setTitle("Hello World");
    BorderPane border = new BorderPane();
    FlowGridPane top = new FlowGridPane(2, 1, clockTile, engineTemp);
    border.setTop(top);
    border.setLeft(speedometer);
    border.setRight(tachometer);
    AnchorPane pane = new AnchorPane();

    HBox hbox = new HBox();
    hbox.setSpacing(10);
    ArrayList<String> backgrounds = new ArrayList<>();
    backgrounds.add("turn-left");
    backgrounds.add("turn-right");
    backgrounds.add("fan");
    backgrounds.add("malfunction");
    backgrounds.add("back-window");
    for(int i = 0; i < 5; i++) {
      Button button = new Button();
      button.setPrefSize(40, 40);
      button.getStyleClass().add(backgrounds.get(i));
      hbox.getChildren().add(button);
    }
    // make icon active
    // hbox.getChildren().get(1).getStyleClass().add("active");

    AnchorPane.setTopAnchor(fluidTile, 40d);
    AnchorPane.setBottomAnchor(fluidTile, 80d);
    AnchorPane.setBottomAnchor(numberTile,0d);
    pane.getChildren().addAll(hbox, fluidTile,numberTile);
    border.setCenter(pane);
    border.getStyleClass().add("bck");
    Scene scene = new Scene(border, TILE_WIDTH * 3, 500);
    scene.getStylesheets().add("styles.css");
    primaryStage.setScene(scene);
    primaryStage.setResizable(false);

    Media gearSound = new Media(getClass().getResource("/sound_001.wav").toExternalForm());
    Media lastGearSound = new Media(getClass().getResource("/sound_002.wav").toExternalForm());
    Media releasedSound = new Media(getClass().getResource("/sound_003.wav").toExternalForm());
    Media turnSound = new Media(getClass().getResource("/turn.wav").toExternalForm());
    MediaPlayer gearPlayer = new MediaPlayer(gearSound);
    MediaPlayer lastGearPlayer = new MediaPlayer(lastGearSound);
    MediaPlayer releasedPlayer = new MediaPlayer(releasedSound);
    MediaPlayer turnPlayer = new MediaPlayer(turnSound);
    turnPlayer.setCycleCount(MediaPlayer.INDEFINITE);
    lastGearPlayer.setCycleCount(MediaPlayer.INDEFINITE);

    scene.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.UP) {
        if (tachometer.getValue() < 65) {
          tachometer.setValue(1.005 * tachometer.getValue() + 0.1);
        }
        if (speedometer.getValue() < 188) {
          speedometer.setValue(tachometer.getValue() / speedCoefficients.get(currentGear));
        }
        if (currentGear == 1 && !gearPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)) {
          gearPlayer.seek(Duration.millis(0));
          gearPlayer.play();
        }
        if (currentGear < 5 && (int) tachometer.getValue() == 25) {
          currentGear += 1;
          tachometer.setValue(roundsReductions.get(currentGear));
          gearPlayer.seek(Duration.millis(0));
          gearPlayer.play();
        }
        if (currentGear == 5 && !lastGearPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)) {
          gearPlayer.stop();
          lastGearPlayer.seek(Duration.millis(0));
          lastGearPlayer.play();
        }
        updateMeter(tachometer);
        updateMeter(speedometer);
      } else if (keyEvent.getCode() == KeyCode.DOWN) {
        if (tachometer.getValue() > 5) {
          tachometer.setValue(0.99 * tachometer.getValue() - (tachometer.getValue() > 1 ? 0.1 : 0));
        }
        if (speedometer.getValue() > 0) {
          speedometer.setValue(0.99 * speedometer.getValue() - (speedometer.getValue() > 0.2 ? 0.1 : 0));
        }
        if (currentGear > 1 && (int) tachometer.getValue() == 25) {
          currentGear -= 1;
        }
        updateMeter(tachometer);
        updateMeter(speedometer);
      } else if (keyEvent.getCode() == KeyCode.LEFT || keyEvent.getCode() == KeyCode.RIGHT) {
        if (!isBlinkerOn) {
          turnPlayer.seek(Duration.millis(0));
          turnPlayer.play();
          isBlinkerOn = true;
        } else {
          turnPlayer.stop();
          isBlinkerOn = false;
        }
      }
    });
    scene.setOnKeyReleased(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.UP) {
        gearPlayer.stop();
        lastGearPlayer.stop();
        releasedPlayer.seek(Duration.millis(0));
        releasedPlayer.play();
      }
    });
    primaryStage.show();
  }


  public static void main(String[] args) {
    launch(args);
  }
}
