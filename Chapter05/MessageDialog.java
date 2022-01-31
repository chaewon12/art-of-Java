import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.mail.*;
import javax.swing.*;

// 메일 메시지 작성을 위한 클래스
public class MessageDialog extends JDialog
{
    // 메시지 타입 식별자
    public static final int NEW = 0;        //새 메시지
    public static final int REPLY = 1;      //답장
    public static final int FORWARD = 2;    //전달

    // Message from, to and subject text fields.
    private JTextField fromTextField;
    private JTextField subjectTextField;
    private JTextField fileTextField;
    private JComboBox toCombobox;
    // Message content text area.
    private JTextArea contentTextArea;

    // Flag specifying whether or not dialog was cancelled.
    private boolean cancelled;

    // 생성자
    public MessageDialog(Frame parent, int type, Message message)
            throws Exception
    {
        // Call super constructor, specifying that dialog is modal.
        super(parent, true);

        //메시지 타입에 따른 타이틀 설정과 수신자, 제목, 내용 값을 얻음
        String to = "", subject = "", content = "", file= "";
        switch (type) {
            // 답장
            case REPLY:
                setTitle("Reply To Message");

                // 수신자
                Address[] senders = message.getFrom();
                if (senders != null || senders.length > 0) {
                    to = senders[0].toString();
                }
                to = message.getFrom()[0].toString();

                // 제목
                subject = message.getSubject();
                if (subject != null && subject.length() > 0) {
                    subject = "RE: " + subject;
                } else {
                    subject = "RE:";
                }

                // 메시지 내용(타입 접두사 붙임)
                content = "\n----------------- " +
                        "REPLIED TO MESSAGE" +
                        " -----------------\n" +
                        EmailClient.getMessageContent(message);
                break;

                // 첨부파일
                //file=message.getFileName();

            // 전달
            case FORWARD:
                setTitle("Forward Message");

                // 제목
                subject = message.getSubject();
                if (subject != null && subject.length() > 0) {
                    subject = "FWD: " + subject;
                } else {
                    subject = "FWD:";
                }

                //  메시지 내용(타입 접두사 붙임)
                content = "\n----------------- " +
                        "FORWARDED MESSAGE" +
                        " -----------------\n" +
                        EmailClient.getMessageContent(message);
                break;

            // 새 메시지
            default:
                setTitle("New Message");
        }

        // Handle closing events.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionCancel();
            }
        });

        // Setup fields panel.
        JPanel fieldsPanel = new JPanel();
        GridBagConstraints constraints;
        GridBagLayout layout = new GridBagLayout();
        fieldsPanel.setLayout(layout);

        JLabel fromLabel = new JLabel("From:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(fromLabel, constraints);
        fieldsPanel.add(fromLabel);
        fromTextField = new JTextField();
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(fromTextField, constraints);
        fieldsPanel.add(fromTextField);

        JLabel toLabel = new JLabel("To:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(toLabel, constraints);
        fieldsPanel.add(toLabel);
        if(to!=""){
            //답장인 경우
            toCombobox = new JComboBox();
            toCombobox.addItem(to);
            toCombobox.setEditable(false);  //사용자 입력 불가
        }
        else{
            toCombobox = new JComboBox(EmailClient.addressBook.toArray());
            toCombobox.setEditable(true);
            toCombobox.getEditor().setItem("");

        }
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 0);
        constraints.weightx = 1.0D;
        layout.setConstraints(toCombobox, constraints);
        fieldsPanel.add(toCombobox);

        JLabel subjectLabel = new JLabel("Subject:");
        constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 0);
        layout.setConstraints(subjectLabel, constraints);
        fieldsPanel.add(subjectLabel);
        subjectTextField = new JTextField(subject);
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 5, 0);
        layout.setConstraints(subjectTextField, constraints);
        fieldsPanel.add(subjectTextField);

        // 파일 첨부
        JLabel fileLabel = new JLabel("File:");
        constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 0);
        layout.setConstraints(fileLabel, constraints);
        fieldsPanel.add(fileLabel);
        fileTextField = new JTextField(37);
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 5, 0);
        layout.setConstraints(fileTextField, constraints);
        fieldsPanel.add(fileTextField);
        JButton findButton = new JButton("Find");
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth =GridBagConstraints.REMAINDER;;
        constraints.insets = new Insets(5, 5, 5, 0);
        layout.setConstraints(findButton, constraints);
        fieldsPanel.add(findButton);
        findButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String file=actionFind();
                fileTextField.setText(file);
            }
        });

        // Setup content panel.
        JScrollPane contentPanel = new JScrollPane();
        contentTextArea = new JTextArea(content, 10, 50);
        contentPanel.setViewportView(contentTextArea);

        // Setup buttons panel.
        JPanel buttonsPanel = new JPanel();
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionSend();
            }
        });
        buttonsPanel.add(sendButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionCancel();
            }
        });
        buttonsPanel.add(cancelButton);

        // Add panels to display.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(fieldsPanel, BorderLayout.NORTH);
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

        // Size dialog to components.
        pack();

        // Center dialog over application.
        setLocationRelativeTo(parent);
    }

    // Validate message fields and close dialog.
    private void actionSend() {
        if (fromTextField.getText().trim().length() < 1
                || toCombobox.getSelectedItem().toString().trim().length() < 1
                || subjectTextField.getText().trim().length() < 1
                || contentTextArea.getText().trim().length() < 1) {
            JOptionPane.showMessageDialog(this,
                    "One or more fields is missing.",
                    "Missing Field(s)", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Close dialog.
        dispose();
    }

    // Cancel creating this message and close dialog.
    private void actionCancel() {
        cancelled = true;

        // Close dialog.
        dispose();
    }

    // Show dialog.
    public boolean display() {
        show();

        // Return whether or not display was successful.
        return !cancelled;
    }

    private String actionFind() {
        JFileChooser chooser = new JFileChooser();
        //chooser.setCurrentDirectory(new File("C:/EOM"));

        int returnVal = chooser.showOpenDialog(null);

        //승인하면 선택한 파일을 가져오고 리턴한다.
        if(returnVal == JFileChooser.APPROVE_OPTION)
        {
            File f = chooser.getSelectedFile();
            return f.getAbsolutePath();
        }
        return "";
    }

    // 수신자 필드 접근자
    public String getFrom() {return fromTextField.getText();}

    // 제목 필드 접근자
    public String getTo() {return toCombobox.getSelectedItem().toString();}

    // 제목 필드 접근자
    public String getSubject() {return subjectTextField.getText();}

    // 파일 필드 접근자
    public String getFile() {return fileTextField.getText();}

    // 메시지 내용 필드 접근자
    public String getContent() {return contentTextArea.getText();}
}
