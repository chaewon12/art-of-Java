// A Small Basic Interpreter.

import java.io.*;
import java.util.*;

// Exception class for interpreter errors.
class InterpreterException extends Exception {
    String errStr; // describes the error

    public InterpreterException(String str) {
        errStr = str;
    }

    public String toString() {
        return errStr;
    }
}

// The Small Basic interpreter.
class SBasic {
    final int PROG_SIZE = 10000; // maximum program size

    // 토큰 타입
    final int NONE = 0;
    final int DELIMITER = 1;    //연산자, 괄호
    final int VARIABLE = 2;     //변수
    final int NUMBER = 3;       //숫자
    final int COMMAND = 4;      //키워드
    final int QUOTEDSTR = 5;    //인용부호가 있는 문자열

    // 에러 타입
    final int SYNTAX = 0;
    final int UNBALPARENS = 1;
    final int NOEXP = 2;
    final int DIVBYZERO = 3;
    final int EQUALEXPECTED = 4;
    final int NOTVAR = 5;
    final int LABELTABLEFULL = 6;
    final int DUPLABEL = 7;
    final int UNDEFLABEL = 8;
    final int THENEXPECTED = 9;
    final int TOEXPECTED = 10;
    final int NEXTWITHOUTFOR = 11;
    final int RETURNWITHOUTGOSUB = 12;
    final int MISSINGQUOTE = 13;
    final int FILENOTFOUND = 14;
    final int FILEIOERROR = 15;
    final int INPUTIOERROR = 16;

    // Small Basic 키워드 내부 표현
    final int UNKNCOM = 0;
    final int PRINT = 1;
    final int INPUT = 2;
    final int IF = 3;
    final int THEN = 4;
    final int FOR = 5;
    final int NEXT = 6;
    final int TO = 7;
    final int GOTO = 8;
    final int GOSUB = 9;
    final int RETURN = 10;
    final int END = 11;
    final int EOL = 12;
    final int REPEAT=13;
    final int UNTIL=14;

    // 프로그램의 끝
    final String EOP = "\0";

    // 이중연산자(<=) 내부 표현
    final char LE = 1;
    final char GE = 2;
    final char NE = 3;

    // 변수를 위한 배열
    private double vars[];

    // 키워드들을 키워드 토큰과 연결시키는 클래스
    class Keyword {
        String keyword; // 문자열 형태(소문자)
        int keywordTok; // 키워드 내부 표현

        Keyword(String str, int t) {
            keyword = str;
            keywordTok = t;
        }
    }

    /* 내부 표현을 포함한 키워드 테이블 생성
     모든 키워드는 소문자로 저장함  */
    Keyword kwTable[] = {
            new Keyword("print", PRINT),
            new Keyword("input", INPUT),
            new Keyword("if", IF),
            new Keyword("then", THEN),
            new Keyword("goto", GOTO),
            new Keyword("for", FOR),
            new Keyword("next", NEXT),
            new Keyword("to", TO),
            new Keyword("gosub", GOSUB),
            new Keyword("return", RETURN),
            new Keyword("end", END),
            new Keyword("repeat",REPEAT),
            new Keyword("until",UNTIL)
    };

    private char[] prog; // 프로그램 배열 참조
    private int progIdx; // 현재 프로그램 인덱스(인터프리터가 작업중인 위치)

    private String token; // 현재 토큰의 문자열 버전
    private int tokType;  // 토큰 타입

    private int kwToken; // 키워드의 내부 표현

    // ForInfo 객체에 FOR 루프의 정보를 저장
    class ForInfo {
        int var; // 카운터 변수
        double target; // 목표값
        int loc; // 루프에 대한 소스 내 인덱스
    }
    // FOR 루프를 위한 스택( 중접 For 루프 사용을 지원한다)
    private Stack fStack;

    // REPEAT 루프 정보 저장
    class ReInfo {
        int loc; // 루프에 대한 소스 내 인덱스
    }
    // REPEAT 를 위한 스택
    private Stack rStack;

    // 레이블 항목 정의
    class Label {
        String name; // 레이블
        int loc; // 소스 파일 내에서 레이블의 위치
        public Label(String n, int i) {
            name = n;
            loc = i;
        }
    }

    // 레이블들에 대한 매핑
    private TreeMap labelTable;

    // gosubs를 위한 스택
    private Stack gStack;

    // 관계연산자
    char rops[] = {
            GE, NE, LE, '<', '>', '=', 0
    };

    /* 보다 편리하게 확인하기 위해 관계연산자를 포함하는 문자열 생성 */
    String relops = new String(rops);

    // Small Basic 생성자
    public SBasic(String progName)
            throws InterpreterException {

        char tempbuf[] = new char[PROG_SIZE];
        int size;

        // 프로그램을 메모리에 읽어들임
        size = loadProgram(tempbuf, progName);

        if(size != -1) {
            // 프로그램을 저장할 적당한 크기의 배열 생성
            prog = new char[size];

            // 프로그램을 프로그램 배열(prog)로 복사
            System.arraycopy(tempbuf, 0, prog, 0, size);
        }
    }

    // 프로그램을 메모리에 읽어들임
    private int loadProgram(char[] p, String fname)
            throws InterpreterException
    {
        int size = 0;

        try {
            FileReader fr = new FileReader(fname);

            BufferedReader br = new BufferedReader(fr);

            size = br.read(p, 0, PROG_SIZE);

            fr.close();
        } catch(FileNotFoundException exc) {
            handleErr(FILENOTFOUND);
        } catch(IOException exc) {
            handleErr(FILEIOERROR);
        }

        // If file ends with an EOF mark, backup.
        if(p[size-1] == (char) 26) size--;

        return size; // 프로그램 크기 리턴
    }

    // 프로그램 실행
    public void run() throws InterpreterException {

        // 새 프로그램 실행을 위한 초기화
        vars = new double[26];
        fStack = new Stack();
        rStack=new Stack();
        labelTable = new TreeMap();
        gStack = new Stack();
        progIdx = 0;

        scanLabels(); // 프로그램 내에서 모든 레이블 검색

        sbInterp(); // 인터프리터 실행

    }

    // Small Basic 인터프리터의 진입점
    private void sbInterp() throws InterpreterException
    {

        // 인터프리터의 메인 루프
        do {
            getToken();
            // 할당 구문 검사
            if(tokType==VARIABLE) {
                putBack(); // 입력스트림에 var 리턴
                assignment(); // 할당 구문 처리
            }
            else // 키워드
                switch(kwToken) {
                    case PRINT:
                        print();
                        break;
                    case GOTO:
                        execGoto();
                        break;
                    case IF:
                        execIf();
                        break;
                    case FOR:
                        execFor();
                        break;
                    case NEXT:
                        next();
                        break;
                    case INPUT:
                        input();
                        break;
                    case GOSUB:
                        gosub();
                        break;
                    case RETURN:
                        greturn();
                        break;
                    case END:
                        return; //sbInterp() 종료
                    case REPEAT:
                        repeat();
                        break;
                    case UNTIL:
                        until();
                        break;
                }
        } while (!token.equals(EOP));
    }

    // 모든 레이블 검색
    //레이블의 위치가 labelTable 맵에 저장됨. 실행에 앞서 모든 레이블을 찾음으로써 실행 속도 개선
    private void scanLabels() throws InterpreterException
    {
        int i;
        Object result;

        // 파일의 첫번째 토큰이 레이블(숫자)인지 검사
        getToken();
        if(tokType==NUMBER)
            labelTable.put(token, new Integer(progIdx));

        findEOL();

        do {
            getToken();
            /*TreeMap의 put() 메소드는 키에 대한 이전 매핑의 참조를 리턴하거나,
              이전 매핑이 없으면 null을 리턴한다*/
            if(tokType==NUMBER) {// 줄 번호
                result = labelTable.put(token,
                        new Integer(progIdx));
                if(result != null)
                    handleErr(DUPLABEL);
            }

            // 공백 줄이 아니면 다음 줄 검색
            if(kwToken != EOL) findEOL();
        } while(!token.equals(EOP));
        progIdx = 0; // reset index to start of program
    }

    // 다음 줄의 시작점 검색
    private void findEOL()
    {
        while(progIdx < prog.length &&
                prog[progIdx] != '\n') ++progIdx;
        if(progIdx < prog.length) progIdx++;
    }

    // 변수에 값 할당
    private void assignment() throws InterpreterException
    {
        int var;
        double value;
        char vname;

        // 변수 이름 얻음
        getToken();
        vname = token.charAt(0);

        if(!Character.isLetter(vname)) {
            handleErr(NOTVAR);
            return;
        }

        var = (int) Character.toUpperCase(vname) - 'A';

        // 등호를 얻음
        getToken();
        if(!token.equals("=")) {
            handleErr(EQUALEXPECTED);
            return;
        }

        // 할당할 값을 얻음
        value = evaluate();

        // 값 할당
        vars[var] = value;
    }

    //  PRINT 구문의 간단한 버전 실행
    private void print() throws InterpreterException
    {
        double result;
        int len=0, spaces;
        String lastDelim = "";

        do {
            getToken(); // 다음 리스트 아이템을 얻음
            if(kwToken==EOL || token.equals(EOP)) break;

            if(tokType==QUOTEDSTR) { // 문자열
                System.out.print(token);
                len += token.length();
                getToken();
            }
            else { // 수식
                putBack();
                result = evaluate();
                getToken();
                System.out.print(result);

                // 현재 합계에 결과 길이를 더함
                Double t = new Double(result);
                len += t.toString().length(); // 길이 저장
            }
            lastDelim = token;

            // 쉼표이면 다음 탭으로 이동
            if(lastDelim.equals(",")) {
                // 다음 탭으로 이동하기 위해 공백 수 계산
                spaces = 8 - (len % 8);
                len += spaces; // 탭 위치에 추가
                while(spaces != 0) {
                    System.out.print(" ");
                    spaces--;
                }
            }
            else if(token.equals(";")) {
                System.out.print(" ");
                len++;
            }
            else if(kwToken != EOL && !token.equals(EOP))
                handleErr(SYNTAX);
        } while (lastDelim.equals(";") || lastDelim.equals(","));

        if(kwToken==EOL || token.equals(EOP)) {
            if(!lastDelim.equals(";") && !lastDelim.equals(","))
                System.out.println();
        }
        else handleErr(SYNTAX);
    }

    // GOTO 구문 실행
    private void execGoto() throws InterpreterException
    {
        Integer loc;

        getToken(); // 이동할 레이블을 얻음

        // labelTable에서 레이블의 위치를 검색
        loc = (Integer) labelTable.get(token);

        if(loc == null)
            handleErr(UNDEFLABEL); // 정의되지 않은 레이블
        else // loc에서 프로그램 실행 시작
            progIdx = loc.intValue();
    }

    // IF 구문 실행
    private void execIf() throws InterpreterException
    {
        double result;

        result = evaluate(); // 관계수식의 값 얻음

    /* 결과가 참인 경우(0이 아님), IF의 목표를 실행하고
       그렇지 않으면, 프로그램의 다음 줄로 이동 */
        if(result != 0.0) {
            getToken();
            if(kwToken != THEN) {
                handleErr(THENEXPECTED);
                return;
            }
        }
        else findEOL();
    }

    // FOR 루프 실행
    private void execFor() throws InterpreterException
    {
        ForInfo stckvar = new ForInfo();
        double value;
        char vname;

        getToken(); // 제어 변수 읽음
        vname = token.charAt(0);
        if(!Character.isLetter(vname)) {
            handleErr(NOTVAR);
            return;
        }

        // 제어 변수의 인덱스 저장
        stckvar.var = Character.toUpperCase(vname) - 'A';

        getToken(); // 등호 읽음
        if(token.charAt(0) != '=') {
            handleErr(EQUALEXPECTED);
            return;
        }

        value = evaluate(); // 초기값 얻음

        vars[stckvar.var] = value;

        getToken(); // TO 읽고 버림
        if(kwToken != TO) handleErr(TOEXPECTED);

        stckvar.target = evaluate(); // 목표값 얻음

    /* 루프가 최소한 한 번 실행될 수 있으면 스택에 정보 저장 */
        if(value >= vars[stckvar.var]) {
            stckvar.loc = progIdx;  //루프의 시작 위치 저장
            fStack.push(stckvar);
        }
        else // 루프 코드를 빠져나감
            while(kwToken != NEXT) getToken();
    }

    // NEXT 구문 실행
    private void next() throws InterpreterException
    {
        ForInfo stckvar;

        try {
            // 현재 For 루프를 위한 정보 검색
            stckvar = (ForInfo) fStack.pop();
            vars[stckvar.var]++; // 컨트롤 변수

            // 목표값을 넘어서면 종료
            if(vars[stckvar.var] > stckvar.target) return;

            // 그렇지 않으면 정보를 다시 스택에 저장
            fStack.push(stckvar);
            progIdx = stckvar.loc;  // 루프의 시작 위치로 이동
            } catch(EmptyStackException exc) {
            handleErr(NEXTWITHOUTFOR);
        }
    }

    // 간단한 형태의 INPUT 실행
    private void input() throws InterpreterException
    {
        int var;
        double val = 0.0;
        String str;

        BufferedReader br = new
                BufferedReader(new InputStreamReader(System.in));

        getToken(); // 프롬프트 문자열이 존재하는지 확인
        if(tokType == QUOTEDSTR) {
            // 있다면 출력 후 쉼표 검사
            System.out.print(token);
            getToken();
            if(!token.equals(",")) handleErr(SYNTAX);
            getToken();
        }
        else System.out.print("? "); // 그렇지 않다면 ? 출력

        // var 값 얻음
        var =  Character.toUpperCase(token.charAt(0)) - 'A';

        try {
            str = br.readLine();
            val = Double.parseDouble(str); // read the value
        } catch (IOException exc) {
            handleErr(INPUTIOERROR);
        } catch (NumberFormatException exc) {
      /* You might want to handle this error
         differently than the other interpreter
         errors. */
            System.out.println("Invalid input.");
        }

        vars[var] = val; // store it
    }

    // GOSUB 실행
    private void gosub() throws InterpreterException
    {
        Integer loc;

        getToken();

        // 호출할 레이블을 찾음
        loc = (Integer) labelTable.get(token);

        if(loc == null)
            handleErr(UNDEFLABEL); // 정의되지 않은 레이블
        else {
            // 리턴될 위치 저장(GOSUB 구문 다음줄을 의미함).
            gStack.push(new Integer(progIdx));

            // loc에 저장된 위치에서 프로그램 실행을 시작
            progIdx = loc.intValue();
        }
    }

    // GOSUB에서 리턴
    private void greturn() throws InterpreterException
    {
        Integer t;

        try {
            // 프로그램 인덱스 복구(저장했던 리턴될 위치를 꺼냄)
            t = (Integer) gStack.pop();
            progIdx = t.intValue();
        } catch(EmptyStackException exc) {
            handleErr(RETURNWITHOUTGOSUB);
        }

    }

    // REPEAT 실행
    private void repeat() throws InterpreterException{
        ReInfo stckvar = new ReInfo();

        stckvar.loc = progIdx;  //루프의 시작 위치 저장
        rStack.push(stckvar);
    }

    // UNTIL
    private void until() throws InterpreterException{
        ReInfo stckvar;
        double result;
        try {
            // REPEAT 루프에 대한 정보 검색
            stckvar = (ReInfo) rStack.pop();
            result=evaluate();  //관계식 계산

            if(result!= 0.0) return; //조건식이 참이면 루프 종료
            // 그렇지 않으면 정보를 다시 스택에 저장
            rStack.push(stckvar);
            progIdx = stckvar.loc;  // 루프의 시작 위치로 이동
        } catch(EmptyStackException exc) {
            handleErr(NEXTWITHOUTFOR);
        }
    }

    // **************** Expression Parser ****************

    // Parser entry point.
    private double evaluate() throws InterpreterException
    {
        double result = 0.0;

        getToken();
        if(token.equals(EOP))
            handleErr(NOEXP); // no expression present

        // Parse and evaluate the expression.
        result = evalExp1();

        putBack();

        return result;
    }

    // Process relational operators.
    private double evalExp1() throws InterpreterException
    {
        double l_temp, r_temp, result;
        char op;

        result = evalExp2();
        // If at end of program, return.
        if(token.equals(EOP)) return result;

        op = token.charAt(0);

        if(isRelop(op)) {
            l_temp = result;
            getToken();
            r_temp = evalExp1();
            switch(op) { // perform the relational operation
                case '<':
                    if(l_temp < r_temp) result = 1.0;
                    else result = 0.0;
                    break;
                case LE:
                    if(l_temp <= r_temp) result = 1.0;
                    else result = 0.0;
                    break;
                case '>':
                    if(l_temp > r_temp) result = 1.0;
                    else result = 0.0;
                    break;
                case GE:
                    if(l_temp >= r_temp) result = 1.0;
                    else result = 0.0;
                    break;
                case '=':
                    if(l_temp == r_temp) result = 1.0;
                    else result = 0.0;
                    break;
                case NE:
                    if(l_temp != r_temp) result = 1.0;
                    else result = 0.0;
                    break;
            }
        }
        return result;
    }

    // Add or subtract two terms.
    private double evalExp2() throws InterpreterException
    {
        char op;
        double result;
        double partialResult;

        result = evalExp3();

        while((op = token.charAt(0)) == '+' || op == '-') {
            getToken();
            partialResult = evalExp3();
            switch(op) {
                case '-':
                    result = result - partialResult;
                    break;
                case '+':
                    result = result + partialResult;
                    break;
            }
        }
        return result;
    }

    // Multiply or divide two factors.
    private double evalExp3() throws InterpreterException
    {
        char op;
        double result;
        double partialResult;

        result = evalExp4();

        while((op = token.charAt(0)) == '*' ||
                op == '/' || op == '%') {
            getToken();
            partialResult = evalExp4();
            switch(op) {
                case '*':
                    result = result * partialResult;
                    break;
                case '/':
                    if(partialResult == 0.0)
                        handleErr(DIVBYZERO);
                    result = result / partialResult;
                    break;
                case '%':
                    if(partialResult == 0.0)
                        handleErr(DIVBYZERO);
                    result = result % partialResult;
                    break;
            }
        }
        return result;
    }

    // Process an exponent.
    private double evalExp4() throws InterpreterException
    {
        double result;
        double partialResult;
        double ex;
        int t;

        result = evalExp5();

        if(token.equals("^")) {
            getToken();
            partialResult = evalExp4();
            ex = result;
            if(partialResult == 0.0) {
                result = 1.0;
            } else
                for(t=(int)partialResult-1; t > 0; t--)
                    result = result * ex;
        }
        return result;
    }

    // Evaluate a unary + or -.
    private double evalExp5() throws InterpreterException
    {
        double result;
        String  op;

        op = "";
        if((tokType == DELIMITER) &&
                token.equals("+") || token.equals("-")) {
            op = token;
            getToken();
        }
        result = evalExp6();

        if(op.equals("-")) result = -result;

        return result;
    }

    // Process a parenthesized expression.
    private double evalExp6() throws InterpreterException
    {
        double result;

        if(token.equals("(")) {
            getToken();
            result = evalExp2();
            if(!token.equals(")"))
                handleErr(UNBALPARENS);
            getToken();
        }
        else result = atom();

        return result;
    }

    // Get the value of a number or variable.
    private double atom() throws InterpreterException
    {
        double result = 0.0;

        switch(tokType) {
            case NUMBER:
                try {
                    result = Double.parseDouble(token);
                } catch (NumberFormatException exc) {
                    handleErr(SYNTAX);
                }
                getToken();
                break;
            case VARIABLE:
                result = findVar(token);
                getToken();
                break;
            default:
                handleErr(SYNTAX);
                break;
        }
        return result;
    }

    // Return the value of a variable.
    private double findVar(String vname)
            throws InterpreterException
    {
        if(!Character.isLetter(vname.charAt(0))){
            handleErr(SYNTAX);
            return 0.0;
        }
        return vars[Character.toUpperCase(vname.charAt(0))-'A'];
    }

    // Return a token to the input stream.
    private void putBack()
    {
        if(token == EOP) return;
        for(int i=0; i < token.length(); i++) progIdx--;
    }

    // Handle an error.
    private void handleErr(int error)
            throws InterpreterException
    {
        String[] err = {
                "Syntax Error",
                "Unbalanced Parentheses",
                "No Expression Present",
                "Division by Zero",
                "Equal sign expected",
                "Not a variable",
                "Label table full",
                "Duplicate label",
                "Undefined label",
                "THEN expected",
                "TO expected",
                "NEXT without FOR",
                "RETURN without GOSUB",
                "Closing quotes needed",
                "File not found",
                "I/O error while loading file",
                "I/O error on INPUT statement"
        };

        throw new InterpreterException(err[error]);
    }

    // 다음 토큰을 읽음
    private void getToken() throws InterpreterException
    {
        char ch;

        tokType = NONE;
        token = "";
        kwToken = UNKNCOM;

        // 프로그램의 끝인지 검사
        if(progIdx == prog.length) {
            token = EOP;
            return;
        }

        // 공백 건너뜀
        while(progIdx < prog.length &&
                isSpaceOrTab(prog[progIdx])) progIdx++;

        // 프로그램의 뒤에 공백문자가 붙어 있으면 종료
        if(progIdx == prog.length) {
            token = EOP;
            tokType = DELIMITER;
            return;
        }

        if(prog[progIdx] == '\r') { // 줄바꿈 문자 처리
            progIdx += 2;
            kwToken = EOL;
            token = "\r\n";
            return;
        }

        // 관계 연산자에 대한 검사
        ch = prog[progIdx];
        if(ch == '<' || ch == '>') {
            if(progIdx+1 == prog.length) handleErr(SYNTAX);

            switch(ch) {
                case '<':
                    if(prog[progIdx+1] == '>') {    //같지 않다는 연산자
                        progIdx += 2;;
                        token = String.valueOf(NE);
                    }
                    else if(prog[progIdx+1] == '=') {
                        progIdx += 2;
                        token = String.valueOf(LE);
                    }
                    else {
                        progIdx++;
                        token = "<";
                    }
                    break;
                case '>':
                    if(prog[progIdx+1] == '=') {
                        progIdx += 2;;
                        token = String.valueOf(GE);
                    }
                    else {
                        progIdx++;
                        token = ">";
                    }
                    break;
            }
            tokType = DELIMITER;
            return;
        }

        if(isDelim(prog[progIdx])) {
            //연산자
            token += prog[progIdx];
            progIdx++;
            tokType = DELIMITER;
        }
        else if(Character.isLetter(prog[progIdx])) {
            // 변수 또는 키워드
            while(!isDelim(prog[progIdx])) {
                token += prog[progIdx];
                progIdx++;
                if(progIdx >= prog.length) break;
            }

            kwToken = lookUp(token);
            if(kwToken==UNKNCOM) tokType = VARIABLE;
            else tokType = COMMAND;
        }
        else if(Character.isDigit(prog[progIdx])) {
            // 숫자
            while(!isDelim(prog[progIdx])) {
                token += prog[progIdx];
                progIdx++;
                if(progIdx >= prog.length) break;
            }
            tokType = NUMBER;
        }
        else if(prog[progIdx] == '"') {
            // 인용부호가 있는 문자열
            progIdx++;
            ch = prog[progIdx];
            while(ch !='"' && ch != '\r') {
                token += ch;
                progIdx++;
                ch = prog[progIdx];
            }
            if(ch == '\r') handleErr(MISSINGQUOTE); //닫는 인용부호가 없음
            progIdx++;
            tokType = QUOTEDSTR;
        }
        else { // 정의되지 않은 문자인 경우 프로그램 종료
            token = EOP;
            return;
        }
    }

    // Return true if c is a delimiter.
    private boolean isDelim(char c)
    {
        if((" \r,;<>+-/*%^=()".indexOf(c) != -1))
            return true;
        return false;
    }

    // Return true if c is a space or a tab.
    boolean isSpaceOrTab(char c)
    {
        if(c == ' ' || c =='\t') return true;
        return false;
    }

    // Return true if c is a relational operator.
    boolean isRelop(char c) {
        if(relops.indexOf(c) != -1) return true;
        return false;
    }

    /* 토큰 테이블에서 토큰의 내부 표현 검색. */
    private int lookUp(String s)
    {
        int i;

        s = s.toLowerCase();    //소문자로 변환

        // 키워드 테이블에 토큰이 있는지 검사
        for(i=0; i < kwTable.length; i++)
            if(kwTable[i].keyword.equals(s))    //키워드 테이블에 있는지 확인
                return kwTable[i].keywordTok;   //키워드의 내부 표현 리턴
        return UNKNCOM; // 알려지지 않은 키워드. 즉, 키워드가 아님
    }
}