import java.awt.*;
import java.awt.event.*;
import java.sql.ResultSet;
import javax.swing.*;

/* This class displays a dialog for entering e-mail
   server connection settings. */
public class ConnectDialog extends JDialog
{
    // These are the e-mail server types.
    private static final String[] TYPES = {"pop3", "imap"};

    // Combo box for e-mail server types.
    private JComboBox typeComboBox;

    // Server, username and SMTP server text fields.
    private JTextField serverTextField, usernameTextField;
    private JTextField smtpServerTextField;

    // Password text field.
    private JPasswordField passwordField;

    // Constructor for dialog.
    public ConnectDialog(Frame parent)
    {
        // Call super constructor, specifying that dialog is modal.
        super(parent, true);

        // Set dialog title.
        setTitle("Connect");

        // Handle closing events.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionCancel();
            }
        });

        // Setup settings panel.
        JPanel settingsPanel = new JPanel();
        settingsPanel.setBorder(
                BorderFactory.createTitledBorder("Connection Settings"));
        GridBagConstraints constraints;
        GridBagLayout layout = new GridBagLayout();
        settingsPanel.setLayout(layout);
        JLabel typeLabel = new JLabel("Type:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(typeLabel, constraints);
        settingsPanel.add(typeLabel);
        typeComboBox = new JComboBox(TYPES);
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        constraints.weightx = 1.0D;
        layout.setConstraints(typeComboBox, constraints);
        settingsPanel.add(typeComboBox);
        JLabel serverLabel = new JLabel("Server:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(serverLabel, constraints);
        settingsPanel.add(serverLabel);
        serverTextField = new JTextField(25);
        constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        constraints.weightx = 1.0D;
        layout.setConstraints(serverTextField, constraints);
        settingsPanel.add(serverTextField);
        JLabel usernameLabel = new JLabel("Username:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(usernameLabel, constraints);
        settingsPanel.add(usernameLabel);
        usernameTextField = new JTextField();
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        constraints.weightx = 1.0D;
        layout.setConstraints(usernameTextField, constraints);
        settingsPanel.add(usernameTextField);
        JLabel passwordLabel = new JLabel("Password:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 5, 0);
        layout.setConstraints(passwordLabel, constraints);
        settingsPanel.add(passwordLabel);
        passwordField = new JPasswordField();
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.weightx = 1.0D;
        layout.setConstraints(passwordField, constraints);
        settingsPanel.add(passwordField);
        JLabel smtpServerLabel = new JLabel("SMTP Server:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 5, 0);
        layout.setConstraints(smtpServerLabel, constraints);
        settingsPanel.add(smtpServerLabel);
        smtpServerTextField = new JTextField(25);
        constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.weightx = 1.0D;
        layout.setConstraints(smtpServerTextField, constraints);
        settingsPanel.add(smtpServerTextField);

        //db에 저장된 연결 정보 입력
        selectConnInfo();

        // Setup buttons panel.
        JPanel buttonsPanel = new JPanel();
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionConnect();
            }
        });
        buttonsPanel.add(connectButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionCancel();
            }
        });
        buttonsPanel.add(cancelButton);

        // Add panels to display.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(settingsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

        // Size dialog to components.
        pack();

        // Center dialog over application.
        setLocationRelativeTo(parent);
    }

    // Validate connection settings and close dialog.
    private void actionConnect() {
        if (serverTextField.getText().trim().length() < 1
                || usernameTextField.getText().trim().length() < 1
                || passwordField.getPassword().length < 1
                || smtpServerTextField.getText().trim().length() < 1) {
            JOptionPane.showMessageDialog(this,
                    "One or more settings is missing.",
                    "Missing Setting(s)", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Close dialog.
        dispose();
    }

    private void selectConnInfo(){
        try{
            SQLiteControl control =new SQLiteControl();
            ResultSet result;

            String sql="SELECT * FROM connInfo ORDER BY ROWID DESC LIMIT 1";    //마지막 행 검색
            result=control.select(sql);

            if(result.next()){
                serverTextField.setText(result.getString("Server"));
                usernameTextField.setText(result.getString("Username"));
                passwordField.setText(result.getString("Password"));
                smtpServerTextField.setText(result.getString("SMTP"));
            }
            result.close();
            control.closeConnection();

        }catch (Exception e){
            System.out.println(e);
        }
    }
    // 연결을 취소하고 프로그램 종료
    private void actionCancel() {
        System.exit(0);
    }

    // 이메일 서버 타입 접근자
    public String getServerType() {return (String) typeComboBox.getSelectedItem();}

    // 이메일 서버 주소 접근자
    public String getServer() {
        return serverTextField.getText();
    }

    // 이메일 사용자 이름 접근자
    public String getUsername() {
        return usernameTextField.getText();
    }

    // 비밀번호 접근자
    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    // SMTP 서버 주소 접근자
    public String getSmtpServer() {
        return smtpServerTextField.getText();
    }
}
