// IMPLEMENTA TUTTE LE FUNZIONI DEL SERVER ( RMI, UDP E TCP)

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.*;

public class WorthServer extends RemoteServer implements RMIServicesINT {

    private ArrayList<Project> projects;
    private ArrayList<User> users;

    private HashMap<String, String> callbacks;
    private final HashMap<String, ClientNotifyINT> clients;

    private int lastIP; // tiene il conto degli indirizzi assegnati e lo usa come offset per la generazione di nuovi

    private final File dbPath;
    private final File usersFile;
    private final File projectListFile;
    private final File ipFile;


    public WorthServer() {
        super();
        clients = new HashMap<>();
        callbacks = new HashMap<>();
        lastIP = -1; // in modo che col primo incremento nella funzione va a 0
        dbPath = new File(Utils.DbPath);
        usersFile = new File(Utils.usersPath);
        projectListFile = new File(Utils.projectsPath);
        ipFile = new File(Utils.lastipPath);   // memorizza il valore da dare a lastIP su un file di testo
    }

    @Override
    public synchronized boolean register(String username, String password) throws RemoteException {
        for (User user : users) {
            if (user.getNickname().equals(username)) return false;
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Writer wr;
        User user = new User(username, password);
        users.add(user);
        user.setStatus("offline");
        callbacks.put(username, "offline");
        updateUsersStuatus(username);
        try {
            wr = new FileWriter(usersFile);
            gson.toJson(users, wr);
            wr.flush();
            wr.close();
        } catch (IOException e) {e.printStackTrace();}
        return true;
    }

    @Override
    public synchronized void cbSubscribe(ClientNotifyINT clientInterface, String user) {
        if (!clients.containsValue(clientInterface)) clients.put(user, clientInterface);
    }

    @Override
    public synchronized void cbUnsubscribe(String user) {
        clients.remove(user);
    }

    @Override
    public synchronized void notifyAddress(String user) throws RemoteException{
        HashMap<String, String> sockets = new HashMap<>();
        ClientNotifyINT cli = clients.get(user);
        if (projects != null) {
            for (Project p: projects){
                if (p.isMember(user)) sockets.put(p.getName(), p.getAddress());
            }
        }
        if (cli != null) cli.notifySockets(sockets);
    }

    public void updateUsersStuatus(String username) throws RemoteException {
        sendCBKS(username);
    }

    private synchronized void sendCBKS(String username) throws RemoteException {
        ClientNotifyINT toRemove = null;
        for (ClientNotifyINT client : clients.values()) {
            try {
                client.notifyUsers(callbacks);
                if (projects==null) client.notifySockets(null);

            } catch (ConnectException e) {toRemove=client;}
        }
        if (toRemove!=null) cbUnsubscribe(username);
    }

    public void start() {
        if (!loadData()) {    // carico il database
            System.out.println(Utils.redtext + "Errore nel caricamento del database!" + Utils.resetcolor);
            return;
        }
        ServerSocketChannel serverSocketChannel;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(Utils.TCPPort));
            serverSocketChannel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Selector selector;
        try {
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        System.out.println(Utils.greentext + "Server READY" + Utils.resetcolor);

        while (true) {
            try {
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                try {
                    // ACCETTA CONNESSIONI DA CLIENT
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                    }

                    // LEGGE I COMANDI DAL CLIENT
                    else if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(2048);
                        client.read(buffer);
                        buffer.flip();
                        String request = StandardCharsets.UTF_8.decode(buffer).toString();
                        String[] command = request.split(" ", 2);

                        if (command[0].equals("exit")) {
                            key.channel().close();
                            key.cancel();
                            continue;
                        }
                        String toClient = cmdHandler(command);
                        ByteBuffer answer = ByteBuffer.allocate(2048);
                        answer.put(toClient.getBytes());
                        answer.flip();
                        key.interestOps(SelectionKey.OP_WRITE);
                        key.attach(answer);
                    }

                    // MANDA LE RISPOSTE AL CLIENT
                    else if (key.isWritable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer response = (ByteBuffer) key.attachment();
                        client.write(response);
                        key.interestOps(SelectionKey.OP_READ);
                        key.attach(null);
                    }
                } catch (IOException e) {
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException exc) {
                        exc.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean loadData() {
        if (dbPath.exists()) {
            // SE ESISTONO DATI SALVATI LI RECUPERO
            Gson gson = new Gson();
            BufferedReader buf;
            try {
                if (usersFile.length()!=0) {
                    users = new ArrayList<>();
                    buf = new BufferedReader(new FileReader(usersFile));
                    Type t = new TypeToken<ArrayList<User>>() {}.getType();
                    users = gson.fromJson(buf, t);
                }
                else users = new ArrayList<>();
                if (projectListFile.length()!=0) {
                    projects = new ArrayList<>();
                    buf = new BufferedReader(new FileReader(projectListFile));
                    Type t = new TypeToken<ArrayList<Project>>() {}.getType();
                    projects = gson.fromJson(buf, t);
                }
                if (users!=null) {
                    for (User user : users) {
                        callbacks.put(user.getNickname(), "offline");
                    }
                }
                if (ipFile != null) {
                    BufferedReader br = new BufferedReader(new FileReader(ipFile));
                    lastIP = br.read();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        else {
            if (!dbPath.mkdir()) return false;
            try {
                usersFile.createNewFile();
                projectListFile.createNewFile();
            } catch (IOException e) {return false;}
            users = new ArrayList<>();
            projects = new ArrayList<>();
        }
        return true;
    }

    // funzione che genera gli indirizzi multicast per le chat
    private String generaIP() {
        // range 224.0.0.0 -> 239.255.255.255
        lastIP++;
        BufferedWriter wr = null;
        try {
            wr = new BufferedWriter(new FileWriter(ipFile));
            wr.write(lastIP);
            wr.flush();
            wr.close();
        } catch (IOException e) {e.printStackTrace();}
        String ip = Utils.multicastBase;
        if (lastIP < 256){      // da 224.0.0.0 -> 224.0.0.255 e così via...
            ip = Utils.multicastBase.substring(0,8) + lastIP;
        }
        else if (lastIP < 65536){
            int byte3 = (int) lastIP /256;
            int byte4 = lastIP -byte3*256;
            ip = Utils.multicastBase.substring(0,6) + byte3 + "." + byte4;
        }
        else if (lastIP < 16777216){
            int byte2 = (int) lastIP /65536;
            int byte3 = lastIP - byte2*65536;
            byte3 = byte3/256;
            int byte4 = lastIP - byte2*65536 - byte3*256;
            ip = Utils.multicastBase.substring(0,4) + byte2 + "." + byte3 + "." + byte4;
        }
        else if (lastIP < 268435456){
            int tmp = (int) lastIP /16777216;
            int byte1 = tmp + 224;
            int byte2 = lastIP - tmp * 16777216;
            byte2 = byte2/65536;
            int byte3 = lastIP - tmp*16777216 - byte2*65536;
            byte3 = byte3/256;
            int byte4 = lastIP - tmp*16777216 - byte2*65536 - byte3*256;
            ip = byte1 + "." + byte2 + "." + byte3 + "." + byte4;
        }
        return ip;
    }

    private String cmdHandler(String[] cmd) {  // gli arriva la richiesta splittata in comando + args
        switch (cmd[0]) {
            case "login": {
                String aux[] = cmd[1].split(" ", 3);
                String username = aux[0];
                String password = aux[1];
                for (User user : users) {
                    if (user.authenthication(username, password) && !(user.getStatus())) {
                        user.setStatus("online");
                        callbacks.replace(username, "online");
                        try {
                            updateUsersStuatus(aux[0]);
                            notifyAddress(aux[0]);
                        } catch (RemoteException e) {e.printStackTrace();}
                        return Utils.greentext + "Accesso effettuato! Bentornato " + aux[0] + Utils.resetcolor;
                    }
                }
                return "Password e/o nickname errati oppure utente gia connesso da un'altra postazione!";
            }

            case "logout": {
                String[] aux = cmd[1].split(" ",2);
                String username = aux[0];
                for (User user : users) {
                    if (username.equals(user.getNickname())) {
                        user.setStatus("offline");
                        callbacks.replace(username, "offline");
                        try {
                            updateUsersStuatus(aux[0]);
                        } catch (RemoteException e) {e.printStackTrace();}
                        return Utils.greentext + "Disconnessione effettuata, arrivederci "+ aux[0] + Utils.resetcolor;
                    }
                }
                return Utils.userERR;
            }

            case "listprojects": {   // listprojects nome
                if (projects==null) return "L'utente non partecipa a nessun progetto.";
                String output = Utils.magentatext + "ELENCO PROGETTI: " + "\n" + Utils.resetcolor;
                for (Project project : projects) {
                    if (project.isMember(cmd[1])) output += project.getName() + "\n";
                }
                if (output.equals("ELENCO PROGETTI: ")) return "L'utente non partecipa a nessun progetto.";
                return output;
            }

            case "createproject": {    // createproject prog nome
                if (projects==null) projects = new ArrayList<>();
                String[] aux = cmd[1].split(" ",2);
                if (aux.length < 2) return Utils.cmdERR;
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Writer wr;
                for (Project project : projects) {
                    if (project.getName().equals(aux[0])) {
                        if (project.isMember(aux[1])) return "Fai già parte di questo progetto.";
                        else return "Esiste già un progetto con questo nome.";
                    }
                }
                String address = generaIP();
                Project project = new Project(aux[0], aux[1], address);
                projects.add(project);
                try {
                    notifyAddress(aux[1]);
                } catch (RemoteException e) {e.printStackTrace();}
                try {
                    wr = new FileWriter(projectListFile);
                    gson.toJson(projects, wr);
                    wr.flush();
                    wr.close();
                } catch (IOException e) {e.printStackTrace();}
                return "Progetto " + aux[0] + " creato correttamente!";
            }

            case "addmember": {   // addmember prog user nomemio
                if (projects==null) projects = new ArrayList<>();
                String[] aux = cmd[1].split(" ", 3);
                if (aux.length != 3) return Utils.cmdERR;
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Writer wr;
                for (Project project : projects) {
                    if (project.getName().equals(aux[0])) {
                        if (project.isMember(aux[2])) {
                            if (project.isMember((aux[1]))) {
                                return "Utente già tra i membri.";
                            }
                            else {
                                // controllo se l'utente che voglio aggiungere esiste
                                boolean found = false;
                                for (User user : users) {
                                    if (user.getNickname().equals(aux[1])) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) return Utils.userERR;
                                project.addMember(aux[1]);
                                try {
                                    notifyAddress(aux[1]);
                                } catch (RemoteException e) {e.printStackTrace();}
                                try {
                                    wr = new FileWriter(projectListFile);
                                    gson.toJson(projects, wr);
                                    wr.flush();
                                    wr.close();
                                } catch (IOException e) {e.printStackTrace();}
                                return "Utente " + aux[1] + " aggiunto al progetto " + aux[0];
                            }
                        }
                    }
                }
                return Utils.genericERR;
            }

            case "showmembers": {     // showmembers prog nome
                if (projects==null) {projects = new ArrayList<>();}
                String[] aux = cmd[1].split(" ", 2);
                if (cmd.length < 2) return Utils.cmdERR;
                for (Project project : projects) {
                    if (project.getName().equals(aux[0])) {
                        if (project.isMember(aux[1])) {
                            return Utils.magentatext + "PARTECIPANTI: " + Utils.resetcolor + project.getMembers().toString();
                        }
                    }
                }
                return Utils.genericERR;
            }

            case "showcards": {      // showcards prog nome
                if (projects==null) {projects = new ArrayList<>();}
                String[] aux = cmd[1].split(" ", 2);
                if (aux.length != 2) return Utils.cmdERR;
                for (Project project : projects) {
                    if (project.getName().equals(aux[0])) {
                        if (project.isMember(aux[1])) {
                            if (project.getAllCards()==null) {
                                return "Non sono ancora presenti card nel progetto, Usa addcard per aggiungerlne.";
                            }
                            String output = Utils.magentatext + "CARDS DEL PROGETTO:\n" + Utils.resetcolor;
                            ArrayList<Card> cards = project.getAllCards();
                            for (int i=0; i<cards.size(); ++i) {
                                Card card = cards.get(i);
                                output+= "Nome: " + card.getName() + " -- " + " Stato: " + card.getList() + "\n";
                            }
                            return output;
                        }
                    }
                }
                return Utils.genericERR;
            }

            case "showcard": {     // showcard prog card nome
                if (projects==null) {
                    projects = new ArrayList<>();
                }
                String[] aux = cmd[1].split(" ", 3);
                if(aux.length != 3) return Utils.cmdERR;
                for (Project project : projects) {
                    if (project.getName().equals(aux[0])) {
                        if (project.isMember(aux[2])) {
                            Card card = project.getCard(aux[1]);
                            if (card!=null) {
                                return Utils.magentatext + "DETTAGLIO CARD: " + Utils.resetcolor + "Nome: " + card.getName() + " -- Description: " + card.getDescription() + " -- Stato lavorazione: " + card.getList();
                            }
                            else return "La card non esiste!";
                        }
                    }
                }
                return Utils.genericERR;
            }

            case "addcard": {      // addcard prog card desc nome
                if (projects==null) { projects = new ArrayList<>();}
                String[] aux = cmd[1].split(" ", 4);
                if (aux.length != 4) return Utils.cmdERR;
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Writer wr;
                for (Project project : projects) {
                    if (project.getName().equals(aux[0])) {
                        if (project.isMember(aux[3])) {
                            if (!project.addCard(aux[1], aux[2].replace("-", " "))) {
                                return "Esiste già una card con questo nome!";
                            }
                            else {
                                try {
                                    wr = new FileWriter(projectListFile);
                                    gson.toJson(projects, wr);
                                    wr.flush();
                                    wr.close();
                                } catch (IOException e) {e.printStackTrace();}
                                return "Card " + aux[1] + " aggiunta al progetto " + aux[0];
                            }
                        }
                    }
                }
                return Utils.genericERR;
            }

            case "movecard": {    // movecard prog card old new name
                if (projects==null) {projects = new ArrayList<>();}
                String[] aux = cmd[1].split(" ", 5);
                if (aux.length != 5) return Utils.cmdERR;
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Writer wr;
                for (Project project : projects) {
                    if (project.getName().equals(aux[0])) {
                        if (project.isMember(aux[4])) {
                            if (!project.moveCard(aux[1], aux[2], aux[3])) return Utils.genericERR;
                            else {
                                try {
                                    wr = new FileWriter(projectListFile);
                                    gson.toJson(projects, wr);
                                    wr.flush();
                                    wr.close();
                                } catch (IOException e) {e.printStackTrace();}
                                return "Card " + aux[1] + " spostata correttamente da " + aux[2] + " a " + aux[3];
                            }
                        }
                    }
                }
                return Utils.genericERR;
            }

            case "getcardhistory": {  // gch prog card nome
                if (projects==null) projects = new ArrayList<>();
                String[] aux = cmd[1].split(" ", 3);
                if (aux.length != 3) return Utils.cmdERR;
                for (Project project : projects) {
                    if (project.getName().equals(aux[0])) {
                        if (project.isMember(aux[2])) {
                            Card card = project.getCard(aux[1]);
                            if (card!=null) {
                                return Utils.magentatext + "STORICO LISTE: " + Utils.resetcolor + card.getHistory().toString();
                            }
                            else return "Card non trovata.";
                        }
                    }
                }
                return Utils.genericERR;
            }

            case "cancelproject": {   // cancel prog nome
                if (projects == null) projects = new ArrayList<>();
                String[] aux = cmd[1].split(" ", 2);
                if (aux.length != 2) return Utils.cmdERR;
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Writer wr;
                int i = 0;
                for (Project project : projects) {
                    if (project.getName().equals(aux[0])) {
                        if (project.isMember(aux[1])) {
                            if (project.isCompleted()) {
                                projects.remove(i);
                                try {
                                    updateUsersStuatus(aux[1]);
                                } catch (RemoteException e) {e.printStackTrace();}
                                try {
                                    wr = new FileWriter(projectListFile);
                                    gson.toJson(projects, wr);
                                    wr.flush();
                                    wr.close();
                                } catch (IOException e) {e.printStackTrace();}
                                File projectDir = new File(Utils.DbPath + "/" + project.getName());
                                File[] contents = projectDir.listFiles();
                                if (contents != null) {
                                    for (File file : contents) {
                                        file.delete();
                                    }
                                }
                                projectDir.delete();
                                return "Progetto " + aux[0] + " eliminato.";
                            }
                            else {
                                return "Tutti i task devono essere completati prima di eliminare un progetto.";
                            }
                        }
                    }
                    i++;
                }
                return Utils.genericERR;
            }

            default: {
                return Utils.redtext + "Comando non disponibile." + Utils.resetcolor;
            }
        }
    }

}
