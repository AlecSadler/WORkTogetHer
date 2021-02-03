// CLASSE CHE MODELLA UN UTENTE

public class User {

    private String nickname;
    private String password;
    private String status;

    public User (String user, String pw) {
        nickname = user;
        password = pw;
        status = "online";
    }

    // confronta i parametri con user e password dell'oggetto, usata nel login
    public boolean authenthication (String nick, String pw) {
        return (nickname.equals(nick) && password.equals(pw));
    }

    public void setStatus(String st) {
        status = st;
    }

    public boolean getStatus(){
        if (status.equals("online")) return true;
        else return false;
    }

    public String getNickname() {
        return nickname;
    }

}
