
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.Iterator;
import java.io.IOException;

class SimpleNIOHTTPServer implements HTTPServerHandler {

    private String bindAddress;
    private int bindPort;
    private Path templatesPath = Paths.get("templates"); 
    private Path dbFilePath = Paths.get("db.txt"); 

    public SimpleNIOHTTPServer(String bindAddress, int bindPort) {
        this.bindAddress = bindAddress;
        this.bindPort = bindPort;
    }

    @Override
    public void run() {
        try {
            Selector selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(bindAddress, bindPort));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server is Active on " + bindAddress + ":" + bindPort);

            while (true) {
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    if (key.isAcceptable()) {
                        SocketChannel clientChannel = serverChannel.accept();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int bytesRead = clientChannel.read(buffer);
                        if (bytesRead > 0) {
                            buffer.flip();
                            String request = StandardCharsets.UTF_8.decode(buffer).toString();
                            handleRequest(clientChannel, request);
                        }
                    }
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRequest(SocketChannel clientChannel, String request) throws IOException {
        String[] lines = request.split("\r\n");
        if (lines.length > 0) {
            String[] requestLine = lines[0].split(" ");
            if (requestLine.length >= 2) {
                String method = requestLine[0];
                String path = requestLine[1];

                if ("GET".equalsIgnoreCase(method)) {
                    serveResource(clientChannel, path);
                } else if ("POST".equalsIgnoreCase(method)) {
                    if ("/submit".equals(path)) {
                        int contentLength = Integer.parseInt(getHeaderValue(lines, "Content-Length"));
                        String requestBody = lines[lines.length - 1];
                        String[] formData = requestBody.split("&");
                        String username = formData[0].split("=")[1];
                        String email = formData[1].split("=")[1];
                        writeFormData(username, email);
                        String response = "HTTP/1.1 200 OK\r\n\r\nInformation Submitted successfully";
                        clientChannel.write(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)));
                    }
                }
            }
        }
        clientChannel.close();
    }

    private void serveResource(SocketChannel clientChannel, String resourcePath) throws IOException {
        if (resourcePath.equals("/") || resourcePath.isEmpty()) {
            resourcePath = "/index.html"; 
            // serve index.html for root path
        }

        Path filePath = templatesPath.resolve(resourcePath.substring(1));
        byte[] content = Files.readAllBytes(filePath);

        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                              "Content-Length: " + content.length + "\r\n" +
                              "\r\n";
        clientChannel.write(ByteBuffer.wrap(httpResponse.getBytes(StandardCharsets.UTF_8)));
        clientChannel.write(ByteBuffer.wrap(content));
    }

    private void writeFormData(String username, String email) throws IOException {
        String entry = username + " " + email + "\n";
        Files.write(dbFilePath, entry.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private String getHeaderValue(String[] lines, String headerName) {
        for (String line : lines) {
            if (line.startsWith(headerName)) {
                return line.split(":")[1].trim();
            }
        }
        return null;
    }
}
