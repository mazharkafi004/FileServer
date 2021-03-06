package fileserver.ui;
import com.google.gson.Gson;
import fileserver.Client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;

public class ClientUI implements Client.Callback {
    private JFrame jFrame;
    private JPanel contentPanel;
    private JTable tblFileList;
    private JButton btnHome;
    private JButton btnBack;
    private JLabel lblCurrentPath;
    private JProgressBar progressBarLoading;
    private JLabel lblLoading;
    private JButton btnCngIP;
    private JTextField textFieldIP;
    private JTextField textFieldPort;
    private JButton btnCngPort;
    private JScrollPane scrollPane;

    private DefaultTableModel tableModel;
    private Client socketClient;

    public ClientUI(JFrame jFrame) {
        this.jFrame = jFrame;
        try {
            socketClient = new Client(this, 3599, InetAddress.getByName("localhost"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.out.println("Failed to create Client");
           System.exit(1);
        }

        lblLoading.setText("No Transfer in progress.");


        tableModel = new DefaultTableModel(new String[] { "Folder", "Name"}, 0) {
            @Override
            public boolean isCellEditable(int i, int i1) {
                return false;
            }
        };

        tblFileList.setModel(tableModel);
        tblFileList.setRowSelectionAllowed(true);
        tblFileList.setColumnSelectionAllowed(false);
        tblFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tblFileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getButton() == MouseEvent.BUTTON1 && mouseEvent.getClickCount() == 2) {
                    JTable jTable = (JTable) mouseEvent.getSource();
                    int row = jTable.getSelectedRow();
                    try {
                        socketClient.changeDirectory((String) jTable.getValueAt(row, 1));
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("Error occurred while changing directory.");
                    }
                    updateFileList();
                }
                showPopup(mouseEvent);
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                showPopup(mouseEvent);
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                showPopup(mouseEvent);
            }
        });

        btnHome.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    socketClient.changeDirectory(null);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Error occurred while changing directory to home.");
                }
                updateFileList();
            }
        });
        btnBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    socketClient.changeDirectory("../");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Error occurred while changing to parent directory.");
                }
                updateFileList();
            }
        });
        btnCngIP.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    socketClient.setServerIp(InetAddress.getByName(textFieldIP.getText()));
                    updateFileList();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        });
        btnCngPort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                socketClient.setServerPort(Integer.parseInt(textFieldPort.getText()));
                updateFileList();
            }
        });
        scrollPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger()) {
                    showPopup(mouseEvent, true);
                }
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger()) {
                    showPopup(mouseEvent, true);
                }
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger()) {
                    showPopup(mouseEvent, true);
                }
            }
        });
    }

    private void showPopup(MouseEvent mouseEvent, boolean v) {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem item;

        item = new JMenuItem("New Folder");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String result = (String) JOptionPane.showInputDialog(jFrame, "Name of the folder:", "Create new Folder",
                        JOptionPane.PLAIN_MESSAGE, null, null, "New Folder");
                try {
                    socketClient.createNewFolder(result);
                    updateFileList();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        popupMenu.add(item);

        item = new JMenuItem("Upload");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                uploadFile();
            }
        });
        popupMenu.add(item);

        item = new JMenuItem("Refresh");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                updateFileList();
            }
        });
        popupMenu.add(item);

        if (!socketClient.getCurrentDirectory().equals("")) {
            item = new JMenuItem("Back");
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        socketClient.changeDirectory("../");
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("Error occurred while changing directory.");
                        // todo msg
                    }
                    updateFileList();
                }
            });
            popupMenu.add(item);
        }

        popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
    }

    private void showPopup(MouseEvent mouseEvent) {
        if (mouseEvent.isPopupTrigger()) {
            JTable jTable = (JTable) mouseEvent.getSource();
            int row = jTable.rowAtPoint(mouseEvent.getPoint());
            jTable.setRowSelectionInterval(row, row);

            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem item;
            if (jTable.getValueAt(row, 0).equals("true")) {
                item = new JMenuItem("Open");
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        try {
                            socketClient.changeDirectory((String) jTable.getValueAt(row, 1));
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("Error occurred while changing directory.");
                            // todo msg
                        }
                        updateFileList();
                    }
                });
                popupMenu.add(item);
            } else {
                item = new JMenuItem("Download");
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {

                        downloadFile((String) jTable.getValueAt(row, 1));
                    }
                });
                popupMenu.add(item);
            }
            item = new JMenuItem("Upload");
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    uploadFile();
                }
            });
            popupMenu.add(item);

            item = new JMenuItem("Refresh");
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    updateFileList();
                }
            });
            popupMenu.add(item);

            if (!socketClient.getCurrentDirectory().equals("")) {
                item = new JMenuItem("Back");
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        try {
                            socketClient.changeDirectory("../");
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("Error occurred while changing directory.");
                            // todo msg
                        }
                        updateFileList();
                    }
                });
                popupMenu.add(item);
            }

            popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        }
    }

    private void downloadFile(String filename) {
        if (socketClient.setLoading(true)) {
            JFileChooser jFileChooser = new JFileChooser();
            jFileChooser.setDialogTitle("Select save location");
            int userSelection = jFileChooser.showSaveDialog(tblFileList);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File file = jFileChooser.getSelectedFile();

                try {
                    socketClient.downloadFile(file, filename);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("A file is already being downloaded or uploaded");
        }
    }

    private void uploadFile() {
        if (socketClient.setLoading(true)) {
            JFileChooser jFileChooser = new JFileChooser();
            jFileChooser.setDialogTitle("Select a File to upload");
            int userSelection = jFileChooser.showDialog(tblFileList, "Upload");
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File file = jFileChooser.getSelectedFile();

                try {
                    socketClient.uploadFile(file);
                } catch (IOException e) {
                    e.printStackTrace();
                    // todo failed file uploading
                }
            }
        } else {
            System.out.println("A file is already being downloaded or uploaded");
        }

    }

    private void updateFileList() {
        String received = null;
        try {
            received = socketClient.getFilelist();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to get File List");

        }
        File[] fileList = new Gson().fromJson(received, File[].class);

        tableModel = new DefaultTableModel(new String[] { "Folder", "Name"}, 0) {
            @Override
            public boolean isCellEditable(int i, int i1) {
                return false;
            }
        };

        tblFileList.setModel(tableModel);
        for (File file : fileList) {
            tableModel.addRow(new String[] {
                    file.isDirectory()?"YES":"NO",
                    file.getName(),

                   });
        }
        lblCurrentPath.setText("Path: root/" + socketClient.getCurrentDirectory());
    }

    public static void main(String[] args) {
        JFrame jFrame = new JFrame();

        jFrame.setContentPane(new ClientUI(jFrame).contentPanel);
        jFrame.setResizable(false);
        jFrame.setSize(600, 600);// todo set size
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setVisible(true);
    }



    @Override
    public void onUploadStart(String filename) {
        lblLoading.setText("Uploading: " + filename);

    }

    @Override
    public void onDownloadStart(String filename) {
        lblLoading.setText("Downloading: " + filename);

    }

    @Override
    public void onDownloadComplete(String filename) {
        lblLoading.setText("Download Complete: " + filename);
        socketClient.setLoading(false);

    }

    @Override
    public void onDownloadError(String errorMsg) {
        socketClient.setLoading(false);
    }

    @Override
    public void updateProgress(long progress) {
        progressBarLoading.setValue(Math.toIntExact(progress));
    }

    @Override
    public void onUploadComplete(String filename) {
        lblLoading.setText("Upload Complete: " + filename);
        socketClient.setLoading(false);
        updateFileList();
    }

    @Override
    public void onUploadError(String errorMsg) {
        socketClient.setLoading(false);
    }
}