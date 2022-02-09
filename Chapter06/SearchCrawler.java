import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.net.http.HttpResponse;
import java.nio.channels.ClosedByInterruptException;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.table.*;

// 검색 크롤러
public class SearchCrawler extends JFrame
{
    // 최대 URLs 개수 drop down 값
    private static final String[] MAX_URLS =
            {"50", "100", "500", "1000"};

    // 금지된 경로 목록
    private HashMap disallowListCache = new HashMap();

    // Search GUI controls.
    private JTextField startTextField;
    private JComboBox maxComboBox;
    private JCheckBox limitCheckBox;
    private JTextField logTextField;
    private JTextField searchTextField;
    private JCheckBox caseCheckBox;
    private JButton searchButton;

    // Search stats GUI controls.
    private JLabel crawlingLabel2;
    private JLabel crawledLabel2;
    private JLabel toCrawlLabel2;
    private JProgressBar progressBar;
    private JLabel matchesLabel2;

    // Table listing search matches.
    private JTable table;

    // 크롤링이 진행중인지 여부를 나타내는 플래그
    private boolean crawling;

    // 검색 결과를 로그 파일에 쓰기 위한 PrintWriter
    private PrintWriter logFileWriter;

    // 생성자
    public SearchCrawler()
    {
        // Set application title.
        setTitle("Search Crawler");

        // Set window size.
        setSize(600, 600);

        // Handle window closing events.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionExit();
            }
        });

        // Set up file menu.
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

        // Set up search panel.
        JPanel searchPanel = new JPanel();
        GridBagConstraints constraints;
        GridBagLayout layout = new GridBagLayout();
        searchPanel.setLayout(layout);

        JLabel startLabel = new JLabel("Start URL:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(startLabel, constraints);
        searchPanel.add(startLabel);

        startTextField = new JTextField();
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        layout.setConstraints(startTextField, constraints);
        searchPanel.add(startTextField);

        JLabel maxLabel = new JLabel("Max URLs to Crawl:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(maxLabel, constraints);
        searchPanel.add(maxLabel);

        maxComboBox = new JComboBox(MAX_URLS);
        maxComboBox.setEditable(true);
        constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(maxComboBox, constraints);
        searchPanel.add(maxComboBox);

        limitCheckBox =
                new JCheckBox("Limit crawling to Start URL site");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 10, 0, 0);
        layout.setConstraints(limitCheckBox, constraints);
        searchPanel.add(limitCheckBox);

        JLabel blankLabel = new JLabel();
        constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(blankLabel, constraints);
        searchPanel.add(blankLabel);

        JLabel logLabel = new JLabel("Matches Log File:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(logLabel, constraints);
        searchPanel.add(logLabel);

        String file =
                System.getProperty("user.dir") +
                        System.getProperty("file.separator") +
                        "crawler.log";
        logTextField = new JTextField(file);
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        layout.setConstraints(logTextField, constraints);
        searchPanel.add(logTextField);

        JLabel searchLabel = new JLabel("Search String:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(searchLabel, constraints);
        searchPanel.add(searchLabel);

        searchTextField = new JTextField();
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 0, 0);
        constraints.gridwidth= 2;
        constraints.weightx = 1.0d;
        layout.setConstraints(searchTextField, constraints);
        searchPanel.add(searchTextField);

        caseCheckBox = new JCheckBox("Case Sensitive");
        constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 0, 5);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(caseCheckBox, constraints);
        searchPanel.add(caseCheckBox);

        searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionSearch();
            }
        });
        constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 5, 5);
        layout.setConstraints(searchButton, constraints);
        searchPanel.add(searchButton);

        JSeparator separator = new JSeparator();
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 5, 5);
        layout.setConstraints(separator, constraints);
        searchPanel.add(separator);

        JLabel crawlingLabel1 = new JLabel("Crawling:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(crawlingLabel1, constraints);
        searchPanel.add(crawlingLabel1);

        crawlingLabel2 = new JLabel();
        crawlingLabel2.setFont(
                crawlingLabel2.getFont().deriveFont(Font.PLAIN));
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        layout.setConstraints(crawlingLabel2, constraints);
        searchPanel.add(crawlingLabel2);

        JLabel crawledLabel1 = new JLabel("Crawled URLs:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(crawledLabel1, constraints);
        searchPanel.add(crawledLabel1);

        crawledLabel2 = new JLabel();
        crawledLabel2.setFont(
                crawledLabel2.getFont().deriveFont(Font.PLAIN));
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        layout.setConstraints(crawledLabel2, constraints);
        searchPanel.add(crawledLabel2);

        JLabel toCrawlLabel1 = new JLabel("URLs to Crawl:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(toCrawlLabel1, constraints);
        searchPanel.add(toCrawlLabel1);

        toCrawlLabel2 = new JLabel();
        toCrawlLabel2.setFont(
                toCrawlLabel2.getFont().deriveFont(Font.PLAIN));
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        layout.setConstraints(toCrawlLabel2, constraints);
        searchPanel.add(toCrawlLabel2);

        JLabel progressLabel = new JLabel("Crawling Progress:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 0, 0);
        layout.setConstraints(progressLabel, constraints);
        searchPanel.add(progressLabel);

        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setStringPainted(true);
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 0, 5);
        layout.setConstraints(progressBar, constraints);
        searchPanel.add(progressBar);

        JLabel matchesLabel1 = new JLabel("Search Matches:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 10, 0);
        layout.setConstraints(matchesLabel1, constraints);
        searchPanel.add(matchesLabel1);

        matchesLabel2 = new JLabel();
        matchesLabel2.setFont(
                matchesLabel2.getFont().deriveFont(Font.PLAIN));
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(5, 5, 10, 5);
        layout.setConstraints(matchesLabel2, constraints);
        searchPanel.add(matchesLabel2);

        // 검색 결과 테이블 설정
        table =
                new JTable(new DefaultTableModel(new Object[][]{},
                        new String[]{"URL"}) {
                    public boolean isCellEditable(int row, int column)
                    {
                        return false;
                    }
                });

        // 검색 결과 판넬 설정
        JPanel matchesPanel = new JPanel();
        matchesPanel.setBorder(
                BorderFactory.createTitledBorder("Matches"));
        matchesPanel.setLayout(new BorderLayout());
        matchesPanel.add(new JScrollPane(table),
                BorderLayout.CENTER);

        // Add panels to display.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(searchPanel, BorderLayout.NORTH);
        getContentPane().add(matchesPanel, BorderLayout.CENTER);
    }

    // 프로그램 종료
    private void actionExit() {
        System.exit(0);
    }

    // 검색/정지 버튼 클릭
    private void actionSearch() {
        // 정지 버튼이 클릭되면 crawling 플래그를 끈다.
        if (crawling) {
            crawling = false;
            return;
        }

        ArrayList errorList = new ArrayList();

        // 시작 페이지가 입력되었는지 확인
        String startUrl = startTextField.getText().trim();
        if (startUrl.length() < 1) {
            errorList.add("Missing Start URL.");
        }
        // 시작 페이지 URL 검증
        else if (verifyUrl(startUrl) == null) {
            errorList.add("Invalid Start URL.");
        }

        // 최대 크롤링할 URL의 개수 확인
        int maxUrls = 0;
        String max = ((String) maxComboBox.getSelectedItem()).trim();
        if (max.length() > 0) {
            try {
                maxUrls = Integer.parseInt(max);
            } catch (NumberFormatException e) {
            }
            if (maxUrls < 1) {
                errorList.add("Invalid Max URLs value.");
            }
        }

        // 검색 결과 로그 파일이 입력되었는지 확인
        String logFile = logTextField.getText().trim();
        if (logFile.length() < 1) {
            errorList.add("Missing Matches Log File.");
        }

        // 검색어가 입력되었는지 확인
        String searchString = searchTextField.getText().trim();
        if (searchString.length() < 1) {
            errorList.add("Missing Search String.");
        }

        // 에러가 있다면 출력하고 리턴
        if (errorList.size() > 0) {
            StringBuffer message = new StringBuffer();

            // 발생한 에러들을 하나의 메시지로 합친다
            for (int i = 0; i < errorList.size(); i++) {
                message.append(errorList.get(i));
                if (i + 1 < errorList.size()) {
                    message.append("\n");
                }
            }

            showError(message.toString());
            return;
        }

        // 시작페이지 URL에서 "www" 문자열 제거
        //startUrl = removeWwwFromUrl(startUrl);

        // 검색 크롤러 시작
        search(logFile, startUrl, maxUrls, searchString);
    }

    private void search(final String logFile, final String startUrl,
                        final int maxUrls, final String searchString)
    {
        // 새 스레드를 생성하여 검색 수행
        Thread thread = new Thread(new Runnable() {
            public void run() {
                // 크롤링이 수행되는 동안 모래시계로 커서 변경
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                // 검색 관련 컨트롤 비활성화
                startTextField.setEnabled(false);
                maxComboBox.setEnabled(false);
                limitCheckBox.setEnabled(false);
                logTextField.setEnabled(false);
                searchTextField.setEnabled(false);
                caseCheckBox.setEnabled(false);

                //검색 버튼 텍스트를 "Stop."으로 변경
                searchButton.setText("Stop");

                // 검색 상태 리셋
                table.setModel(new DefaultTableModel(new Object[][]{},
                        new String[]{"URL"}) {
                    public boolean isCellEditable(int row, int column)
                    {
                        return false;
                    }
                });
                updateStats(startUrl, 0, 0, maxUrls);

                // 검색 결과 로그 파일을 연다
                try {
                    logFileWriter = new PrintWriter(new FileWriter(logFile));
                } catch (Exception e) {
                    showError("Unable to open matches log file.");
                    return;
                }

                // crawling 플래그를 켠다
                crawling = true;

                // 실제로 크롤링을 수행
                crawl(startUrl, maxUrls, limitCheckBox.isSelected(),
                        searchString, caseCheckBox.isSelected());

                // crawling 플래그를 끈다(종료)
                crawling = false;

                // 검색 결과 로그 파일을 닫는다
                try {
                    logFileWriter.close();
                } catch (Exception e) {
                    showError("Unable to close matches log file.");
                }

                // 검색 완료 표시
                crawlingLabel2.setText("Done");

                // 컴색 관련 컨트롤 활성화
                startTextField.setEnabled(true);
                maxComboBox.setEnabled(true);
                limitCheckBox.setEnabled(true);
                logTextField.setEnabled(true);
                searchTextField.setEnabled(true);
                caseCheckBox.setEnabled(true);

                // 검색 버튼의 텍스트를 "Search."로 설정
                searchButton.setText("Search");

                // 기본 커서로 되돌린다
                setCursor(Cursor.getDefaultCursor());

                // 검색 결과가 하나도 나오지 않았음을 알린다
                if (table.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(SearchCrawler.this,
                            "Your Search String was not found. Please try another.",
                            "Search String Not Found",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        thread.start();
    }

    // 에러 출력
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    // 검색 상태 갱신 메소드
    private void updateStats(
            String crawling, int crawled, int toCrawl, int maxUrls)
    {
        crawlingLabel2.setText(crawling);
        crawledLabel2.setText("" + crawled);
        toCrawlLabel2.setText("" + toCrawl);

        // 프로그래스 바 갱신
        if (maxUrls == -1) {
            progressBar.setMaximum(crawled + toCrawl);
        } else {
            progressBar.setMaximum(maxUrls);
        }
        progressBar.setValue(crawled);

        matchesLabel2.setText("" + table.getRowCount());
    }

    // 검색 결과를 테이블과 로그 파일에 추가
    private void addMatch(String url) {
        // 검색 결과 URL을 테이블에 추가
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        for(int i=0;i<model.getRowCount() ;i++){
            if(url.equals(model.getValueAt(i,0))){
               return;
            }
        }
        model.addRow(new Object[]{url});
        // Add URL to matches log file.
        try {
            logFileWriter.println(url);
        } catch (Exception e) {
            showError("Unable to log match.");
        }
    }

    // URL 포멧 검증 메소드
    private URL verifyUrl(String url) {
        // HTTP URL만 허용

        if (!url.toLowerCase().startsWith("https://")&&!url.toLowerCase().startsWith("http://"))
            return null;

        // URL 포멧 검증
        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(url);
        } catch (Exception e) {
            return null;
        }

        return verifiedUrl;
    }

    // 해당 URL에 대해 로봇의 접근이 허락되는지 검사
    private boolean isRobotAllowed(URL urlToCheck) {
        String host = urlToCheck.getHost().toLowerCase();

        // 해당 호스트에 대한 금지 경로 목록이 이미 저장되어 있는지 검사
        ArrayList disallowList =
                (ArrayList) disallowListCache.get(host);

        // 금지 경로 목록이 없다면 다운로드하여 저장
        if (disallowList == null) {
            disallowList = new ArrayList();

            try {
                URL robotsFileUrl =
                        new URL("http://" + host + "/robots.txt");

                // 로봇 파일을 연다
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(
                                robotsFileUrl.openStream()));

                // 로봇 파일을 읽어서 금지 경로 목록을 만든다
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.indexOf("Disallow:") == 0) {
                        String disallowPath =
                                line.substring("Disallow:".length());

                        // 현재 금지 경로에 주석이 포함되어 있다면 제거한다
                        int commentIndex = disallowPath.indexOf("#");
                        if (commentIndex != - 1) {
                            disallowPath =
                                    disallowPath.substring(0, commentIndex);
                        }

                        // 공백 문자 제거
                        disallowPath = disallowPath.trim();

                        // 금지 목록에 경로 추가
                        disallowList.add(disallowPath);
                    }
                }

                // 호스트별 금지 경로 목록에 저장장
               disallowListCache.put(host, disallowList);
            }
            catch (Exception e) {
                //로봇 파일이 없을 때 예외가 발생하므로 접근이 허락된 것으로 가정
                return true;
            }
        }

        //금지 경로 목록을 순회하면서 해당 URL에 대한 접근이 금지되었는지 검사
        String file = urlToCheck.getFile();
        for (int i = 0; i < disallowList.size(); i++) {
            String disallow = (String) disallowList.get(i);
            if (file.startsWith(disallow)) {
                return false;
            }
        }

        return true;
    }

    // 해당 URL에 대한 페이지를 다운로드
    private String downloadPage(URL pageUrl) {
        try {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(
                            pageUrl.openStream()));

            // 버퍼로 페이지 읽어들임
            String line;
            StringBuffer pageBuffer = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                pageBuffer.append(line);
            }

            return pageBuffer.toString();
        } catch (Exception e) {
        }

        return null;
    }

    //리다이렉트 url 지원
    private URL getFinalURL(URL url){
        try{
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setInstanceFollowRedirects(false);  //리다이렉트 따라가지 않음
            con.connect();

            int resCode = con.getResponseCode();
            // 301(영구이동), 302(임시 이동), 303(기타 위치 보기)이면 재귀 호출로 따라감
            if(resCode==303||resCode==302||resCode==303){
                String location = con.getHeaderField("Location");
                if (location.startsWith("/")) {
                    location = url.getProtocol() + "://" + url.getHost() + location;
                }
                return getFinalURL(new URL(location));
            }
        }catch(Exception e){
            System.out.println(e);
        }
        return url;
    }

    // "www" 문자열 제거 메소드
    private String removeWwwFromUrl(String url) {
        int index = url.indexOf("://www.");
        if (index != -1) {
            return url.substring(0, index + 3) +
                    url.substring(index + 7);
        }

        return (url);
    }

    // 페이지 내용을 파싱하여 링크 정보를 가져오는 메소드
    private ArrayList retrieveLinks(
            URL pageUrl, String pageContents, Set crawledList,
            boolean limitHost)
    {
        // 링크를 나타내는 정규식 패턴 명세
        Pattern p =
                Pattern.compile("<a\\s+href\\s*=\\s*\"?(.*?)[\"|>]",
                        Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(pageContents);

        // 링크 리스트를 만든다
        ArrayList linkList = new ArrayList();
        while (m.find()) {
            String link = m.group(1).trim();

            // 빈 링크 무시
            if (link.length() < 1) {
                continue;
            }

            // 페이지 앵커로만 구성된 링크(페이지 내에서 디옹하는데 사용되는 링크) 무시
            if (link.charAt(0) == '#') {
                continue;
            }

            // 메일 링크 무시
            if (link.indexOf("mailto:") != -1) {
                continue;
            }

            // 자바 스크립트 링크 무시
            if (link.toLowerCase().indexOf("javascript") != -1) {
                continue;
            }

            // 절대 경로 및 상대 경로를 완전한 링크로 바꾼다
            if (link.indexOf("://") == -1) {
                // 절대 경로 처리
                if (link.charAt(0) == '/') {
                    link = "http://" + pageUrl.getHost() + link;
                    // 상대 경로 처리
                } else {
                    String file = pageUrl.getFile();
                    if (file.indexOf('/') == -1) {
                        link = "http://" + pageUrl.getHost() + "/" + link;
                    } else {
                        String path =
                                file.substring(0, file.lastIndexOf('/') + 1);
                        link = "http://" + pageUrl.getHost() + path + link;
                    }
                }
            }

            // 링크에서 앵커를 제거
            int index = link.indexOf('#');
            if (index != -1) {
                link = link.substring(0, index);
            }

            // 링크에서 "www" 문자열 제거
            //link = removeWwwFromUrl(link);

            // 링크를 검사하고 유효하지 않다면 무시
            URL verifiedLink = verifyUrl(link);
            if (verifiedLink == null) {
                continue;
            }

            // 검색 범위를 시작 페이지로 제한하는 경우
            if (limitHost &&
                    !pageUrl.getHost().toLowerCase().equals(
                            verifiedLink.getHost().toLowerCase()))
            {
                continue;
            }

            // 이미 크롤링된 링크라면 무시
            if (crawledList.contains(link)) {
                continue;
            }

            //리스트에 링크 추가
            linkList.add(link);
        }

        return (linkList);
    }

    // 해당 페이지 내용에 사용자가 입력한 검색어가 있는지 검사하는 메소드
    private boolean searchStringMatches(
            String pageContents, String searchString,
            boolean caseSensitive)
    {
        String searchContents = pageContents;

        // 대소문자를 구분하지 않는 경우 소문자로 변경
        if (!caseSensitive) {
            searchContents = pageContents.toLowerCase();
        }

        // 검색어(문장)를 각각의 단어들로 나눈다
        Pattern p = Pattern.compile("[\\s]+");
        String[] terms = p.split(searchString);

        // 각 단어에 해당하는 검색 결과가 있는지 검사
        for (int i = 0; i < terms.length; i++) {
            if (caseSensitive) {
                if (searchContents.indexOf(terms[i]) == -1) {
                    return false;
                }
            } else {
                if (searchContents.indexOf(terms[i].toLowerCase()) == -1) {
                    return false;
                }
            }
        }

        return true;
    }

    // 입력된 검색어에 대해 실제로 크롤링을 수행하는 메소드
    public void crawl(
            String startUrl, int maxUrls, boolean limitHost,
            String searchString, boolean caseSensitive)
    {
        // 크롤링 작업에 필요한 리스트 설정
        Set crawledList = Collections.synchronizedSet(new HashSet());
        Set toCrawlList =Collections.synchronizedSet(new LinkedHashSet());

        // 크롤링할 리스트에 시작 페이지 URL 추가
       toCrawlList.add(startUrl);

       // 크롤링 스레드
        Runnable crawlingTask =new Runnable() {
           @Override
            public void run() {
               while (crawling && toCrawlList.size() > 0){
                   //크롤링할 최대 URL 개수에 도달했는지 검사
                   if(maxUrls!=-1){
                       if(crawledList.size()>=maxUrls){
                           break;
                       }
                   }

                   // 크롤링할 리스트에서 현재 처리할 URL 얻음
                   String url = (String) toCrawlList.iterator().next();

                   // 크롤링할 리스트에서 URL 제거
                   toCrawlList.remove(url);

                   //리다이렉트 URL 지원
                   try {
                       URLConnection con = null;
                       con = new URL(url).openConnection();
                       url = getFinalURL(con.getURL()).toString();
                   } catch (IOException e) {
                       e.printStackTrace();
                   }

                   //스트링 url을 URL 객체로 변환
                   URL verifiedUrl = verifyUrl(url);

                   // URL이 금지된 경로라면 처리하지 않는다.
                   if (!isRobotAllowed(verifiedUrl)) {
                       return ;
                   }

                   // 검색 상태 갱신
                   updateStats(url, crawledList.size(), toCrawlList.size(), maxUrls);

                   // 크롤링된 목록에 추가
                   crawledList.add(url);

                   // url이 가리키는 페이지를 다운로드
                   String pageContents = downloadPage(verifiedUrl);

                   /* 페이지 다운로드가 성공했다면, 해당 페이지의 모든 링크를 뽑아내고
                페이지의 내용이 검색어를 포함하는지 검사한다.*/
                   if (pageContents != null && pageContents.length() > 0)
                   {
                       // 페이지에서 유효한 링크 목록을 뽑는다
                       ArrayList links =
                               retrieveLinks(verifiedUrl, pageContents, crawledList,
                                       limitHost);

                       // 크롤링할 리스트에 링크 추가
                       toCrawlList.addAll(links);

                       // 페이지에 검색어가 있다면 검색 결과에 추가
                       if (searchStringMatches(pageContents, searchString,
                               caseSensitive))
                       {
                           addMatch(url);
                       }
                   }
                   // 검색 상태 갱신
                   updateStats(url, crawledList.size(), toCrawlList.size(),
                           maxUrls);
               }

            }
        };

        // 스레드 주소를 저장할 배열 선언
        int threadCnt=10;
        Thread thread[] = new Thread[threadCnt];

        while(crawledList.size()< maxUrls){
            for(int i=0; i<threadCnt;i++){
                thread[i] = new Thread(crawlingTask);
                thread[i].start();
            }
            // 다른 스레드 기다리도록 동기화
            for (int i=0; i<threadCnt;i++) {
                try {
                    thread[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // 검색 크롤러의 메인 매소드
    public static void main(String[] args) {
        SearchCrawler crawler = new SearchCrawler();
        crawler.show();
    }
}
