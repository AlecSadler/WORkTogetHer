// PROGRAMMA CLIENT ESEGUITO DA UN UTENTE PER LAVORARE SUL SISTEMA

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;


public class ClientApp extends RemoteObject implements ClientNotifyINT {

    private String username;
      boolean loggedIn;

    private SocketChannel socCh;
    private HashMap<String, String> usersCallback;
    private final HashMap<String, String> projectMulticast;

    private final ArrayList<String> messageQueue;
    private final ArrayList<Thread> chatThreads;

    public ClientApp() {
        super();
        usersCallback = null;
        username = "init";
        loggedIn = false;
        projectMulticast = new HashMap<>();
        messageQueue = new ArrayList<>();
        chatThreads = new ArrayList<>();
    }

    public String getUsername() {return username;}

    // implementazione servizi RMI
    @Override
    public void notifyUsers(HashMap<String, String> users) {
        usersCallback = users;
    }

    @Override
    public void notifySockets(HashMap<String, String> sockets) {
        if (sockets==null) return;
        for (String project : sockets.keySet()) {
            if (!projectMulticast.containsKey(project)) {
                Thread listener = new Thread(new ChatListenerTH(this, project, sockets.get(project)));
                chatThreads.add(listener);
                projectMulticast.put(project, sockets.get(project));
                listener.start();
            }
        }
    }

    // trasmette le richieste e riceve le risposte codifica/decodifica i flussi di bytes
    private String serverComunica(String command) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(2048);
        buf.clear();
        buf.put(command.getBytes(StandardCharsets.UTF_8));
        buf.flip();
        socCh.write(buf);
        ByteBuffer bufRec = ByteBuffer.allocate(2048);
        socCh.read(bufRec);
        bufRec.flip();
        return StandardCharsets.UTF_8.decode(bufRec).toString();
    }

    // instaura la comunicazione col server e invia i comandi
    public void start() {

        try {
            // INIZIALIZZAZIONE SERVIZI RMI
            Registry registry;
            RMIServicesINT stub;
            ClientNotifyINT callbackObj;
            ClientNotifyINT stubCallback;
            try {
                registry = LocateRegistry.getRegistry(Utils.RMIPort);
                stub = (RMIServicesINT) registry.lookup("RMI_SERVICES");
                callbackObj = this;
                stubCallback = (ClientNotifyINT) UnicastRemoteObject.exportObject(callbackObj, 0);
            } catch (NotBoundException e) {
                e.printStackTrace();
                return;
            }

            // INIZIALIZZAZIONE CONNESSIONE TCP
            try {
                socCh = SocketChannel.open();
                socCh.connect(new InetSocketAddress(Utils.ipAddress, Utils.TCPPort));
            } catch (ClosedChannelException e) {
                e.printStackTrace();
                return;
            }

            Scanner scanner = new Scanner(System.in);
            //boolean loggedIn = false;
            boolean end = false;
            System.out.println(Utils.yellowtext + ">>> BENVENUTO IN WOR(k)T(oget)H(er) <<<\n" + Utils.resetcolor);
            Utils.showHelp();
            System.out.println();
            while (!end) {
                System.out.print(Utils.bluetext + ">> " + Utils.resetcolor);
                String op = scanner.nextLine() + " " + username;
                String[] cmd = op.split(" ",2); // isolo il comando
                if (cmd.length == 0) {
                    System.out.println("Nessun comando rilevato.\n");
                    continue;
                }

                switch (cmd[0].toLowerCase()) {

                    case "register": {    // register nome passw nome
                        if (loggedIn) {
                            System.out.println("Sei già online!");
                            break;
                        }
                        String[] aux = cmd[1].split(" ", 3); // la stringa dopo il comando
                        if (aux.length != 3) {
                            System.out.println(Utils.cmdERR);
                            break;
                        }
                        if (!stub.register(aux[0], aux[1])) {
                            System.out.println("Nickname già utilizzato.");
                        } else {
                            System.out.println(aux[0] + Utils.greentext + " registrato correttamente, benvenuto in Worth!" + Utils.resetcolor);
                        }
                        break;
                    }

                    case "login": {       // login   nome passw nome
                        if (loggedIn) {
                            System.out.println("Sei già online!");
                            break;
                        }
                        String[] aux = cmd[1].split(" ", 3);
                        if (aux.length != 3) {
                            System.out.println(Utils.cmdERR);
                            break;
                        }

                        stub.cbSubscribe(stubCallback, aux[0]);
                        String resp = serverComunica(cmd[0].toLowerCase() + " " + aux[0] + " " + aux[1]);
                        System.out.println(resp);
                        if (!resp.contains("errati")) {
                            loggedIn = true;
                            username = aux[0];
                        } else {
                            stub.cbUnsubscribe(aux[0]);
                        }
                        break;
                    }

                    case "logout": {
                        if (!loggedIn) {     // logout nome nome
                            System.out.println("Non sei connesso con nessun account.");
                            break;
                        }
                        String[] aux = cmd[1].split(" ", 2);
                        if (aux.length != 2) {
                            System.out.println(Utils.cmdERR);
                            break;
                        }
                        // per evitare che un client possa disconnettere un account diverso
                        if (!username.equals(aux[0])) {
                            System.out.println("Non puoi disocnnettere un altro account.");
                            break;
                        }
                        String resp = serverComunica(cmd[0].toLowerCase() + " " + aux[1]);
                        System.out.println(resp);
                        if (!resp.contains("sconosciuto")) {
                            loggedIn = false;
                            stub.cbUnsubscribe(aux[0]);
                            for (Thread thread : chatThreads) {
                                thread.interrupt();
                            }
                            chatThreads.clear();
                            synchronized (messageQueue) {
                                messageQueue.clear();
                            }
                            projectMulticast.clear();
                        }
                        break;
                    }

                    case "listusers": {
                        if (!loggedIn) {
                            System.out.println(Utils.loginERR);
                            break;
                        }
                        if (usersCallback == null) {
                            System.out.println("Nessun dato disponibile.");
                            break;
                        }
                        for (String key : usersCallback.keySet()) {
                            if (usersCallback.get(key).equals("online"))
                                System.out.println(key + " - " + Utils.greentext + usersCallback.get(key) + Utils.resetcolor);
                            else
                                System.out.println(key + " - " + Utils.redtext + usersCallback.get(key) + Utils.resetcolor);
                        }
                        break;
                    }

                    case "listonlineusers": {
                        if (!loggedIn) {
                            System.out.println(Utils.loginERR);
                            break;
                        }
                        if (usersCallback == null) {
                            System.out.println("Nessun dato disponibile.");
                            break;
                        }
                        for (String key : usersCallback.keySet()) {
                            if (usersCallback.get(key).equals("online")) {
                                System.out.println(key + " - " + Utils.greentext + usersCallback.get(key) + Utils.resetcolor);
                            }
                        }
                        break;
                    }

                    case "readchat": {   // readchat prog name
                        if (!loggedIn) {
                            System.out.println(Utils.loginERR);
                            break;
                        }
                        String[] aux = cmd[1].split(" ", 2);
                        if (aux.length != 2) {
                            System.out.println(Utils.cmdERR);
                            break;
                        }
                        String address = projectMulticast.get(aux[0]);
                        if (address == null) {
                            System.out.println(Utils.genericERR);
                            break;
                        }
                        synchronized (messageQueue) {
                            int letti = 0;
                            ArrayList<String> readMsgs = new ArrayList<>();
                            for (String message : messageQueue) {
                                if (message.contains("->" + aux[0] + "<-") && !message.contains(username + " ha scritto: ")) {
                                    String[] text = message.split("->");
                                    System.out.println(text[0]);
                                    letti++;
                                    readMsgs.add(message);
                                }
                            }
                            if (!readMsgs.isEmpty()) {
                                for (int i = 0; i < letti; ++i) {
                                    messageQueue.remove(readMsgs.get(i));
                                }
                            } else System.out.println("Non ci sono messaggi da prelevare.");
                        }
                        break;
                    }

                    case "sendchatmsg": {  // sendmsg prog messg user
                        if (!loggedIn) {
                            System.out.println(Utils.loginERR);
                            break;
                        }
                        String[] aux = cmd[1].split(" ", 3);
                        if (aux.length != 3) {
                            System.out.println(Utils.cmdERR);
                            break;
                        }
                        String address = projectMulticast.get(aux[0]);
                        if (address == null) {
                            System.out.println(Utils.genericERR);
                            break;
                        }

                        String message = username + " ha scritto: " + aux[1].replace("-", " ");
                        try {
                            DatagramSocket socket = new DatagramSocket();
                            InetAddress ip = InetAddress.getByName(address);
                            byte[] tosend = message.getBytes();
                            DatagramPacket packet = new DatagramPacket(tosend, tosend.length, ip, Utils.UDPListenPort);
                            socket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                            break;
                        }
                        break;
                    }

                    case "cancelproject": {    // cancelproject prog name
                        if (!loggedIn) {
                            System.out.println(Utils.loginERR);
                            break;
                        }
                        String[] aux = cmd[1].split(" ", 2);
                        if (aux.length != 2) {
                            System.out.println(Utils.cmdERR);
                            break;
                        }
                        String resp = serverComunica(cmd[0].toLowerCase() + " " + aux[0] + " " + aux[1]);
                        if (resp.equals("")) throw new IOException();
                        if (resp.contains("eliminato")) projectMulticast.remove(cmd[1]);
                        System.out.println(resp);
                        break;
                    }

                    case "addcard":  {  // addcard prog nome desc user
                        if (!loggedIn) {
                            System.out.println(Utils.loginERR);
                            break;
                        }
                        String[] aux = cmd[1].split(" ", 4);
                        if (aux.length != 4) {
                            System.out.println(Utils.cmdERR);
                            break;
                        }
                        String resp = serverComunica(cmd[0].toLowerCase() + " " + aux[0] + " " + aux[1] + " " + aux[2] + " " + aux[3]);
                        if (resp.equals("")) throw new IOException();
                        System.out.println(resp);
                        // se l'aggiunta va a buon fine lo notifico in chat
                        if (resp.contains("aggiunta")) {
                            String address = projectMulticast.get(aux[0]);
                            if (address == null) {
                                System.out.println(Utils.genericERR);
                                break;
                            }
                            String message = username + " ha scritto: " + "Ho aggiunto la card " + aux[1] + " al progetto " + aux[0];
                            try {
                                DatagramSocket socket = new DatagramSocket();
                                InetAddress ip = InetAddress.getByName(address);
                                byte[] tosend = message.getBytes();
                                DatagramPacket packet = new DatagramPacket(tosend, tosend.length, ip, Utils.UDPListenPort);
                                socket.send(packet);
                            } catch (IOException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        break;
                    }

                    // ricarica la schermata di avvio con i comandi
                    case "help": {
                        Utils.showHelp();
                        break;
                    }

                    case "exit": {
                        if (loggedIn) {
                            System.out.println("Per terminare il client è necessario effettuare il logout.");
                        }
                        else {
                            serverComunica(cmd[0].toLowerCase() + " " + cmd[1]);
                            end = true;
                        }
                        break;
                    }

                    default: {
                        if (!loggedIn) {
                            System.out.println(Utils.loginERR);
                            break;
                        }
                        String resp = serverComunica(cmd[0].toLowerCase() + " " + cmd[1]);
                        if (resp.equals("")) throw new IOException();
                        System.out.println(resp);
                        break;
                    }
                }
            }
            System.out.println(Utils.greentext + "Client arrestato." + Utils.resetcolor);
            System.exit(0);

        } catch (ConnectException e) {
            System.out.println(Utils.redtext + "Server irraggiungibile, arresto in corso" + Utils.resetcolor);
            System.exit(0);
        } catch (RemoteException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IOException e) {
            System.out.println(Utils.redtext + "Server irraggiungibile, arresto in corso" + Utils.resetcolor);
            System.exit(0);
        }
    }

    // aggiunge un messaggio alla coda da leggere
    public void putNewMsg(String message) {
        synchronized (messageQueue) {
            messageQueue.add(message);
        }
    }

    public static void main(String[] args) {
        ClientApp client = new ClientApp();
        client.start();
    }

}
