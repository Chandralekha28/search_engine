package com.database;
import java.sql.*;

public class DBConnector {
    private static final String HOST = "localhost";
    private static final short PORT = 5432;
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";
    private static final String DATABASE = "wse";

    private static Connection conn = null;
    
    {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static boolean testConnection(IsolationLevel isolationLevel) {
        try {
            getConnection(isolationLevel);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String getJDBCString() {
        return String.format("jdbc:postgresql://%s:%d/%s", HOST, PORT, DATABASE);
    }
    
    public static enum IsolationLevel {
        READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED, "Read Committed"),
        REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ, "Repeatable Read"),
        SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE, "Serializable");

        private final int i;
        private final String s;

        IsolationLevel(int i, String s) {
            this.i = i;
            this.s = s;
        }

        public int toInt() {
            return this.i;
        }

        @Override
        public String toString() {
            return this.s;
        }
    }

    public static Connection getConnection(IsolationLevel isolationLevel) throws SQLException {
        if(DBConnector.conn == null) {
            DBConnector.conn = DriverManager.getConnection(getJDBCString(), USER, PASSWORD);
            conn.setTransactionIsolation(isolationLevel.toInt());
        }
        return DBConnector.conn;
    }

}