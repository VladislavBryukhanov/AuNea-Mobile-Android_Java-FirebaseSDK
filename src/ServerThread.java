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
        udpSocket.setSoTimeout(1500);

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                ctc.getFirstUserIP(), ctc.getFirstUserPort());
        udpSocket.send(sendPacket);

        sendPacket = new DatagramPacket(sendData, sendData.length,
                ctc.getSecondUserIP(), ctc.getSecondUserPort());
        udpSocket.send(sendPacket);

        byte[] buf = new byte[320];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        while(true) {
            udpSocket.receive(packet);
            if(packet.getAddress().equals(ctc.getSecondUserIP())) {
                packet = new DatagramPacket(packet.getData(), packet.getData().length,
                        ctc.getFirstUserIP(), ctc.getFirstUserPort());
                udpSocket.send(packet);
            } else if (packet.getAddress().equals(ctc.getFirstUserIP())) {
                packet = new DatagramPacket(packet.getData(), packet.getData().length,
                        ctc.getSecondUserIP(), ctc.getSecondUserPort());
                udpSocket.send(packet);
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

