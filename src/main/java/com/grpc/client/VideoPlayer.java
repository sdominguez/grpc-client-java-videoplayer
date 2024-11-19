package com.grpc.client;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class VideoPlayer extends JFrame {
    private MediaPlayer mediaPlayer;
    private static final String TEMP_VIDEO_PATH = "temp_video.mp4";
    private JFXPanel fxPanel;

    public VideoPlayer() {
        initComponents();
        Thread descarga = new Thread(new DownloadVideoClient(TEMP_VIDEO_PATH, "vagrantInstalation123456"));
        Thread panel = new Thread(() -> {
            Platform.runLater(() -> initFX());
        });
        descarga.start();
        panel.start();
        try {
            descarga.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void initComponents() {
        setTitle("Reproductor de Video");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        add(mainPanel);

        fxPanel = new JFXPanel();
        mainPanel.add(fxPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        JButton playButton = new JButton("Play");
        JButton pauseButton = new JButton("Pause");
        JButton stopButton = new JButton("Stop");
        controlPanel.add(playButton);
        controlPanel.add(pauseButton);
        controlPanel.add(stopButton);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        playButton.addActionListener(e -> Platform.runLater(() -> {
            if (mediaPlayer != null) mediaPlayer.play();
        }));

        pauseButton.addActionListener(e -> Platform.runLater(() -> {
            if (mediaPlayer != null) mediaPlayer.pause();
        }));

        stopButton.addActionListener(e -> Platform.runLater(() -> {
            if (mediaPlayer != null) mediaPlayer.stop();
        }));
    }

    private void initFXOLD() {
        Platform.runLater(() -> {
            File tempFile = new File(TEMP_VIDEO_PATH);
            if (!tempFile.exists() || tempFile.length() == 0) {
                System.err.println("El archivo temporal no existe o está vacío. Reproducción fallida.");
                return;
            }

            Media media = new Media(tempFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(mediaPlayer);
            mediaView.setPreserveRatio(true);

            javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane();
            root.getChildren().add(mediaView);

            root.widthProperty().addListener((obs, oldVal, newVal) -> mediaView.setFitWidth(newVal.doubleValue()));
            root.heightProperty().addListener((obs, oldVal, newVal) -> mediaView.setFitHeight(newVal.doubleValue()));

            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.dispose();
                if (tempFile.delete()) {
                    System.out.println("Archivo temporal eliminado.");
                } else {
                    System.err.println("Error al eliminar el archivo temporal.");
                }
            });

            Scene scene = new Scene(root);
            fxPanel.setScene(scene);
        });
    }

    private void initFX() {
        Platform.runLater(() -> {
            File tempFile = new File(TEMP_VIDEO_PATH);
            if (!tempFile.exists() || tempFile.length() == 0) {
                System.err.println("El archivo temporal no existe o está vacío. Reproducción fallida.");
                return;
            }

            Media media = new Media(tempFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(mediaPlayer);
            mediaView.setPreserveRatio(true);

            javafx.scene.layout.VBox root = new javafx.scene.layout.VBox();
            javafx.scene.layout.StackPane videoPane = new javafx.scene.layout.StackPane();
            videoPane.getChildren().add(mediaView);

            videoPane.setPrefHeight(Double.MAX_VALUE);
            VBox.setVgrow(videoPane, javafx.scene.layout.Priority.ALWAYS);

            root.getChildren().add(videoPane);


            javafx.scene.control.Slider progressBar = new javafx.scene.control.Slider();
            progressBar.setMin(0);
            progressBar.setMax(1);
            progressBar.setPrefHeight(30);
            root.getChildren().add(progressBar);


            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!progressBar.isValueChanging()) {
                    progressBar.setValue(newTime.toSeconds() / mediaPlayer.getTotalDuration().toSeconds());
                }
            });


            progressBar.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
                if (!isChanging) {
                    mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(progressBar.getValue()));
                }
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.dispose();
                if (tempFile.delete()) {
                    System.out.println("Archivo temporal eliminado.");
                } else {
                    System.err.println("Error al eliminar el archivo temporal.");
                }
            });

            Scene scene = new Scene(root);
            fxPanel.setScene(scene);


            root.widthProperty().addListener((obs, oldWidth, newWidth) -> mediaView.setFitWidth(newWidth.doubleValue()));
            videoPane.heightProperty().addListener((obs, oldHeight, newHeight) -> mediaView.setFitHeight(newHeight.doubleValue()));
        });
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VideoPlayer player = new VideoPlayer();
            player.setVisible(true);
        });
    }
}
