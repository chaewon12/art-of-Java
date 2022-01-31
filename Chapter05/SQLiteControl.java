import java.sql.*;
import java.util.*;

public class SQLiteControl extends SQLiteManager {
    public SQLiteControl(){
        createConnection();
    }

    public void insert(String sql, ArrayList<String> list)throws SQLException {
        Connection conn = ensureConnection();
        PreparedStatement pstmt = null;

        try {
            // PreparedStatement 생성
            pstmt = conn.prepareStatement(sql);

            int i = 1;
            for (String s : list) {
                pstmt.setObject(i++, s);
            }

            // 쿼리 실행
            pstmt.executeUpdate();

            // 트랜잭션 COMMIT
            conn.commit();
            closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            // 트랜잭션 ROLLBACK
            if (conn != null) {
                conn.rollback();
            }
        }finally {
            // PreparedStatement 종료
            if( pstmt != null ) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ResultSet select(String sql)throws SQLException{
        Connection conn = ensureConnection();
        Statement stmt;
        ResultSet result = null;
        try{
            // Statement 생성
            stmt = conn.createStatement();
            // 데이터 조회
            result = stmt.executeQuery(sql);

            //return result;
        }catch(SQLException e){
            System.out.println(e);
        }
        return result;
    }

}
