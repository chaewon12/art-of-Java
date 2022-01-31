public class SBDemo {

    public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("Usage: sbasic <filename>");
            return;
        }

        try {
            SBasic ob = new SBasic(args[0]);    //SBasic 객체 생성
            ob.run();   //프로그램 실행
        } catch(InterpreterException exc) {
            System.out.println(exc);
        }
    }
}

