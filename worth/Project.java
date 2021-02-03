// CLASSE CHE MODELLA UN PROGETTO

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

public class Project {

    private String name;
    private String multicastAddress;
    private ArrayList<String> members;
    private ArrayList<String> cards;
    private ArrayList<Card> todo;
    private ArrayList<Card> inprogress;
    private ArrayList<Card> toberevised;
    private ArrayList<Card> done;
    private File DbPath;

    public Project(String n, String nick, String chatAdd) {

        name = n;

        multicastAddress = chatAdd;
        members = new ArrayList<>();
        cards = new ArrayList<>();
        todo = new ArrayList<>();
        inprogress = new ArrayList<>();
        toberevised = new ArrayList<>();
        done = new ArrayList<>();
        members.add(nick);

        DbPath = new File("Data/" + name);
        if (!DbPath.exists()) {
            if (!DbPath.mkdir()) {
                System.out.println(Utils.genericERR);
            }
        }
    }


    public String getName() {
        return name;
    }

    public ArrayList<String> getMembers() {
        return members;
    }

    public boolean isMember (String nick) {
        return members.contains(nick);
    }

    public void addMember(String nick) {
        if (!members.contains(nick)) {
            members.add(nick);
        }
    }

    public String getAddress() {
        return multicastAddress;
    }

    public Card getCard(String name) {
        if (!cards.contains(name)) return null;
        for (Card card : todo) {
            if (card.getName().equals(name)) {
                return card;
            }
        }
        for (Card card : inprogress) {
            if (card.getName().equals(name)) {
                return card;
            }
        }
        for (Card card : toberevised) {
            if (card.getName().equals(name)) {
                return card;
            }
        }
        for (Card card : done) {
            if (card.getName().equals(name)) {
                return card;
            }
        }
        return null;
    }

    public ArrayList<Card> getAllCards() {
        if (cards==null || cards.isEmpty()) {
            return null;
        }
        ArrayList<Card> allCards = new ArrayList<>();
        if (todo!=null && !todo.isEmpty()) {
            allCards.addAll(todo);
        }
        if (inprogress!=null && !inprogress.isEmpty()) {
            allCards.addAll(inprogress);
        }
        if (toberevised!=null && !toberevised.isEmpty()) {
            allCards.addAll(toberevised);
        }
        if (done!=null && !done.isEmpty()) {
            allCards.addAll(done);
        }
        return allCards;
    }

    public boolean addCard(String n, String descr) {
        if (!cards.contains(n)) {
            Writer wr;
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Card card = new Card(n, descr);
            if (todo == null) todo = new ArrayList<>();
            todo.add(card);     // le card vengono aggiunte a TODO di default
            cards.add(n);
            File cardFile = new File("Data/" + name + "/" + n + ".json");
            if (!cardFile.exists()) {
                try {
                    cardFile.createNewFile();
                } catch (IOException e) {e.printStackTrace();}
            }
            try {
                wr = new FileWriter(cardFile);
                gson.toJson(card, wr);
                wr.flush();
                wr.close();
            } catch (IOException e) {e.printStackTrace();}
            return true;
        }
        else return false;
    }

    // mi serve quando sposto una card, la prelevo ed elimino per poi aggiungerla all'altra lista
    public Card copyNremove(ArrayList<Card> list, String name) {
        for (Card card : list) {
            if (card.getName().equals(name)) {
                list.remove(card);
                return card;
            }
        }
        return null;
    }

    public boolean moveCard(String name, String from, String to) {
        if (from.equals(to)) return true;
        if (!cards.contains(name)) return false;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter wr;
        Card card;
        switch(from) {
            case "todo": {
                if (!to.equals("inprogress")) return false;
                card = copyNremove(todo, name);
                if (card==null) return false;
                card.setStatus(to);
                inprogress.add(card);
                break;
            }
            case "inprogress": {
                if (!to.equals("toberevised") && !to.equals("done")) return false;
                card = copyNremove(inprogress, name);
                if (card==null) return false;
                if (to.equals("toberevised")) {
                    toberevised.add(card);
                } else {
                    done.add(card);
                }
                card.setStatus(to);
                break;
            }
            case "toberevised": {
                if (!to.equals("inprogress") && !to.equals("done")) return false;
                card = copyNremove(toberevised, name);
                if (card==null) return false;
                if (to.equals("inprogress")) {
                    inprogress.add(card);
                } else {
                    done.add(card);
                }
                card.setStatus(to);
                break;
            }
            default: {
                return false;
            }
        }
        File cardFile = new File("Data/"+this.name+"/"+name+".json");
        try {
            wr = new FileWriter(cardFile,false);
            gson.toJson(card, wr);
            wr.flush();
            wr.close();
        } catch (IOException e) {e.printStackTrace();}
        return true;
    }

    // come da specifica, per eliminare un progetto devono essere in DONE tutte le cards
    public boolean isCompleted() {
        return todo.isEmpty() && inprogress.isEmpty() && toberevised.isEmpty();
    }

}
