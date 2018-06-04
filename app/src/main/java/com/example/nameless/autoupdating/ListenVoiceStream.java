package com.example.nameless.autoupdating;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

/**
 * Created by nameless on 21.05.18.
 */

public class ListenVoiceStream {

    private boolean status = true;
    private UDPClient client;

    protected ListenVoiceStream(UDPClient client) {
        this.client = client;
    }

    public void start() {
        try {

//            int minBufSize = 320;
            int minBufSize = 20;
            byte[] buf = new byte[minBufSize];

            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            DatagramSocket udpSocket = client.getUdpSocket();
            udpSocket.setSoTimeout(3000);


            AudioTrack audio = new AudioTrack(AudioManager.STREAM_MUSIC,
                    8000,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_8BIT,
                    minBufSize*10,
                    AudioTrack.MODE_STREAM );

            while(status) {
                udpSocket.receive(packet);
                audio.play();
                audio.write(buf, 0, buf.length);
            }
            audio.stop();
            udpSocket.close();

        } catch (SocketTimeoutException s) {
            stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        status = false;
    }

}
