package com.tsc.statgui;

import jdk.jfr.StackTrace;

import java.io.IOException;
import  java.net.*;

public class TCPManager {
    private String host;
    private int port;
    private Socket socket;
    private boolean connected;

    public TCPManager(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.connected=false;
        this.connect();
    }

    public void connect() throws IOException {
            // открываем сокет и коннектимся
            // получаем сокет сервера
            this.socket = new Socket(host, port);
            this.connected = true;
        // вывод исключений
    }

    public void sendCommand(String cmd) throws IOException {
        if(this.socket.isClosed()) this.connect();
            this.socket.getOutputStream().write((cmd+"\r\n").getBytes());
            this.socket.getOutputStream().flush();

    }

    public String getAnswer() {
        try {
            int c;
            String data = "";
            do {
                c = this.socket.getInputStream().read();
                data+=(char)c;
            } while(this.socket.getInputStream().available()>0);
            this.socket.close();
            return data;
        }
        catch(Exception e)
        {System.out.println("get error: "+e);}

        return new String();
    }

    public boolean isConnected() {
        return connected;
    }
}

