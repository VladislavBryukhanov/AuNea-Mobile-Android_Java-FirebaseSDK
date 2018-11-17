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

    public UDPClient(InetAddress destinationAddr, int port) {
        this.serverAddress = destinationAddr;
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

    public int createPrivateStream(String msg) throws IOException {
        udpSocket.setSoTimeout(5000);
        DatagramPacket dp = new DatagramPacket(msg.getBytes(), msg.getBytes().length, serverAddress, port);
        this.udpSocket.send(dp);
        byte[] buf = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        udpSocket.receive(packet);
        closeStream();
        int port = Integer.parseInt(new String(packet.getData()).trim());
        return port;
    }

    public void connectToPrivateStream(String msg) throws IOException {
        udpSocket.setSoTimeout(15000);
        DatagramPacket dp = new DatagramPacket(msg.getBytes(), msg.getBytes().length, serverAddress, port);
        this.udpSocket.send(dp);
        byte[] buf = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        udpSocket.receive(packet);
    }
    public void closeStream() {
        udpSocket.close();
    }

}
