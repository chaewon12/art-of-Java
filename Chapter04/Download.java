import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.*;

// URL에서부터 파일을 다운로드 하는 클래스
class Download extends Observable implements Runnable {
    // 다운로드 버퍼의 최대값
    private static final int MAX_BUFFER_SIZE = 1024;

    // 상태 코드 이름
    public static final String STATUSES[] = {"Downloading",
            "Paused", "Complete", "Cancelled", "Error"};

    // 상태 코드
    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;

    private URL url; // 다운로드 할 URL
    private int size; // 다운로드 할 파일의 크기(bytes)
    private int downloaded; // 다운로드 한 파일의 크기(bytes)
    private int status; // 현재 다운로드 상태

    //Download 생성자
    public Download(URL url) {
        this.url = url;
        size = -1;
        downloaded = 0;
        status = DOWNLOADING;

        // 다운로드 시작
        download();
    }

    // 다운로드 할 URL 얻음
    public String getUrl() {
        return url.toString();
    }

    // 다운로드 할 파일 사이즈 얻음
    public int getSize() {
        return size;
    }

    // 다운로드의 진행률 얻음
    public float getProgress() {
        return ((float) downloaded / size) * 100;
    }

    // 다운로드 상태 얻음
    public int getStatus() {
        return status;
    }

    // 다운로드 정지
    public void pause() {
        status = PAUSED;
        stateChanged();
    }

    // 다운로드 재개
    public void resume() {
        status = DOWNLOADING;
        stateChanged();
        download();
    }

    // 다운로드 취소
    public void cancel() {
        status = CANCELLED;
        stateChanged();
    }

    // 다운로드 에러
    private void error() {
        status = ERROR;
        stateChanged();
    }

    //다운로드를 시작하거나 재개.
  /*자신을 호출한 객체의 참조를 스레드에 넘긴다.
  각 다운로드는 독립적으로 동작해야 하기 때문에 자신만의 스레드로 실행되어야 함.*/
    private void download() {
        Thread thread = new Thread(this);
        thread.start();
    }

    // URL에서 파일 이름 부분을 얻음
    private String getFileName(URL url) throws URISyntaxException {
        String filename = Paths.get(new URI(url.toString()).getPath())
                .getFileName().toString();
        filename=appendFileNum(filename,1);
        return filename;
    }

    // 중복된 파일이름 번호 추가
    public static String appendFileNum(String FileName, int seq){
        String newFileName="";

        //파일이 존재하는지 확인
        if(new File(FileName).exists()){
            int plusSeq = 1;
            String seqStr = "_" + seq;

            //파일 이름은 파일명.파일확장자로 구성되어 있다.
            String Name = FileName.substring(0,FileName.lastIndexOf("."));
            String Type = FileName.substring(FileName.lastIndexOf("."));

            // 만약 파일명_숫자인 경우 파일명_숫자+1
            if (FileName.lastIndexOf("_") != -1 & !Name.endsWith("_")) {
                // _와 .확장자 사이에 적힌 숫자를 추출
                String numStr = FileName.substring(
                        FileName.lastIndexOf("_") + 1,
                        FileName.lastIndexOf(Type));
                try {
                    plusSeq = Integer.parseInt(numStr);
                    plusSeq = plusSeq + 1;

                    newFileName = Name.substring(0,
                            Name.lastIndexOf("_"))
                            + "_" + plusSeq + Type;
                } catch (NumberFormatException e) {
                    newFileName = Name + seqStr + Type;
                    return appendFileNum(newFileName, ++plusSeq);
                }

            } else {
                newFileName = Name + seqStr + Type;
            }
            return appendFileNum(newFileName, ++plusSeq);
        } else {
            return FileName;
        }
    }


    // 파일을 다운로드함(스레드가 실행할 부분을 기술)
    @Override
    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;

        try {
            // URL 연결 작업
            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

            // 파일의 어느 부분을 다운로드 할건지 명세
            connection.setRequestProperty("Range",
                    "bytes=" + downloaded + "-");

            // 서버에 접속
            connection.connect();
            // 응답 코드가 200번대 있는지 확인
            if (connection.getResponseCode() / 100 != 2) {
                error();
            }

            // 유효한 content length인지 검사
            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                error();
            }

            //아직 다운로드에 대한 크기가 설정되지 않았으면 설정
            if (size == -1) {
                size = contentLength;
                stateChanged();
            }

            // 파일을 연 다음, 파일 포인터를 파일의 끝으로 이동
            file = new RandomAccessFile(getFileName(url), "rw");
            file.seek(downloaded);

            stream = connection.getInputStream();
            while (status == DOWNLOADING) {
        /* 다운로드할 부분이 얼마나 더 남아 있는지에 따라
           버퍼의 크기 조절 */
                byte buffer[];
                if (size - downloaded > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else {
                    buffer = new byte[size - downloaded];
                }

                // 서버에서부터 버퍼로 읽어옴
                int read = stream.read(buffer);
                if (read == -1)
                    break;

                // 버퍼의 내용을 파일에 씀
                file.write(buffer, 0, read);
                downloaded += read;
                stateChanged();
            }
      /* 이 지점에 도달하면 다운로드가 끝났음을 의미하므로
         상태값을 완료(complete)로 바꿈 */
            if (status == DOWNLOADING) {
                status = COMPLETE;
                stateChanged();
            }
        } catch (Exception e) {
            System.out.println(e);
            error();
        } finally {
            // Close file.
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {}
            }

            // Close connection to server.
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {}
            }
        }
    }

    // 다운로드의 상태가 변경되었음을 관찰자(observers)들에게 알림
    private void stateChanged() {
        setChanged();
        notifyObservers();
    }
}
