package com.example.nameless.autoupdating;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Created by nameless on 13.05.18.
 */

public class UDPClient {

    private DatagramSocket udpSocket;
    private InetAddress serverAddress;
    private int port;

    public UDPClient(InetAddress destinationAddr, int port) throws UnknownHostException, SocketException {
        this.serverAddress = destinationAddr;
        this.port = port;
        udpSocket = new DatagramSocket();
    }

    public DatagramSocket getUdpSocket() {
        return udpSocket;
    }

    public int createPrivateStream(String msg) throws IOException {
        DatagramPacket dp = new DatagramPacket(msg.getBytes(), msg.getBytes().length, serverAddress, port);
        this.udpSocket.send(dp);

        byte[] buf = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        udpSocket.receive(packet);
        int port = Integer.parseInt(new String(packet.getData()).trim());
        return port;
    }

    public void closeStream() {
        udpSocket.close();
    }

}
