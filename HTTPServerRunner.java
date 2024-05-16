 public class HTTPServerRunner {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java HTTPServerRunner <bindAddress> <bindPort>");
            System.exit(1);
        }

        String bindAddress = args[0];
        int bindPort = Integer.parseInt(args[1]);

        SimpleNIOHTTPServer simpleNioHttpServer = new SimpleNIOHTTPServer(bindAddress, bindPort);
        simpleNioHttpServer.run();
    }
}
