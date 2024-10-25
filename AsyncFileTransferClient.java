import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

public class AsyncFileTransferClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        try {
            String filePath = "path/to/your/file.txt";
            String fileName = Paths.get(filePath).getFileName().toString();

            AsynchronousSocketChannel clientChannel = AsynchronousSocketChannel.open();
            clientChannel.connect(new java.net.InetSocketAddress(SERVER_ADDRESS, SERVER_PORT)).get();

            ByteBuffer fileNameBuffer = ByteBuffer.wrap(fileName.getBytes());
            clientChannel.write(fileNameBuffer).get();

            try (FileInputStream fis = new FileInputStream(filePath)) {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                int bytesRead;
                while ((bytesRead = fis.read(buffer.array())) != -1) {
                    buffer.limit(bytesRead);
                    clientChannel.write(buffer).get();
                    buffer.clear();
                }
            }
            
            System.out.println("File sent successfully.");
            clientChannel.close();

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
