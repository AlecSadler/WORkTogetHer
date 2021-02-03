// CLASSE CHE MODELLA UNA CARD DEL PROGETTO

import java.util.ArrayList;

public class Card {

    private String name;
    private String description;
    private String list;
    private ArrayList<String> history;

    public Card(String name, String description) {
        this.name = name;
        this.description = description;
        history = new ArrayList<>();
        history.add("TODO");
        list = "TODO";
    }

    // aggiunge anche la voce nella history
    public void setStatus(String to) {
        history.add(to.toUpperCase());
        list = to.toUpperCase();
    }

    public String getName() {
        return name;
    }

    public String getList() {
        return list;
    }

    public String getDescription() {
        return description;
    }

    public ArrayList<String> getHistory() {
        return history;
    }

}
