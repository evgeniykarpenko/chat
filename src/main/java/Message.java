import java.io.Serializable;

public class Message implements Serializable {
    private final MessageType type;
    private final String data;
    private final boolean privat;

    public MessageType getType() {
        return type;
    }

    public boolean getPrivate() {
        return this.privat;
    }

    public String getData() {
        return data;
    }

    public Message(MessageType type) {
        this.type = type;
        this.data = null;
        this.privat = false;
    }

    public Message(MessageType type, String data) {
        this.type = type;
        this.data = data;
        this.privat = false;
    }

    public Message(MessageType type, String data, Boolean privat) {
        this.type = type;
        this.data = data;
        this.privat = true;
    }
}
