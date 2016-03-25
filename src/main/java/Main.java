import java.sql.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

import java.lang.StringBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import static spark.Spark.*;
import spark.template.freemarker.FreeMarkerEngine;
import spark.ModelAndView;
import static spark.Spark.get;

import com.heroku.sdk.jdbc.DatabaseUrl;

public class Main {

  private static final String CLIENT_ID = "3MVG9Rd3qC6oMalXohixmFPPMKzEwbcTwF5FzVCrDL9DyMFdr.h1.HrFzhd8CwbAxiXx2a6aV.uNnJiap07BV";
  private static final String APP_HOST = "https://warm-fortress-58277.herokuapp.com";

  public static void main(String[] args) {

    port(Integer.valueOf(System.getenv("PORT")));
    staticFileLocation("/public");

    get("/hello", (req, res) -> "Hello World");

    get("/oauth2", (request, response) -> {
      StringBuilder redirectUrl = new StringBuilder();
      redirectUrl.append("https://login.salesforce.com/services/oauth2/authorize?response_type=token&client_id=").append(CLIENT_ID).append("&redirect_uri=").append(URLEncoder.encode(APP_HOST + "/callback"));
      response.redirect(redirectUrl.toString());
    });

    get("/", (request, response) -> {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("message", "Hello World!");

            return new ModelAndView(attributes, "index.ftl");
        }, new FreeMarkerEngine());

    get("/db", (req, res) -> {
      Connection connection = null;
      Map<String, Object> attributes = new HashMap<>();
      try {
        connection = DatabaseUrl.extract().getConnection();

        Statement stmt = connection.createStatement();
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
        stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
        ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

        ArrayList<String> output = new ArrayList<String>();
        while (rs.next()) {
          output.add( "Read from DB: " + rs.getTimestamp("tick"));
        }

        attributes.put("results", output);
        return new ModelAndView(attributes, "db.ftl");
      } catch (Exception e) {
        attributes.put("message", "There was an error: " + e);
        return new ModelAndView(attributes, "error.ftl");
      } finally {
        if (connection != null) try{connection.close();} catch(SQLException e){}
      }
    }, new FreeMarkerEngine());

  }

}
