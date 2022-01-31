import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.sql.ResultSet;
import java.util.*;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.internet.*;
import javax.swing.*;
import javax.swing.event.*;

// The E-mail Client.
public class EmailClient extends JFrame
{
    public static String loginUser,connSMTP;
    public static ArrayList<String> addressBook=new ArrayList<>();;
    // Message table's data model.
    private MessagesTableModel tableModel;

    // Table listing messages.
    private JTable table;

    // This the text area for displaying messages.
    private JTextArea messageTextArea;

    /* This is the split panel that holds the messages
       table and the message view panel. */
    private JSplitPane splitPane;

    // 답장, 전달, 삭제
    private JButton replyButton, forwardButton, deleteButton;

    // 테이블에서 현재 선택된 메시지
    private Message selectedMessage;

    // 어떤 메시지가 현재 삭제되고 있는지 여부를 나타내는 플래그
    private boolean deleting;

    // JavaMail 세션
    private Session session;

    // E-mail Client 생성자
    public EmailClient()
    {
        // Set application title.
        setTitle("E-mail Client");

        // Set window size.
        setSize(640, 480);

        // Handle window closing events.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionExit();
            }
        });

        // Setup file menu.
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem fileExitMenuItem = new JMenuItem("Exit",
                KeyEvent.VK_X);
        fileExitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionExit();
            }
        });
        fileMenu.add(fileExitMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Setup buttons panel.
        JPanel buttonPanel = new JPanel();
        JButton newButton = new JButton("New Message");
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionNew ();
            }
        });
        buttonPanel.add(newButton);

        // Setup messages table.
        tableModel = new MessagesTableModel();
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                tableSelectionChanged();
            }
        });
        // Allow only one row at a time to be selected.
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Setup E-mails panel.
        JPanel emailsPanel = new JPanel();
        emailsPanel.setBorder(
                BorderFactory.createTitledBorder("E-mails"));
        messageTextArea = new JTextArea();
        messageTextArea.setEditable(false);
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(table), new JScrollPane(messageTextArea));
        emailsPanel.setLayout(new BorderLayout());
        emailsPanel.add(splitPane, BorderLayout.CENTER);

        // Setup buttons panel 2.
        JPanel buttonPanel2 = new JPanel();
        replyButton = new JButton("Reply");
        replyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionReply();
            }
        });
        replyButton.setEnabled(false);
        buttonPanel2.add(replyButton);
        forwardButton = new JButton("Forward");
        forwardButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionForward();
            }
        });
        forwardButton.setEnabled(false);
        buttonPanel2.add(forwardButton);
        deleteButton = new JButton("Delete");
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionDelete();
            }
        });
        deleteButton.setEnabled(false);
        buttonPanel2.add(deleteButton);

        // Add panels to display.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buttonPanel, BorderLayout.NORTH);
        getContentPane().add(emailsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel2, BorderLayout.SOUTH);
    }

    // 프로그램 종료
    private void actionExit() {
        System.exit(0);
    }

    // 테이블의 행이 선택될 때마다 호출된다.
    private void tableSelectionChanged() {
    // 선택된 행에 들어있는 메시지가 삭제중이 아니라면 사용자에게 보여준다.
        if (!deleting) {
            selectedMessage =
                    tableModel.getMessage(table.getSelectedRow());
            showSelectedMessage();
            updateButtons();
        }
    }

    // 새 메시지 작성
    private void actionNew () {
        sendMessage(MessageDialog.NEW, null);
    }
    // 답장 보내기
    private void actionReply() {
        sendMessage(MessageDialog.REPLY, selectedMessage);
    }
    // 전달하기
    private void actionForward() {
        sendMessage(MessageDialog.FORWARD, selectedMessage);
    }

    // 선택한 메시지 삭제
    private void actionDelete() {
        deleting = true;

        try {
            // 서버에서 메시지 삭제
            selectedMessage.setFlag(Flags.Flag.DELETED, true);
            Folder folder = selectedMessage.getFolder();
            folder.close(true);
            folder.open(Folder.READ_WRITE);
        } catch (Exception e) {
            showError("Unable to delete message.", false);
        }

        // 테이블에서 메시지 삭제
        tableModel.deleteMessage(table.getSelectedRow());

        // GUI 갱신
        messageTextArea.setText("");
        deleting = false;
        selectedMessage = null;
        updateButtons();
    }

    // 메시지 보내기
    private void sendMessage(int type, Message message) {
        // 메시지 대화상자를 띄운다.
        MessageDialog dialog;
        try {
            dialog = new MessageDialog(this, type, message);
            if (!dialog.display()) {
                // 취소 버튼에 의해 리턴되는 경우
                return;
            }
        } catch (Exception e) {
            System.out.println(e);
            showError("Unable to send message.", false);
            return;
        }

        try {
            // 메시지 대화상자의 접근자를 이용해 새 메시지 작성
            Message newMessage = new MimeMessage(session);
            newMessage.setFrom(new InternetAddress(dialog.getFrom()));
            newMessage.setRecipient(Message.RecipientType.TO,
                    new InternetAddress(dialog.getTo()));
            newMessage.setSubject(dialog.getSubject());
            newMessage.setSentDate(new Date());

            //파일 첨부를 위한 Multipart 객체 생성
            Multipart multiPart = new MimeMultipart();
            // 내용 부분
            MimeBodyPart contentsBodyPart = new MimeBodyPart();
            contentsBodyPart.setText(dialog.getContent());
            multiPart.addBodyPart(contentsBodyPart);
            // 파일 부분
            if(!dialog.getFile().equals("")){
                MimeBodyPart fileBodyPart = new MimeBodyPart();
                FileDataSource fileData=new FileDataSource(dialog.getFile());
                fileBodyPart.setDataHandler(new DataHandler(fileData));
                fileBodyPart.setFileName(MimeUtility.encodeText(fileData.getName()));
                multiPart.addBodyPart(fileBodyPart);
            }
            newMessage.setContent(multiPart);

            // 새 메시지를 보냄
            Transport.send(newMessage);
        } catch (Exception e) {
            System.out.println(e);
            showError("Unable to send message.", false);
        }
        //db에 주소록 저장
        insertAddressBook(dialog);
        //db에서 주소록 불러와 갱신
        selectAddressBook();

    }

    // 선택된 메시지를 보여준다
    private void showSelectedMessage() {
        // 메시지가 로딩되는 동안 커서를 모래시계로 바꾼다.
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            messageTextArea.setText(
                    getMessageContent(selectedMessage));
            messageTextArea.setCaretPosition(0);
        } catch (Exception e) {
            showError("Unabled to load message.", false);
        } finally {
            // 커서를 원래대로
            setCursor(Cursor.getDefaultCursor());
        }
    }

    //각 버튼의 상태를 테이블에 현재 선택된 메시지가 있는지 여부에 따라 갱신
    private void updateButtons() {
        if (selectedMessage != null) {
            replyButton.setEnabled(true);
            forwardButton.setEnabled(true);
            deleteButton.setEnabled(true);
        } else {
            replyButton.setEnabled(false);
            forwardButton.setEnabled(false);
            deleteButton.setEnabled(false);
        }
    }

    // Show the application window on the screen.
    public void show() {
        super.show();

        // Update the split panel to be divided 50/50.
        splitPane.setDividerLocation(.5);
    }

    // 이메일 서버에 접속
    public void connect() {
        // 연결 대화상자를 띄운다
        ConnectDialog dialog = new ConnectDialog(this);
        dialog.show();

        // 접속 URL 생성
        StringBuffer connectionUrl = new StringBuffer();
        connectionUrl.append(dialog.getServerType() + "://");
        connectionUrl.append(dialog.getUsername() + ":");
        connectionUrl.append(dialog.getPassword() + "@");
        connectionUrl.append(dialog.getServer() + "/");

    /* Display dialog stating that messages are
       currently being downloaded from server. */
        final DownloadingDialog downloadingDialog =
                new DownloadingDialog(this);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                downloadingDialog.show();
            }
        });

        // JavaMail 세션을 초기화 한 뒤 접속
        Store store = null;
        try {
            // JavaMail 세션을 SMTP 서버로 초기화
            Properties props = new Properties();
            props.put("mail.smtp.host", dialog.getSmtpServer());
            props.put("mail.smtp.auth","true");
            props.put("mail.smtp.ssl.enable","true");
            props.put("mail.smtp.ssl.trust",dialog.getSmtpServer());

            props.put("mail.pop3.host", dialog.getServer());
            props.put("mail.pop3.socketFactory.port", "995");
            props.put("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.pop3.socketFactory.fallback", "false");

            props.put("mail.imap.host", dialog.getServer());
            props.put("mail.imap.socketFactory.port", "993");
            props.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.imap.socketFactory.fallback", "false");

            session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(dialog.getUsername(), dialog.getPassword());
                }
            });

            // 이메일 서버에 접속
            URLName urln = new URLName(connectionUrl.toString());
            store = session.getStore(urln);
            store.connect();

            loginUser=dialog.getUsername();
            connSMTP=dialog.getSmtpServer();
        } catch (Exception e) {
            // 다운로딩 대화상자를 닫는다
            downloadingDialog.dispose();
            System.out.println(e);
            showError("Unable to connect.", true);
        }

        // 서버로부터 메시지 헤더를 다운로드
        try {
            // 받은 편지함 폴더를 연다
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);

            // 메시지 리스트를 받아온다
            Message[] messages = folder.getMessages();

            // 폴더의 각 메시지에 대해 헤더 정보를 가져온다
            FetchProfile profile = new FetchProfile();
            profile.add(FetchProfile.Item.ENVELOPE);
            folder.fetch(messages, profile);

            // 테이블에 메시지를 넣는다.
            tableModel.setMessages(messages);
        } catch (Exception e) {
            // 다운로딩 대화상자를 닫는다.
            downloadingDialog.dispose();

            // Show error dialog.
            showError("Unable to download messages.", true);
        }

        // Close the downloading dialog.
        downloadingDialog.dispose();

        //db에 연결 정보 저장
        insertConnInfo(dialog);

        //db에서 주소록 불러옴
        selectAddressBook();
    }

    //DB에 연결 정보를 저장
    private void insertConnInfo(ConnectDialog dialog){
        try{
            SQLiteControl control =new SQLiteControl();
            ArrayList<String> list=new ArrayList<>();
            String sql="REPLACE INTO connInfo VALUES (?,?,?,?)";

            list.add(dialog.getServer());
            list.add(dialog.getUsername());
            list.add(dialog.getPassword());
            list.add(dialog.getSmtpServer());

            //DB에 삽입
            control.insert(sql, list);
        }catch (Exception e){
            System.out.println(e);
            showError("DBerr: Unable insert", true);
        }
    }
    //DB에 주소록 저장
    private void insertAddressBook(MessageDialog dialog){
        try{
            SQLiteControl control =new SQLiteControl();
            ArrayList<String> list=new ArrayList<>();
            String sql="REPLACE INTO addressBook VALUES (?,?,?)";

            list.add(loginUser);
            list.add(connSMTP);
            list.add(dialog.getTo());

            //DB에 삽입
            control.insert(sql, list);
        }catch (Exception e){
            System.out.println(e);
            showError("DBerr: Unable insert", true);
        }
    }
    //DB에서 주소록 불러오기
    private void selectAddressBook(){
        try{
            SQLiteControl control =new SQLiteControl();
            ResultSet result;

            String sql="SELECT Address FROM addressBook WHERE Username='"+loginUser+"' AND SMTP='"+connSMTP+"'";
            result=control.select(sql);

            addressBook.clear();
            while(result.next()){
                addressBook.add(result.getString("Address"));
            }
            result.close();
            control.closeConnection();
        }catch (Exception e){
            System.out.println(e);
        }
    }
    // Show error dialog and exit afterwards if necessary.
    private void showError(String message, boolean exit) {
        JOptionPane.showMessageDialog(this, message, "Error",
                JOptionPane.ERROR_MESSAGE);
        if (exit)
            System.exit(0);
    }

    // 메시지 내용을 얻는다.
    public static String getMessageContent(Message message)
            throws Exception {
        Object content = message.getContent();
        if (content instanceof Multipart) {
            StringBuffer messageContent = new StringBuffer();
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                Part part = (Part) multipart.getBodyPart(i);
                if (part.isMimeType("text/plain")) {
                    messageContent.append(part.getContent().toString());
                }
            }
            return messageContent.toString();
        } else {
            return content.toString();
        }
    }

    // 이메일 클라이언트 실행
    public static void main(String[] args) {
        //SQLiteManager manager = new SQLiteManager();
        //manager.createConnection();     // db 연결

        EmailClient client = new EmailClient();
        client.show();

        // Display connect dialog.
        client.connect();
    }
}
