package com.company;
/*
   This module contains the recursive descent
   parser that uses variables.
*/

// Exception class for parser errors.
class ParserException extends Exception {
    String errStr; // describes the error

    public ParserException(String str) {
        errStr = str;
    }

    public String toString() {
        return errStr;
    }
}

class Parser {
    // These are the token types.
    final int NONE = 0;
    final int DELIMITER = 1;    //문자
    final int VARIABLE = 2;     //변수
    final int NUMBER = 3;       //숫자
    final int STRING=4;         //문자열

    // These are the types of syntax errors.
    final int SYNTAX = 0;
    final int UNBALPARENS = 1;  //괄호 오류
    final int NOEXP = 2;        //표현이 없음
    final int DIVBYZERO = 3;    //0으로 나눔
    final int OPERATOR = 4;

    // This token indicates end-of-expression.
    final String EOE = "\0";

    private String exp;   // 표현을 담고있는 문자열(파싱 과정이 끝난 수식 표현)
    private int expIdx;   // 표현의 현재 인덱스
    private String token; // 현재 인덱싱 된 토큰
    private int tokType;  // 현재 인덱싱 된 토큰의 타입

    // 변수형을 위한 배열
    private String vars[] = new String[26];

    // 파서 진입 지점
    public String evaluate(String expstr) throws ParserException
    {
        String result;
        exp = expstr;
        expIdx = 0;

        getToken();
        if(token.equals(EOE))
            handleErr(NOEXP); // 표현식이 없음

        // 재귀적 파싱, 계산 시작
        result = evalExp1();

        if(!token.equals(EOE)) // 문자열의 마지막 토큰은 EOE 여야 한다.
            handleErr(SYNTAX);

        if(!isDigitstr(result)){    //결과 값이 문자열이면 큰따옴표 삽입
            result="\""+result+"\"";
        }
        return result;
    }

    // 변수 값 할당 처리
    private String evalExp1() throws ParserException
    {
        String result;
        int varIdx;
        int ttokType;
        String temptoken;

        if(tokType == VARIABLE) {
            //토큰 임시 저장
            temptoken = new String(token);
            ttokType = tokType;

            // 변수 인덱스 계산
            varIdx = Character.toUpperCase(token.charAt(0)) - 'A';

            getToken();
            //새로 받아온 토큰이 =이 아니면 토큰을 다시 돌려보내야함(인덱스 값 조정)
            if(!token.equals("=")) {
                putBack();
                // 임시 저장한 토큰 다시 되돌림
                token = new String(temptoken);
                tokType = ttokType;
            }
            else {
                getToken(); // 변수에 할당할 토큰을 가져옴
                result = evalExp2();
                vars[varIdx] = result;
                return result;
            }
        }

        return evalExp2();
    }

    // 덧셈, 뺄셈
    private String evalExp2() throws ParserException
    {
        char op;
        String result;
        String partialResult;

        result = evalExp3();
        while((op = token.charAt(0)) == '+' || op == '-') {
            getToken();
            partialResult = evalExp3();
            switch(op) {
                case '-':
                    if(isDigitstr(result)&&isDigitstr(partialResult))
                        result = Double.toString(Double.parseDouble(result) - Double.parseDouble(partialResult));
                    else
                        result=result.replace(partialResult,"");
                    break;
                case '+':
                    if(isDigitstr(result)&&isDigitstr(partialResult))
                        result = Double.toString(Double.parseDouble(result) + Double.parseDouble(partialResult));
                    else
                        result=result+partialResult;
                    break;
            }
        }
        return result;
    }

    // 곱셈, 나눗셈
    private String evalExp3() throws ParserException
    {
        char op;
        String result;
        String partialResult;

        result = evalExp4();

        while((op = token.charAt(0)) == '*' ||
                op == '/' || op == '%') {
            getToken();
            partialResult = evalExp4();
            switch(op) {
                case '*':
                    if(isDigitstr(result)&&isDigitstr(partialResult))
                        result = Double.toString(Double.parseDouble(result) * Double.parseDouble(partialResult));
                    else if(!isDigitstr(result)&&isDigitstr(partialResult)){    //문자열*숫자인 경우
                        if(Double.parseDouble(partialResult)<0) handleErr(OPERATOR);
                        else result=result.repeat(Integer.parseInt(partialResult));
                    }
                    else if(isDigitstr(result)&&!isDigitstr(partialResult)){    //숫자*문자열인 경우
                        if(Double.parseDouble(result)<0) handleErr(OPERATOR);
                        else result=partialResult.repeat(Integer.parseInt(result));
                    }
                    else{
                        handleErr(OPERATOR);
                    }
                    break;
                case '/':
                    if(partialResult == "0.0")
                        handleErr(DIVBYZERO);
                    if(isDigitstr(result)&&isDigitstr(partialResult))
                        result = Double.toString(Double.parseDouble(result) / Double.parseDouble(partialResult));
                    else{
                        handleErr(OPERATOR);
                    }
                    break;
                case '%':
                    if(partialResult == "0.0")
                        handleErr(DIVBYZERO);
                    if(isDigitstr(result)&&isDigitstr(partialResult))
                        result = Double.toString(Double.parseDouble(result) % Double.parseDouble(partialResult));
                    else{
                        handleErr(OPERATOR);
                    }
                    break;
            }
        }
        return result;
    }

    // 지수
    private String evalExp4() throws ParserException
    {
        String result;
        String partialResult;
        String ex;
        int t;

        result = evalExp5();

        if(token.equals("^")) {
            getToken();
            partialResult = evalExp4();
            ex = result;
            if(partialResult == "0.0") {
                result = "1.0";
            }
            else{
                if(isDigitstr(result)&&isDigitstr(partialResult)){
                    for(t=Integer.parseInt(partialResult)-1; t > 0; t--)
                        result =  Double.toString(Double.parseDouble(result)* Double.parseDouble(ex));
                }
                else{
                    handleErr(OPERATOR);
                }
            }

        }
        return result;
    }

    // 단항 +, -
    private String evalExp5() throws ParserException
    {
        String result;
        String  op;

        op = "";
        //단항 연산자이면 op에 저장 후 evalExp6으로, 아니면 evalExp6으로 바로 넘어감
        if((tokType == DELIMITER) &&
                token.equals("+") || token.equals("-")) {
            op = token;
            getToken();
        }
        result = evalExp6();

        if(op.equals("-")) {
            if (isDigitstr(result))
                result = Double.toString(-Double.parseDouble(result));    //음수 처리
            else handleErr(OPERATOR);
        }
        return result;
    }

    // 괄호
    private String evalExp6() throws ParserException
    {
        String result;

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

    //숫자 또는 변수 값 가져옴
    private String atom() throws ParserException
    {
        String result = "0.0";

        switch(tokType) {
            case NUMBER:
                try {
                    result = token;
                } catch (NumberFormatException exc) {
                    handleErr(SYNTAX);
                }
                getToken();
                break;
            case VARIABLE:
                result =  findVar(token);
                getToken();
                break;
            case STRING:
                result = token;
                getToken();
                break;
            default:
                handleErr(SYNTAX);
                break;
        }
        return result;
    }

    //변수의 값 반환(인덱스는 문자아스키코드-A)
    private String findVar(String vname) throws ParserException
    {
        if(!Character.isLetter(vname.charAt(0))){
            handleErr(SYNTAX);
            return "0.0";
        }
        return vars[Character.toUpperCase(vname.charAt(0))-'A'];
    }

    // 입력 스트림의 값만큼 인덱스 값을 되돌린다
    private void putBack()
    {
        if(token == EOE) return;
        for(int i=0; i < token.length(); i++) expIdx--;
    }

    // 에러처리
    private void handleErr(int error) throws ParserException
    {
        String[] err = {
                "Syntax Error",
                "Unbalanced Parentheses",
                "No Expression Present",
                "Division by Zero",
                "This operator cannot be applied"
        };

        throw new ParserException(err[error]);
    }

    // 토큰값을 가져오는 메소드
    private void getToken() throws ParserException {
        tokType = NONE;
        token = "";

//        // 표현의 마지막이면 getToken() 종료
//        if(expIdx == exp.length()) {
//            token = EOE;
//            return;
//        }

        // 공백이면 다음으로 넘어감
        /*charAt() 메소드
        * String 문자열에서 한 글자만 선택하여 char 타입으로 변환
        * str.charAt(index)의 형태로 사용한다
        */
        while(expIdx < exp.length() &&
                Character.isWhitespace(exp.charAt(expIdx))) ++expIdx;

        // 표현의 마지막이면 getToken() 종료
        if(expIdx == exp.length()) {
            token = EOE;
            return;
        }

        if(isDelim(exp.charAt(expIdx))) { // 연산자형
            token += exp.charAt(expIdx);
            expIdx++;
            tokType = DELIMITER;
        }
        else if(Character.isLetter(exp.charAt(expIdx))) { // 변수형
            while(!isDelim(exp.charAt(expIdx))) {
                token += exp.charAt(expIdx);
                expIdx++;
                if(expIdx >= exp.length()) break;
            }
            tokType = VARIABLE;
        }
        else if(Character.isDigit(exp.charAt(expIdx))) { // 숫자형
            while(!isDelim(exp.charAt(expIdx))) {
                token += exp.charAt(expIdx);
                expIdx++;
                if(expIdx >= exp.length()) break;
            }
            tokType = NUMBER;
        }
        else if(isQuotes(exp.charAt(expIdx))){  //문자열형
            while(!isDelim(exp.charAt(expIdx))) {
                token += exp.charAt(expIdx);
                expIdx++;
                if(expIdx >= exp.length()){
                    if(!isQuotes(exp.charAt(expIdx-1))) // 문자열이 "로 안끝나면 오류
                        handleErr(SYNTAX);
                    break;
                }
            }
            token=token.replace("\"","");   //큰 따옴표 제거
            tokType = STRING;
        }
        else { // 정의되지 않은 형(표현식 종료)
            token = EOE;
            return;
        }
    }

    // 연산자형 검사 메소드
    private boolean isDelim(char c)
    {
        if((" +-/*%^=()".indexOf(c) != -1))
            return true;
        return false;
    }
    //큰따옴표 검사 메소드
    private boolean isQuotes(char c){
        if(c=='\"') return true;
        return false;
    }

    //숫자 문자열인지 검사하는 메소드
    //숫자 문자열이 아니면 Double 형변환시 오류 발생하는 것을 이용
    private boolean isDigitstr(String str){
        try{
            Double.parseDouble(str);
        }catch (NumberFormatException exc){
            return false;
        }
        return true;
    }
}

