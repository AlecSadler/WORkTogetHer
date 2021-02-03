// COSTANTI UTILIZZATE NELL'IMPLEMENTAZIONE

public class Utils {

    // PATH PRINCIPALI
    public final static String DbPath = "./Data";
    public final static String projectsPath = "./Data/projects.json";
    public final static String usersPath = "./Data/users.json";
    public final static String lastipPath = "lastip.txt";

    // PORTE DI COMUNICAZIONE E INDIRIZZI
    public final static int TCPPort = 6666;
    public final static int UDPListenPort = 7777;
    public final static int RMIPort = 9999;
    public final static String ipAddress = "Localhost";
    public final static String multicastBase = "224.0.0.0";

    // COLORI ANSI PER LA CONSOLE
    public static final String redtext = "\u001b[31;1m";
    public static final String greentext = "\u001b[32;1m";
    public static final String resetcolor = "\u001b[0m";
    public static final String magentatext = "\u001b[35;1m";
    public static final String bluetext = "\033[1;94m";
    public static final String yellowtext = "\033[1;93m";
    public static final String whitebkg = "\033[0;107m";
    public static final String blacktext = "\033[1;30m";
    public static final String ciantext = "\033[1;96m";

    // MESSAGGI DI ERRORE COMUNI
    public static final String genericERR = redtext + "Oops, qualcosa è andato storto!" + resetcolor;
    public static final String cmdERR = redtext + "Comando non riconosciuto! Ricontrollare la struttura." + resetcolor;
    public static final String userERR = redtext + "Utente sconosciuto!" + resetcolor;
    public static final String loginERR = redtext + "Devi effettuare il login per questa funzione" + resetcolor;

    // METODO CHE STAMPA LA LISTA DELLE FUNZIONI
    public static void showHelp() {
        System.out.println(whitebkg + blacktext + "ELENCO DELLE FUNZIONI DISPONIBILI:" + resetcolor);
        System.out.println(ciantext + "register [nickname] [password]" + resetcolor);
        System.out.println(ciantext + "login [nickname] [password]" + resetcolor);
        System.out.println(ciantext + "logout [nickname]" + resetcolor);
        System.out.println(ciantext + "listprojects" + resetcolor + " - mostra i progetti a cui stai lavorando");
        System.out.println(ciantext + "createproject [nomeprog]" + resetcolor + " - crea un progetto");
        System.out.println(ciantext + "addmember [nomeprog] [nickname]" + resetcolor + " - aggiungi un membro ad un progetto");
        System.out.println(ciantext + "showmembers [nomeprog]" + resetcolor + " - visualizza i membri di un progetto");
        System.out.println(ciantext + "showcards [nomeprog]" + resetcolor + " - mostra i taks (Cards) di un progetto");
        System.out.println(ciantext + "listusers" + resetcolor + " - visualizza l'elenco degli utenti resistrati su Worth");
        System.out.println(ciantext + "listonlineusers" + resetcolor + " - visualizza chi è online in questo momento");
        System.out.println(ciantext + "showcard [nomeprog] [cardname]" + resetcolor + " - visualizza il dettaglio di una Card");
        System.out.println(ciantext + "addcard [nomeprog] [cardname] [descr]" + resetcolor + " - aggiungi una card ad un progetto");
        System.out.println(ciantext + "movecard [nomeprog] [cardname] [list1] [list2]" + resetcolor + " - sposta una card da uno stato di lavorazione ad un altro");
        System.out.println(ciantext + "getcardhistory [nomeprog] [cardname]" + resetcolor + " - visualizza lo storico di lavorazione di una card");
        System.out.println(ciantext + "readchat [nomeprog]" + resetcolor + " - visualizza i messaggi non letti nella chat del progetto");
        System.out.println(ciantext + "sendchatmsg [nomeprog] [message]" + resetcolor + " - invia un messaggio agli altri membri del progetto");
        System.out.println(ciantext + "cancelproject [nomeprog]" + resetcolor + " - elimina un progetto");
        System.out.println(ciantext + "exit" + resetcolor + " - termina il client");
        System.out.println(ciantext + "help" + resetcolor + " - per rivedere questa schermata in seguito");
        System.out.println(magentatext + "N.B. I comandi non sono case-sensitive (login = LoGIn)" + resetcolor);
        System.out.println(magentatext + "N.B. Se un messaggio di chat ha più parole separarle con un trattino (ciao-mario)" + resetcolor);
    }
}
