import java.sql.*;
import java.util.*;

public class DbSchemaProbe {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:mysql://localhost:3306/psc?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    String user = "root";
    String pass = "root";
    Class.forName("com.mysql.cj.jdbc.Driver");
    try (Connection c = DriverManager.getConnection(url, user, pass)) {
      System.out.println("Connected: " + !c.isClosed());
      List<String> tables = List.of("topic_question","topic","questionnaire_template","questionnaire","questionnaire_question","question_paper","question_paper_answer");
      for (String t: tables) {
        System.out.println("\nTABLE: " + t);
        try (PreparedStatement ps = c.prepareStatement(
          "SELECT COLUMN_NAME, DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? ORDER BY ORDINAL_POSITION")) {
          ps.setString(1, t);
          try (ResultSet rs = ps.executeQuery()) {
            int n=0;
            while (rs.next()) { n++; System.out.println("  " + rs.getString(1) + " (" + rs.getString(2) + ")"); }
            if (n==0) System.out.println("  <not found>");
          }
        }
      }

      System.out.println("\nSAMPLE topic_question rows:");
      try (Statement st = c.createStatement()) {
        try (ResultSet rs = st.executeQuery("SELECT * FROM topic_question LIMIT 5")) {
          ResultSetMetaData md = rs.getMetaData();
          int cols = md.getColumnCount();
          while (rs.next()) {
            StringBuilder b = new StringBuilder("  ");
            for (int i=1;i<=cols;i++) {
              if (i>1) b.append(", ");
              b.append(md.getColumnName(i)).append("=").append(rs.getString(i));
            }
            System.out.println(b.toString());
          }
        }
      } catch (Exception e) {
        System.out.println("  topic_question sample failed: " + e.getMessage());
      }

      String[] checks = {
        "SELECT COUNT(*) c FROM topic_question",
        "SELECT COUNT(*) c FROM questionnaire_question",
        "SELECT COUNT(*) c FROM topic_question tq JOIN topic t ON t.id = tq.topic_id",
        "SELECT COUNT(*) c FROM questionnaire_template qt JOIN topic t ON t.id = qt.topic_id"
      };
      for (String q: checks) {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(q)) {
          rs.next();
          System.out.println("\nCHECK: " + q + " => " + rs.getLong(1));
        } catch (Exception e) {
          System.out.println("\nCHECK FAILED: " + q + " => " + e.getMessage());
        }
      }
    }
  }
}
