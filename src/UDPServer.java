import java.io.IOException;
import java.net.*;

public class UDPServer {

    public static DatagramSocket udpSocket;
    private final int port = 2891;

    public UDPServer() throws SocketException {
        udpSocket = new DatagramSocket(port);
    }

    private void listen() throws IOException {
        System.out.println("Server running at " + InetAddress.getLocalHost() + ":" + port);
        while(true) {
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            udpSocket.receive(packet);

            ClientToClient ctc = new ClientToClient(
                    packet.getAddress(),
                    packet.getPort());
            ServerThread st = new ServerThread(ctc);
            st.start();
        }
    }

    public static void main(String[] args) {
        try {
            UDPServer serv = new UDPServer();
            serv.listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
