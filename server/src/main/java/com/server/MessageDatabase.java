package com.server;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Base64;

import org.apache.commons.codec.digest.Crypt;
import org.json.JSONArray;
import org.json.JSONObject;

public class MessageDatabase {

    private Connection dbConnection = null;
    private static MessageDatabase dbInstance = null;

    public static synchronized MessageDatabase getInstance() {
        if (dbInstance == null) {
            dbInstance = new MessageDatabase();
        }
        return dbInstance;
    }

    private MessageDatabase() {
        try {
            init();
        } catch (SQLException e) {
        }
    }

    private void init() throws SQLException {

        String dbName = "DB";

        String database = "jdbc:sqlite:" + dbName;
        dbConnection = DriverManager.getConnection(database);

        if (null != dbConnection) {
            String createUserTable = "create table users (nickname varchar(50) NOT NULL, password varchar(50) NOT NULL, email varchar(50), primary key(nickname))";
            String createMessageTable = "create table messages (longitude double NOT NULL, latitude double NOT NULL, nickname varchar(50) NOT NULL, dangertype varchar(50) NOT NULL, sent INTEGER NOT NULL, phonenumber varchar(10), areacode varchar(10))";
            Statement createStatement = dbConnection.createStatement();
            createStatement.executeUpdate(createUserTable);
            createStatement.executeUpdate(createMessageTable);
            createStatement.close();

        }
    }

    public void closeDB() throws SQLException {
        if (null != dbConnection) {
            dbConnection.close();
            System.out.println("closing db connection");
            dbConnection = null;
        }
    }

    public boolean setUser(JSONObject user) throws SQLException {

        if (checkIfUserExists(user.getString("username"))) {
            return false;
        }
        SecureRandom strongRandomNumberGenerator = new SecureRandom();
        byte bytes[] = new byte[13];
        strongRandomNumberGenerator.nextBytes(bytes);
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes;
        String passu = user.getString("password");
        String hashedPassword = Crypt.crypt(passu, salt);
        String setUserString = "insert into users " +
                "VALUES('" + user.getString("username") + "','" + hashedPassword + "','"
                + user.getString("email") + "')";
        Statement createStatement;
        createStatement = dbConnection.createStatement();
        createStatement.executeUpdate(setUserString);
        createStatement.close();

        return true;
    }

    public boolean checkIfUserExists(String givenUserName) throws SQLException {

        Statement queryStatement = null;
        ResultSet rs;

        String checkUser = "select nickname from users where nickname = '" + givenUserName + "'";
        System.out.println("checking user");

        queryStatement = dbConnection.createStatement();
        rs = queryStatement.executeQuery(checkUser);

        if (rs.next()) {
            System.out.println("user exists");
            return true;
        } else {
            return false;
        }
    }

    public boolean authenticateUser(String givenUserName, String givenPassword) throws SQLException {

        Statement queryStatement = null;
        ResultSet rs;

        String getMessagesString = "select nickname, password from users where nickname = '" + givenUserName + "'";
        System.out.println(givenUserName);

        queryStatement = dbConnection.createStatement();
        rs = queryStatement.executeQuery(getMessagesString);

        if (rs.next() == false) {

            System.out.println("cannot find such user");
            return false;

        } else {

            String pass = rs.getString("password");
            if (pass.equals(Crypt.crypt(givenPassword, pass))) {
                return true;

            } else {

                return false;
            }

        }

    }

    public void setMessage(JSONObject message) throws SQLException {

        if (null != dbConnection) {
            if (message.has("phonenumber") && message.has("areacode")) {
                String setMessageString = "insert into messages " +
                        "VALUES('" + message.getDouble("longitude") + "','" + message.getDouble("latitude") + "','"
                        + message.getString("nickname") + "','" + message.getString("dangertype") + "','"
                        + message.getLong("sent") + "','" + message.getString("phonenumber") + "','"
                        + message.getString("areacode") + "')";
                Statement createStatement;
                createStatement = dbConnection.createStatement();
                createStatement.executeUpdate(setMessageString);
                createStatement.close();
            }
        }

    }

    public JSONArray getMessages() throws SQLException {

        Statement queryStatement = null;
        WarningMessage msg = new WarningMessage();
        JSONArray msgs = new JSONArray();

        String getMessagesString = "select nickname, dangertype, sent, longitude, latitude, phonenumber, areacode from messages where phonenumber is not null and areacode is not null ";

        queryStatement = dbConnection.createStatement();
        ResultSet rs = queryStatement.executeQuery(getMessagesString);

        while (rs.next()) {
            JSONObject obj = new JSONObject();

            obj.put("nickname", rs.getString("nickname"));
            obj.put("dangertype", rs.getString("dangertype"));

            long timestamp = rs.getLong("sent");
            msg.setSent(timestamp);

            Date j = new Date(timestamp);
            SimpleDateFormat format = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSSX");
            format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            String date = format.format(j);
            obj.put("sent", date);
            obj.put("longitude", rs.getDouble("longitude"));
            obj.put("latitude", rs.getDouble("latitude"));

            String numero = rs.getString("phonenumber");
            String koodi = rs.getString("areacode");
            if (!numero.isEmpty() && !koodi.isEmpty()) {
                obj.put("phonenumber", rs.getString("phonenumber"));
                obj.put("areacode", rs.getString("areacode"));

            }
            msgs.put(obj);
        }
        return msgs;

    }
}