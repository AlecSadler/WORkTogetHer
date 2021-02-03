// ESEGUIBILE DEL SERVER, AVVIA TUTTI I SERVIZI

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class MainServer {

    public static void main(String[] args) {
        WorthServer server = new WorthServer();
        try {
            RMIServicesINT stub;
            Registry registry;
            stub = (RMIServicesINT) UnicastRemoteObject.exportObject(server, 0);
            LocateRegistry.createRegistry(Utils.RMIPort);
            registry = LocateRegistry.getRegistry(Utils.RMIPort);
            registry.rebind("RMI_SERVICES", stub);
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
        server.start();
    }
}
