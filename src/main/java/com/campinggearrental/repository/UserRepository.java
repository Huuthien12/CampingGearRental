package com.campinggearrental.repository;

import java.sql.SQLException;

public interface UserRepository {
    boolean authenticate(String username, String password) throws SQLException;
}