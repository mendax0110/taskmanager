package com.taskmanager.repository;

import com.taskmanager.exception.TaskNotFoundException;
import com.taskmanager.model.PersonalTask;
import com.taskmanager.model.Task;
import com.taskmanager.model.WorkTask;

import javax.imageio.IIOException;
import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/**
 * FILE I/O: Persists to a csv file.
 */
public class FileTaskRepository implements TaskRepository
{
    private final Path filePath;
    private final Map<Integer, Task> cache = new LinkedHashMap<>();
    private int idCounter = 1;
    private String DELIMITER = ",";

    public FileTaskRepository(String filePath)
    {
        this.filePath = Paths.get(filePath);
        loadFromFile();
    }

    private Task parseLine(String line)
    {
        try
        {
            String[] parts = line.split(DELIMITER);
            int id = Integer.parseInt(parts[0]);
            String type = parts[1];
            String title = parts[2];
            String desc = parts[3];
            LocalDate due = parts[4].equals("null") ? null : LocalDate.parse(parts[4]);
            boolean completed = Boolean.parseBoolean(parts[5]);

            Task task;
            if ("WorkTask".equals(type))
            {
                WorkTask.Urgency urgency = WorkTask.Urgency.valueOf(parts[7]);
                task = new WorkTask(id, title, desc, due, parts[6], urgency);
            }
            else
            {
                task = new PersonalTask(id, title, desc, due, Integer.parseInt(parts[6]));
            }
            if (completed) task.complete();
            return task;
        }
        catch (Exception ex)
        {
            System.err.println("Skipping malformed line: " + line);
            return null;
        }
    }

    private String serializeTask(Task task)
    {
        String extra = "";
        if (task instanceof WorkTask wt)
        {
            extra = "," + wt.getProject() + "," + wt.getUrgency().name();
        }
        else if (task instanceof PersonalTask pt)
        {
            extra = "," + pt.getEffortLevel();
        }

        return String.format("%d,%s,%s,%s,%s,%b%s",
                task.getId(),
                task.getClass().getSimpleName(),
                task.getTitle().replace(",", ";"),
                task.getDescription().replace(",", ";"),
                task.getDueDate() != null ? task.getDueDate() : "null",
                task.isCompleted(),
                extra);
    }

    private void loadFromFile()
    {
        if (!Files.exists(filePath)) return;

        try (BufferedReader reader = Files.newBufferedReader(filePath))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith("#") || line.isBlank()) continue;

                Task task = parseLine(line);
                if (task != null)
                {
                    cache.put(task.getId(), task);
                    if (task.getId() >= idCounter)
                    {
                        idCounter = task.getId() + 1;
                    }
                }
            }
        }
        catch (IOException ex)
        {
            System.err.println("Warning: Could not load tasks from file: " + ex.getMessage());
        }
    }

    private void saveToFile()
    {
        try (BufferedWriter write = Files.newBufferedWriter(filePath))
        {
            write.write("# Task Manager - CSV Storage");
            write.newLine();

            for (var task : cache.values())
            {
                write.write(serializeTask(task));
                write.newLine();
            }
        }
        catch (IOException ex)
        {
            System.err.println("Error saving tasks: " + ex.getMessage());
        }
    }

    @Override
    public void save(Task task)
    {
        cache.put(task.getId(), task);
        saveToFile();
    }

    @Override
    public Optional<Task> findById(int id)
    {
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public List<Task> findAll()
    {
        return new ArrayList<>(cache.values());
    }

    @Override
    public void delete(int id) throws TaskNotFoundException
    {
        if (!cache.containsKey(id)) throw new TaskNotFoundException(id);
        cache.remove(id);
        saveToFile();
    }

    @Override
    public void update(Task task) throws TaskNotFoundException
    {
        if (!cache.containsKey(task.getId())) throw new TaskNotFoundException(task.getId());
        cache.put(task.getId(), task);
        saveToFile();
    }

    @Override
    public int nextId()
    {
        return idCounter++;
    }

}