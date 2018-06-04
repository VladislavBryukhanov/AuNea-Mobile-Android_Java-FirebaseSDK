import com.google.gson.Gson;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

public class ServerThread extends Thread {
    private DatagramSocket udpSocket;
    private ClientToClient ctc;

    public ServerThread(ClientToClient ctc) {
        this.ctc = ctc;
    }


    public void run() {
        try {
        udpSocket = new DatagramSocket(0);
        int port = udpSocket.getLocalPort();
        byte[] sendData = String.valueOf(port).getBytes();
        udpSocket.setSoTimeout(3000);

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                ctc.getFirstUserIP(), ctc.getFirstUserPort());
        UDPServer.udpSocket.send(sendPacket);

        sendPacket = new DatagramPacket(sendData, sendData.length,
                ctc.getSecondUserIP(), ctc.getSecondUserPort());
        UDPServer.udpSocket.send(sendPacket);

//        byte[] buf = new byte[320];
        byte[] buf = new byte[20];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        int prevPort = 0; //если с 1 ip через разные порты идет звонок
        if((ctc.getFirstUserIP().getHostAddress()).equals(ctc.getSecondUserIP().getHostAddress())) {
             prevPort = -1;
        }

        while(true) {
            udpSocket.receive(packet);
            if(prevPort == -1) {
                prevPort = packet.getPort();
            }

            if(packet.getAddress().equals(ctc.getSecondUserIP())
                    && (prevPort == 0 || packet.getPort() != prevPort)) {
                packet = new DatagramPacket(packet.getData(), packet.getData().length,
                        ctc.getFirstUserIP(), ctc.getFirstUserPort());
                UDPServer.udpSocket.send(packet);
            } else if (packet.getAddress().equals(ctc.getFirstUserIP())) {
                packet = new DatagramPacket(packet.getData(), packet.getData().length,
                        ctc.getSecondUserIP(), ctc.getSecondUserPort());
                UDPServer.udpSocket.send(packet);
            }
        }

        } catch (SocketTimeoutException s) {
            udpSocket.close();
            this.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

