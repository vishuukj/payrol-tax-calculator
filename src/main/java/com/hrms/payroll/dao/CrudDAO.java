package com.hrms.payroll.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD contract implemented by every DAO in the system.
 * Demonstrates abstraction + Java generics + interface-based design.
 *
 * @param <T>  the domain model type
 * @param <ID> the primary key type
 */
public interface CrudDAO<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    boolean update(T entity);
    boolean deleteById(ID id);
}
