package com.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import video.Video;
import video.VideoServiceGrpc;

import java.io.FileOutputStream;
import java.io.IOException;

public class DownloadVideoClient implements Runnable {

    private String tempVideoPath;
    private String uidVideoToDownload;
    private ManagedChannel channel;
    private VideoServiceGrpc.VideoServiceStub stub;

    DownloadVideoClient(String tempVideoPath, String uidVideoToDownload){
        this.tempVideoPath = tempVideoPath;
        this.uidVideoToDownload = uidVideoToDownload;
    }

    public void run() {
        channel = ManagedChannelBuilder
                .forAddress("localhost", 55555).usePlaintext().build();
        stub = VideoServiceGrpc.newStub(channel);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(tempVideoPath);
            FileOutputStream finalFileOutputStream = fileOutputStream;

            stub.getVideo(Video.VideoRequest.newBuilder()
                    .setUid(uidVideoToDownload).build(), new StreamObserver<Video.VideoChunk>() {
                @Override
                public void onNext(Video.VideoChunk chunk) {
                    try {
                        finalFileOutputStream.write(chunk.getChunk().toByteArray());
                    } catch (IOException e) {
                        System.err.println("Error al escribir en el archivo: " + e.getMessage());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("Error en el streaming: " + t.getMessage());
                    t.printStackTrace();
                    try {
                        finalFileOutputStream.close();
                    } catch (IOException e) {
                        System.err.println("Error al cerrar el archivo tras un fallo: " + e.getMessage());
                    }
                    channel.shutdown();
                }

                @Override
                public void onCompleted() {
                    System.out.println("Video descargado completamente.");
                    try {
                        finalFileOutputStream.close();
                    } catch (IOException e) {
                        System.err.println("Error al cerrar el archivo: " + e.getMessage());
                    }
                    channel.shutdown();
                }
            });
        } catch (IOException e) {
            System.err.println("Error al crear el archivo temporal: " + e.getMessage());
        }
    }


}
