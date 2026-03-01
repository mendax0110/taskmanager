package com.taskmanager.repository;

import com.taskmanager.exception.TaskNotFoundException;
import com.taskmanager.model.Task;

import java.util.*;

/**
 * Collection concept: Use hashmap for fast lookups
 */
public class InMemoryTaskRepository implements TaskRepository
{
    private final Map<Integer, Task> storage = new LinkedHashMap<>();
    private int idCounter = 1;

    @Override
    public void save(Task task)
    {
        storage.put(task.getId(), task);
    }

    @Override
    public Optional<Task> findById(int id)
    {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Task> findAll()
    {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void delete(int id) throws TaskNotFoundException
    {
        if (!storage.containsKey(id))
        {
            throw new TaskNotFoundException(id);
        }
        storage.remove(id);
    }

    @Override
    public void update(Task task) throws TaskNotFoundException
    {
        if (!storage.containsKey(task.getId()))
        {
            throw new TaskNotFoundException(task.getId());
        }
        storage.put(task.getId(), task);
    }

    @Override
    public int nextId()
    {
        return idCounter++;
    }
}