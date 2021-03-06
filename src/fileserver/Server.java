package fileserver;
import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static final String FILE_LIST = "1001";
    public static final String DOWNLOAD_FILE = "1002";
    public static final String UPLOAD_FILE = "1003";
    public static final String CHANGE_DIRECTORY = "1004";
    public static final String NEW_FOLDER = "1005";
    public static final String CONNECTED = "1051";
    public static final String DISCONNECTED = "1052";
    public static final String YES = "1053";
    public static final String NO = "1054";

    private int serverPort;

    private ServerSocket serverSocket;

    private File rootDirectory;

    Thread serverMain;
    Callback callback;

    public Server(Callback callback) {
        serverPort = 3599;
        setRootDirectory(null);
        this.callback = callback;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        stopServer();
        this.serverPort = serverPort;
        if (callback != null) {
            callback.printMsg("Port successfully changed to: " + this.serverPort);
        }
        startServer();
    }

    public File getRootDirectory() {
        return rootDirectory;
    }


    public boolean setRootDirectory(File root) {
        try {
            if (root == null) {
                root = new File(System.getProperty("user.dir") + File.separator + "Files");
            }
            if (!root.exists()) {
                root.mkdirs();
            }
            rootDirectory = root.getCanonicalFile();
        } catch (IOException e) {
            System.out.println("Failed to set root directory.");
            if (callback != null) {
                callback.printMsg("Failed to change Root Directory.");
            }
            e.printStackTrace();
        }
        System.out.println("Root Directory: " + rootDirectory.getAbsolutePath());
        if (callback != null) {
            callback.printMsg("Root Directory changed to:" + rootDirectory.getAbsolutePath());
        }
        return true;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(serverPort);
            System.out.println("Started server at port: " + serverPort);
            if (callback != null) {
                callback.printMsg("Started server at port: " + serverPort);
            }
        } catch (IOException e) {
            if (callback != null) {
                callback.printMsg("Failed to start Server.");
            }
            e.printStackTrace();
        }

        serverMain = new Thread(new Runnable() {
            @Override
            public void run() {
                listenToSocket();
            }
        });

        serverMain.start();

    }

    public void stopServer() {
        try {
            serverSocket.close();
            if (callback != null) {
                callback.printMsg("Server successfully stopped.");
            }
        } catch (IOException e) {
            if (callback != null) {
                callback.printMsg("Error occurred while stopping the server.");
            }
            e.printStackTrace();
        }
    }

    private String getFileList(String path) {
        File currentDirectory = new File(rootDirectory.getAbsolutePath() + File.separator + path);
        System.out.println(path);

        File[] fileList = currentDirectory.listFiles();


        String jsonData = new Gson().toJson(fileList);
        return jsonData;
    }

    private String isDirectory(String path) {
        File currentDirectory = new File(rootDirectory.getAbsolutePath() + File.separator + path);
        try {
            currentDirectory = currentDirectory.getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (currentDirectory.getAbsolutePath().equals(rootDirectory.getAbsolutePath())) {
            return "";
        } else if (currentDirectory.isDirectory() && currentDirectory.exists()
                && currentDirectory.getAbsolutePath().length() > rootDirectory.getAbsolutePath().length()) {
            System.out.println(currentDirectory.getPath());
            return currentDirectory.getAbsolutePath().substring(rootDirectory.getAbsolutePath().length() + 1);
        }
        return Server.NO;
    }

    private void listenToSocket() {
        Socket socket = null;
        while (!serverSocket.isClosed()) {
            try {
                if (callback != null) {
                    callback.printMsg("Waiting for Clients to connect.");
                }
                socket = serverSocket.accept();
                System.out.println("Connected to a new client: " + socket);
                if (callback != null) {
                    callback.printMsg("Connected to a new client: " + socket);
                }

                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                ClientHandler clientTask = new ClientHandler(socket, dis, dos);
                clientTask.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (callback != null) {
            callback.printMsg("Stopped Waiting for Clients.");
        }
    }

    public interface Callback {
        public void printMsg(String msg);
    }

    public class ClientHandler extends Thread {
        Socket socket;
        DataInputStream dis;
        DataOutputStream dos;

        public ClientHandler(Socket socket, DataInputStream dis, DataOutputStream dos) {
            this.socket = socket;
            this.dis = dis;
            this.dos = dos;
        }

        @Override
        public void run() {
            try {
                String receivedcmd = dis.readUTF();
                String received;
                File file;
                switch (receivedcmd) {
                    case Server.FILE_LIST:
                        if (callback != null) {
                            callback.printMsg("File List request from: " + socket);
                        }
                        received = dis.readUTF();
                        dos.writeUTF(getFileList(received));
                        break;
                    case Server.CHANGE_DIRECTORY:
                        if (callback != null) {
                            callback.printMsg("Change Directory request from: " + socket);
                        }
                        received = dis.readUTF();
                        dos.writeUTF(isDirectory(received));
                        break;
                    case Server.NEW_FOLDER:
                        if (callback != null) {
                            callback.printMsg("New folder request from: " + socket);
                        }
                        received = dis.readUTF();
                        file = new File(rootDirectory.getAbsolutePath() + File.separator + received);
                        if (file.exists() && file.isDirectory()) {
                            if (callback != null) {
                                callback.printMsg("Directory already exists.");
                            }
                        } else {
                            file.mkdirs();
                            if (callback != null) {
                                callback.printMsg("Directory created successfully.");
                            }
                        }
                        break;
                    case Server.DOWNLOAD_FILE:
                        if (callback != null) {
                            callback.printMsg("File Download request from: " + socket);
                        }
                        received = dis.readUTF();
                        file = new File(rootDirectory.getAbsolutePath() + File.separator + received);
                        if (file.exists() && !file.isDirectory()) {
                            dos.writeUTF(Server.YES);
                            dos.writeUTF(String.valueOf(file.length()));
                            received = dis.readUTF();
                            if (received.equals(Server.YES)) {
                                FileInputStream fileInputStream = new FileInputStream(file);
                                byte[] buffer = new byte[4096];

                                int read = 0;
                                while ((read = fileInputStream.read(buffer)) > 0) {
                                    dos.write(buffer, 0, read);
                                }
                                dos.writeUTF(Server.YES);
                                if (callback != null) {
                                    callback.printMsg("File sent Successfully.");
                                }
                            }
                        } else {
                            if (callback != null) {
                                callback.printMsg("File does not exit or is a directory.");
                            }
                        }
                        break;
                    case Server.UPLOAD_FILE:
                        if (callback != null) {
                            callback.printMsg("File Upload request from: " + socket);
                        }
                        String filepath = rootDirectory.getAbsolutePath() + File.separator + dis.readUTF();
                        long fileSize = Long.parseLong(dis.readUTF());
                        file = new File(filepath);
                        if ((!file.exists()) && rootDirectory.getFreeSpace() > fileSize) {
                            dos.writeUTF(Server.YES);

                            int read = 0;
                            long remaining = fileSize;
                            byte[] buffer = new byte[4096];
                            FileOutputStream fileOutputStream = new FileOutputStream(file);

                            while ((read = dis.read(buffer, 0,
                                    Math.toIntExact(Math.min(buffer.length, remaining)))) > 0) {
                                remaining -= read;
                                fileOutputStream.write(buffer, 0, read);
                                System.out.println(
                                        "Upload complete ===> " + ((fileSize - remaining) * 100 / fileSize) + "%");
                            }
                            received = dis.readUTF();
                            if (received.equals(Server.YES)) {
                                System.out.println("Upload Complete.");
                                if (callback != null) {
                                    callback.printMsg("File successfully received.");
                                }
                            }
                        } else {
                            dos.writeUTF(Server.NO);
                            if (callback != null) {
                                callback.printMsg("File already exists or insufficient space.");
                            }
                        }
                        break;
                    case Server.DISCONNECTED:

                    default:
                        break;
                }

                dis.close();
                dos.close();
                socket.close();
                System.out.println("Disconnected from " + socket);
                if (callback != null) {
                    callback.printMsg("Disconnected from: " + socket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}