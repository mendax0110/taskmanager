package com.taskmanager.pattern;

import com.taskmanager.model.Task;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Concrete Observer - logs all task events
 */
public class AuditLogger implements TaskObserver
{
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final List<String> log = new ArrayList<>();
    
    private void record(String event)
    {
        String entry = "[" + LocalDateTime.now().format(FMT) + "] " + event;
        log.add(entry);
        System.out.println("  >> LOG: " + entry);
    }
    
    @Override
    public void onTaskAdded(Task task)
    {
        record("ADDED task #" + task.getId() + " - \"" + task.getTitle() + "\"");
    }
    
    @Override
    public void onTaskCompleted(Task task)
    {
        record("COMPLETED task #" + task.getId() + " - \"" + task.getTitle() + "\"");
    }

    @Override
    public void onTaskDeleted(int taskId)
    {
        record("DELETED task #" + taskId);
    }

    public List<String> getLog()
    {
        return Collections.unmodifiableList(log);
    }

    public void printLog()
    {
        System.out.println("\n=== Audit Log (" + log.size() + " entries) ===");
        log.forEach(System.out::println);
    }
}