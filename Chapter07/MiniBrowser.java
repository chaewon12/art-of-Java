import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

// 미니 브라우저
public class MiniBrowser extends JFrame
        implements HyperlinkListener
{
    // 페이지 리스트 내를 반복할 버튼들
    private JButton backButton, forwardButton;

    // 페이지 위치 텍스트 필드
    private JTextField locationTextField;

    // 페이지를 출력하기 위한 Editor pane
    private JEditorPane displayEditorPane;

    // 브라우저가 방문한 페이지 리스트
    private ArrayList pageList = new ArrayList();

    // 생성자
    public MiniBrowser()
    {
        // 제목 설정
        super("Mini Browser");

        // 윈도우 크기 설정
        setSize(640, 480);

        // closing 이벤트 처리
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionExit();
            }
        });

        // file 메뉴 설정
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

        // button 패널 설정
        JPanel buttonPanel = new JPanel();
        backButton = new JButton("< Back");
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionBack();
            }
        });
        backButton.setEnabled(false);
        buttonPanel.add(backButton);
        forwardButton = new JButton("Forward >");
        forwardButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionForward();
            }
        });
        forwardButton.setEnabled(false);
        buttonPanel.add(forwardButton);
        locationTextField = new JTextField(35);
        locationTextField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    actionGo();
                }
            }
        });
        buttonPanel.add(locationTextField);
        JButton goButton = new JButton("GO");
        goButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionGo();
            }
        });
        buttonPanel.add(goButton);

        // 페이지 출력 설정
        displayEditorPane = new JEditorPane();
        displayEditorPane.setContentType("text/html");
        displayEditorPane.setEditable(false);
        displayEditorPane.addHyperlinkListener(this);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buttonPanel, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(displayEditorPane),
                BorderLayout.CENTER);
    }

    // 프로그램 종료
    private void actionExit() {
        System.exit(0);
    }

    // 이전 페이지로 이동
    private void actionBack() {
        URL currentUrl = displayEditorPane.getPage();
        int pageIndex = pageList.indexOf(currentUrl.toString());
        try {
            showPage(
                    new URL((String) pageList.get(pageIndex - 1)), false);
        }
        catch (Exception e) {}
    }

    // 다음 페이지로 이동
    private void actionForward() {
        URL currentUrl = displayEditorPane.getPage();
        int pageIndex = pageList.indexOf(currentUrl.toString());
        try {
            showPage(
                    new URL((String) pageList.get(pageIndex + 1)), false);
        }
        catch (Exception e) {}
    }

    // 위치 텍스트 필드에서 지정한 페이지를 보여줌
    private void actionGo() {
        URL verifiedUrl = verifyUrl(locationTextField.getText());
        if (verifiedUrl != null) {
            showPage(verifiedUrl, true);
        } else {
            showError("Invalid URL");
        }
    }

    // 대화상자에 에러 메세지 출력
    private void showError(String errorMessage) {
        JOptionPane.showMessageDialog(this, errorMessage,
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    // URL 형식 검사
    private URL verifyUrl(String url) {
        //HTTPS URL만을 허용
        if (!url.toLowerCase().startsWith("https://"))
            return null;

        //URL의 형식 검증
        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(url);
        } catch (Exception e) {
            return null;
        }

        return verifiedUrl;
    }

    /*지정한 페이지를 보여주고,
    * 필요하면 이 페이지를 페이지 리스트에 추가함 */
    private void showPage(URL pageUrl, boolean addToList)
    {
        // 웹페이지 검색(crawling)이 진행중일 때 모래시계를 보여줌
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            // 현재 표시중인 페이지의 URL을 얻음
            URL currentUrl = displayEditorPane.getPage();
            // 지정한 페이지를 띄우고 표시함
            displayEditorPane.setPage(pageUrl);
            // 새롭게 표시될 페이지의 URL을 얻음
            //URL newUrl = displayEditorPane.getPage();

            // 필요하다면 페이지를 리스트에 추가
            if (addToList) {
                int listSize = pageList.size();
                if (listSize > 0) {
                    int pageIndex =
                            pageList.indexOf(currentUrl.toString());
                    if (pageIndex < listSize - 1) {
                        for (int i = listSize - 1; i > pageIndex; i--) {
                            pageList.remove(i);
                        }
                    }
                }
                pageList.add(pageUrl.toString());
            }
            // 현재 페이지의 URL로 위치 텍스트 필드가 갱신됨
            locationTextField.setText(pageUrl.toString());
            // 출력될 페이지에 기반해 버튼들이 갱신됨
            updateButtons(pageUrl);
        } catch (Exception e) {
            showError("Unable to load page");
        } finally {
            // 기본 커서로 돌아감
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /* 표시될 페이지에 기반해서
    * back 버튼과 forward 버튼의 상태를 갱신*/
    private void updateButtons(URL pageUrl) {
        if (pageList.size() < 2) {
            backButton.setEnabled(false);
            forwardButton.setEnabled(false);
        } else {
            String currentUrl =pageUrl.toString();
            int pageIndex = pageList.indexOf(currentUrl);
            backButton.setEnabled(pageIndex > 0);
            forwardButton.setEnabled(
                    pageIndex < (pageList.size() - 1));
        }
    }

    // 클릭된 하이퍼링크들을 처리
    public void hyperlinkUpdate(HyperlinkEvent event) {
        HyperlinkEvent.EventType eventType = event.getEventType();
        if (eventType == HyperlinkEvent.EventType.ACTIVATED) {
            if (event instanceof HTMLFrameHyperlinkEvent) {
                HTMLFrameHyperlinkEvent linkEvent =
                        (HTMLFrameHyperlinkEvent) event;
                HTMLDocument document =
                        (HTMLDocument) displayEditorPane.getDocument();
                document.processHTMLFrameHyperlinkEvent(linkEvent);
            } else {
                showPage(event.getURL(), true);
            }
        }
    }

    // 미니 브라우저 실행
    public static void main(String[] args) {
        MiniBrowser browser = new MiniBrowser();
        browser.show();
    }
}
