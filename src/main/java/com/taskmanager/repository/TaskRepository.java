package com.taskmanager.repository;

import com.taskmanager.exception.TaskNotFoundException;
import com.taskmanager.model.Task;

import java.util.List;
import java.util.Optional;

/**
 * Repo Pattern
 * Interface defines the contract for data access.
 * The service layer doesn't casre or know wether data comes from in-memory list, a file or a database
 */
public interface TaskRepository
{
    void save(Task task);

    Optional<Task> findById(int id);

    List<Task> findAll();

    void delete(int id) throws TaskNotFoundException;

    void update(Task task) throws TaskNotFoundException;

    int nextId();
}