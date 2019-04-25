import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ConsoleHelper.writeMessage("Enter server port: ");
        int serverPort = ConsoleHelper.readInt();
        ServerSocket serverSocket = new ServerSocket(serverPort);
        ConsoleHelper.writeMessage("Server start!");

        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                handler.start();
            }
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Server down!");
            serverSocket.close();
        }
    }

    private static class Handler extends Thread {
        Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            Message mes;
            String name;
            do {
                connection.send(new Message(MessageType.NAME_REQUEST));
                mes = connection.receive();
                name = mes.getData();
            }
            while (mes.getType() != MessageType.USER_NAME || name.isEmpty() || connectionMap.containsKey(name));
            connection.send(new Message(MessageType.NAME_ACCEPTED));
            connectionMap.put(name, connection);
            return name;
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (Map.Entry<String, Connection> conn : connectionMap.entrySet()) {
                if (conn.getKey() != userName) {
                    connection.send(new Message(MessageType.USER_ADDED, conn.getKey()));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            Message mes;
            do {
                mes = connection.receive();
                if (mes.getType() == MessageType.TEXT)
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + mes.getData()));
                else ConsoleHelper.writeMessage("!!!Error to send message!!!");
            }
            while (true);
        }

        public void run() {
            System.out.println(socket.getRemoteSocketAddress());
            //ConsoleHelper.writeMessage(socket.getRemoteSocketAddress());
            String userName = null;
            try (Connection connection = new Connection(socket)){
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection, userName);
                serverMainLoop(connection, userName);
            }catch (IOException | ClassNotFoundException e){
                if (userName != null) {
                    connectionMap.remove(userName);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
                }
                else
                    System.out.println("Error.");
            }



            ConsoleHelper.writeMessage("Connection closed...");
        }
    }


    public static void sendBroadcastMessage(Message message) {
        for (Map.Entry<String, Connection> nameConnect: connectionMap.entrySet()) {
            try {
                nameConnect.getValue().send(message);
            }catch (IOException e){
                System.out.println("Error to sen br message");
            }
        }
    }




}
