// INTERFACCIA REMOTA LATO CLIENT PER SERVIZIO NOTIFICHE CALLBACK

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

public interface ClientNotifyINT extends Remote {

    void notifyUsers(HashMap<String, String> users) throws RemoteException;

    void notifySockets(HashMap<String, String> sockets) throws RemoteException;

}
