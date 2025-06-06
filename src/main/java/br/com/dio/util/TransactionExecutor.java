package br.com.dio.util;
import java.sql.Connection;
import java.sql.SQLException;

public class TransactionExecutor {

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection conn) throws SQLException;
    }

    public static <T> T execute(Connection conn, SqlFunction<T> function) throws SQLException {
        try {
            T result = function.apply(conn);
            conn.commit();
            return result;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
    }
}
