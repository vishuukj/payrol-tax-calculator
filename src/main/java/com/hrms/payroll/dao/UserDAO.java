package com.hrms.payroll.dao;

import com.hrms.payroll.model.User;

import java.util.Optional;

public interface UserDAO extends CrudDAO<User, Integer> {
    Optional<User> findByUsername(String username);
    void updateLastLogin(int userId);
}
