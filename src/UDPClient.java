import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class UDPClient {

    private DatagramSocket udpSocket;
    private InetAddress serverAddress;
    private Scanner scanner;
    private int port;

    private UDPClient(){}

    private UDPClient(String destinationAddr, int port) throws UnknownHostException, SocketException {
        this.serverAddress = InetAddress.getByName(destinationAddr);
        this.port = port;
        udpSocket = new DatagramSocket();
        scanner = new Scanner(System.in);
    }

    private int start() throws IOException {
        String in;
        while(true) {
            in = scanner.nextLine();
            DatagramPacket dp = new DatagramPacket(in.getBytes(), in.getBytes().length, serverAddress, port);
            this.udpSocket.send(dp);
        }
    }

    public static void main(String[] args) {
        try {
            UDPClient client = new UDPClient("localhost", 2891);
            client.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
