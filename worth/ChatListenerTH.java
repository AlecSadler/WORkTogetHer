// THREAD PER RICEVERE I MESSAGGI DI CHAT

import java.io.IOException;
import java.net.*;

public class ChatListenerTH extends Thread {

    private final ClientApp client;
    private final String project;
    private final String address;

    public ChatListenerTH(ClientApp cli, String prog, String ip) {
        client = cli;
        project = prog;
        address = ip;
    }

    public void run() {
        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket(Utils.UDPListenPort);
            socket.joinGroup(InetAddress.getByName(address));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        while (!currentThread().isInterrupted()) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                if (!currentThread().isInterrupted()) {
                    String message = new String(packet.getData())+" ->"+ project +"<-";
                    client.putNewMsg(message);
                }
            } catch (IOException e) {e.printStackTrace();}
        }
    }

}
