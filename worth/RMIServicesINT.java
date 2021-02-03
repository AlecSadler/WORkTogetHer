// INTERFACCIA REMOTA LATO SERVER PER SERVIZI RMI

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIServicesINT extends Remote {

    boolean register (String username, String password) throws RemoteException;

    void cbSubscribe(ClientNotifyINT clientInterface, String user) throws RemoteException;

    void cbUnsubscribe(String user) throws RemoteException;

    void notifyAddress(String user) throws RemoteException;

}
