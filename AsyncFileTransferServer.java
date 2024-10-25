import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsyncFileTransferServer {
    private static final int PORT = 5000;
    private static final String SAVE_DIR = "received_files/";
    private static final int THREAD_POOL_SIZE = 10; 

    public static void main(String[] args) {
        try {
            AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(
                    THREAD_POOL_SIZE,
                    Executors.defaultThreadFactory()
            );

            AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel
                    .open(channelGroup)
                    .bind(new java.net.InetSocketAddress(PORT));

            System.out.println("Server is listening on port " + PORT);

            serverSocketChannel.accept(null, new java.nio.channels.CompletionHandler<AsynchronousSocketChannel, Void>() {
                @Override
                public void completed(AsynchronousSocketChannel clientChannel, Void attachment) {
                    serverSocketChannel.accept(null, this); 

                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    try {
                        clientChannel.read(buffer).get();
                        buffer.flip();
                        String fileName = new String(buffer.array(), buffer.position(), buffer.limit());
                        buffer.clear();

                        System.out.println("Receiving file: " + fileName);
                        
                        try (FileOutputStream fos = new FileOutputStream(SAVE_DIR + fileName)) {
                            clientChannel.read(buffer, null, new java.nio.channels.CompletionHandler<Integer, Void>() {
                                @Override
                                public void completed(Integer bytesRead, Void attachment) {
                                    if (bytesRead == -1) {
                                        try {
                                            clientChannel.close();
                                            System.out.println("File received successfully: " + fileName);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        return;
                                    }

                                    buffer.flip();
                                    try {
                                        fos.write(buffer.array(), 0, buffer.limit());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    buffer.clear();
                                    clientChannel.read(buffer, null, this); 
                                }

                                @Override
                                public void failed(Throwable exc, Void attachment) {
                                    exc.printStackTrace();
                                    try {
                                        clientChannel.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    exc.printStackTrace();
                }
            });
            channelGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
