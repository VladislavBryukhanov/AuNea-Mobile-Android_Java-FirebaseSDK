package com.example.nameless.autoupdating.voip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by nameless on 13.05.18.
 */

public class UDPClient {

    private DatagramSocket udpSocket;
    private InetAddress serverAddress;
    private int port;
    private Runnable rejectCallback;

    public UDPClient(InetAddress destinationAddr, int port, Runnable rejectCallback) {
        this.serverAddress = destinationAddr;
        this.rejectCallback = rejectCallback;
        this.port = port;
        try {
            udpSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public DatagramSocket getUdpSocket() {
        return udpSocket;
    }

    public int createPrivateStream(String msg) {
        try {
            udpSocket.setSoTimeout(5000);
            DatagramPacket dp = new DatagramPacket(msg.getBytes(), msg.getBytes().length, serverAddress, port);
            this.udpSocket.send(dp);
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            udpSocket.receive(packet);
            int port = Integer.parseInt(new String(packet.getData()).trim());
            return port;
        } catch (IOException e) {
            e.printStackTrace();
            rejectCallback.run();
            return -1;
        }
    }

    public void closeStream() {
        udpSocket.close();
    }

}
