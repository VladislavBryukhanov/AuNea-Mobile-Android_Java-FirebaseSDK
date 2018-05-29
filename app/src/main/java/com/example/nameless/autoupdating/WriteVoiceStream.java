package com.example.nameless.autoupdating;

import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.nameless.autoupdating.UserList;
import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by nameless on 21.05.18.
 */

public class WriteVoiceStream {
    //                    private int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private int minBufSize = 320;
    private byte[] buffer = new byte[minBufSize];
    private DatagramSocket socket;
    private AudioRecord recorder;
    private int sampleRate = 8000 ; // 44100 for music
    //    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
    private boolean status = true;

    private int port;

    public WriteVoiceStream(int port) {
        this.port = port;

        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void start() {

        status = true;
        startStreaming();
    }

    public void stop() {
        status = false;
        recorder.release();
        socket.close();
    }

    public void startStreaming() {
        try {
            DatagramPacket packet;
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,audioFormat,minBufSize*10);
            recorder.startRecording();

            while(status) {
//                        minBufSize = recorder.read(buffer, 0, buffer.length);
                recorder.read(buffer, 0, buffer.length);
                packet = new DatagramPacket (buffer, buffer.length, UserList.voiceStreamServerIpAddress, port);
                socket.send(packet);
            }

//                    writeToFile();
//                    playFromFile();
//                    playFromStream();

        } catch(UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToFile() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filepath+"/record.pcm");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while(status) {
            recorder.read(buffer, 0, buffer.length);
            try {
                os.write(buffer, 0, minBufSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void  playFromFile() {
        File file = new File(Environment.getExternalStorageDirectory().getPath()+"/record.pcm");
        byte[] buf = new byte[(int)file.length()];
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            bis.read(buf, 0, buf.length);
            bis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        AudioTrack audio = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                audioFormat,
                minBufSize*10,
                AudioTrack.MODE_STREAM );
        audio.play();

        while(status)
        {
            audio.write(buf, 0, buf.length);
        }
        audio.stop();
    }

    public void playFromStream() {
        AudioTrack audio = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                audioFormat,
                minBufSize*10,
                AudioTrack.MODE_STREAM );
        audio.play();
        while(status)
        {
            recorder.read(buffer, 0, buffer.length);
            audio.write(buffer, 0, buffer.length);
        }
        audio.stop();
    }
}
