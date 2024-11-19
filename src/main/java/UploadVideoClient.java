import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import video.Video;
import video.VideoServiceGrpc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class UploadVideoClient {
    private final VideoServiceGrpc.VideoServiceStub asyncStub;
    private final Object lock = new Object();
    private boolean isCompleted = false;

    public UploadVideoClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        asyncStub = VideoServiceGrpc.newStub(channel);
    }

    public void uploadVideo(String uid, String filePath) throws IOException, InterruptedException{
        File videoFile = new File(filePath);
        System.out.println("reading file: "+videoFile.getName());
        FileInputStream fileInputStream = new FileInputStream(videoFile);
        StreamObserver<Video.VideoChunk> requestObserver =
                asyncStub.uploadVideo(new StreamObserver<>() {
                    @Override
                    public void onNext(Video.UploadStatus status) {
                        System.out.println("Server response: " + status.getMessage());
                    }

                    @Override
                    public void onError(Throwable t) {
                        synchronized (lock) {
                            isCompleted = true;
                            lock.notify();
                        }
                        System.err.println("Error during video upload: " + t);
                    }

                    @Override
                    public void onCompleted() {
                        synchronized (lock) {
                            isCompleted = true;
                            lock.notify();
                        }
                        System.out.println("Video upload completed successfully.");
                    }
                });

        byte[] buffer = new byte[4096];
        int bytesRead;
        System.out.println("Reading bytes..");
        try {
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                Video.VideoChunk chunk = Video.VideoChunk.newBuilder()
                        .setUid(uid)
                        .setChunk(ByteString.copyFrom(buffer, 0, bytesRead))
                        .build();
                requestObserver.onNext(chunk);
            }
            System.out.println("Bytes transfer end");
        }catch(Exception e){
            requestObserver.onError(e);
        }finally {
            requestObserver.onCompleted();
        }

        synchronized (lock) {
            while (!isCompleted) {
                lock.wait();
            }
        }

        System.out.println("Upload process finished.");

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String filePath = "C:\\Users\\sauld\\Videos\\InstalacionVagrant-Feb2024.mp4";
        UploadVideoClient client = new UploadVideoClient("localhost", 55555);
        String uid = "vagrantInstalation123456";
        client.uploadVideo(uid, filePath);
    }
}
