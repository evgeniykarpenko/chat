

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    public class SocketThread extends Thread {

        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void processIncomingPrivateMessage(String message) {

            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage("New user: " + userName);
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage("User removed: " + userName);
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            String name;
            Message mes;
            ConsoleHelper.writeMessage("Enter name: ");
            while (true) {
                mes = connection.receive();

                if (mes.getType() == MessageType.NAME_REQUEST) {
                    name = getUserName();
                    connection.send(new Message(MessageType.USER_NAME, name));
                } else if (mes.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    return;
                } else throw new IOException("Unexpected MessageType");
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            Message mes;
            while (true) {
                mes = connection.receive();
                if (mes.getPrivate()&&(mes.getType() == MessageType.TEXT)) {
                    processIncomingPrivateMessage(mes.getData());
                }
                else if (mes.getType() == MessageType.TEXT) {
                    processIncomingMessage(mes.getData());
                } else if (mes.getType() == MessageType.USER_ADDED)
                    informAboutAddingNewUser(mes.getData());
                else if (mes.getType() == MessageType.USER_REMOVED)
                    informAboutDeletingNewUser(mes.getData());
                else throw new IOException("Unexpected MessageType");
            }
        }

        public void run() {
            ConsoleHelper.writeMessage("Enter server adress: ");
            String serverAdress = getServerAddress();
            ConsoleHelper.writeMessage("Enter server port: ");
            int serverPort = getServerPort();
            try {
                Socket socket = new Socket(serverAdress, serverPort);
                Client.this.connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e){
                notifyConnectionStatusChanged(false);
                ConsoleHelper.writeMessage("Client connection error");

            }
        }
    }

    protected String getServerAddress() {
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            clientConnected = false;
            ConsoleHelper.writeMessage("Error to sen message !!!");
        }
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Interrupt error");
            }
        }

        if (clientConnected) {
            ConsoleHelper.writeMessage("Connection established write - 'exit' to stop.");
        } else {
            ConsoleHelper.writeMessage("Client error.");
        }

        while (clientConnected) {
            String line = ConsoleHelper.readString();
            if (line.equals("exit")) break;
            if (shouldSendTextFromConsole()) sendTextMessage(line);
        }
    }
}

