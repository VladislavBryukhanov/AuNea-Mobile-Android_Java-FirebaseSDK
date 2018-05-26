import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class UDPServer {

    private DatagramSocket udpSocket;
    private int port;
//    private HashMap<ClientToClient, Boolean> dialogs;
    private HashMap<String, ClientToClient> dialogs;

    public UDPServer(int port) throws SocketException {
        this.port = port;
        this.udpSocket = new DatagramSocket(port);
        dialogs = new HashMap<>();
    }

    private void listen() throws IOException {
        System.out.println("Server running at " + InetAddress.getLocalHost() + ":" + port);
//        String msg;
        while(true) {
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            udpSocket.receive(packet);
//            ServerThread st = new ServerThread(udpSocket, packet);
//            st.start();
//            msg = new String(packet.getData()).trim();
//            System.out.println(msg);

            String json = new String(packet.getData()).trim();
            ClientToClient ctc = new Gson().fromJson(json, ClientToClient.class);
            if(dialogs.containsKey(ctc.getFirstUser()) || dialogs.containsKey(ctc.getSecondUser())) {
                String key;
                if(dialogs.containsKey(ctc.getFirstUser())) {
                    key = ctc.getFirstUser();
                } else {
                    key = ctc.getSecondUser();
                }
                ctc = dialogs.get(key);
                ctc.setSecondUserIP(packet.getAddress());
                ctc.setSecondUserPort(packet.getPort());
                ctc.setConnected(true);
//                dialogs.put(ctc.getFirstUser(), ctc);

/*                byte[] sendData = new Gson().toJson(ctc).getBytes();

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                        ctc.getFirstUserIP(), ctc.getFirstUserPort());
                udpSocket.send(sendPacket);
                sendPacket = new DatagramPacket(sendData, sendData.length,
                        ctc.getSecondUserIP(), ctc.getSecondUserPort());
                udpSocket.send(sendPacket);*/

                ServerThread st = new ServerThread(ctc);
                st.start();
                dialogs.remove(key);

            } else {
                ctc.setFirstUserIP(packet.getAddress());
                ctc.setFirstUserPort(packet.getPort());
                dialogs.put(ctc.getFirstUser(), ctc);
                ctc.setConnected(false);
            }
        }
    }
    public static void main(String[] args) {
        try {
            UDPServer serv = new UDPServer(2891);
            serv.listen();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
