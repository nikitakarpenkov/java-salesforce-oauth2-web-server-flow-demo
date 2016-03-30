import java.sql.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.lang.StringBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import org.apache.http.client.methods.HttpGet;

import static spark.Spark.*;
import spark.template.freemarker.FreeMarkerEngine;
import spark.ModelAndView;
import static spark.Spark.get;

import com.heroku.sdk.jdbc.DatabaseUrl;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class Main {

  private static final String CLIENT_ID = "3MVG9Rd3qC6oMalXohixmFPPMKzEwbcTwF5FzVCrDL9DyMFdr.h1.HrFzhd8CwbAxiXx2a6aV.uNnJiap07BV";
  private static final String CLIENT_SECRET = "2223890121992725601";
  private static final String APP_HOST = "https://warm-fortress-58277.herokuapp.com";

  public static void main(String[] args) {

    port(Integer.valueOf(System.getenv("PORT")));
    staticFileLocation("/public");

    get("/hello", (req, res) -> "Hello World");

    get("/oauth2", (request, response) -> {
      StringBuilder redirectUrl = new StringBuilder();
      redirectUrl.append("https://login.salesforce.com/services/oauth2/authorize?response_type=code&client_id=")
                 .append(CLIENT_ID)
                 .append("&redirect_uri=")
                 .append(URLEncoder.encode(APP_HOST + "/callback"));
      response.redirect(redirectUrl.toString());
      return null;
    });

    get("/callback", (request, response) -> {

      String authorizationCode = request.queryParams("code");

      String obtainAccessTokenApiUrl = "https://login.salesforce.com/services/oauth2/token";

      HttpClient httpClient = new DefaultHttpClient();
      HttpPost httpPost = new HttpPost(obtainAccessTokenApiUrl);

      List<NameValuePair> urlParameters = new ArrayList<NameValuePair>(2);
      urlParameters.add(new BasicNameValuePair("grant_type", "authorization_code"));
      urlParameters.add(new BasicNameValuePair("code", request.queryParams("code")));
      urlParameters.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
      urlParameters.add(new BasicNameValuePair("redirect_uri", APP_HOST + "/callback"));

      httpPost.setEntity(new UrlEncodedFormEntity(urlParameters));

      HttpResponse httpResponse = httpClient.execute(httpPost);

      //System.out.println("Response Code : " + 
      //                                httpResponse.getStatusLine().getStatusCode());

      BufferedReader responseReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
      StringBuilder result = new StringBuilder();
      String responseLine = "";
      while ((responseLine = responseReader.readLine()) != null) {
        result.append(responseLine);
      }

      return result.toString();

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
