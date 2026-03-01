package com.taskmanager.service;

import com.taskmanager.exception.TaskNotFoundException;
import com.taskmanager.model.Task;
import com.taskmanager.model.WorkTask;
import com.taskmanager.pattern.TaskObserver;
import com.taskmanager.repository.TaskRepository;

import java.util.*;
import java.util.stream.*;

/**
 * Service layer: Business logic is here
 */
public class TaskService
{
    private final TaskRepository repository;

    private final List<TaskObserver> observers = new ArrayList<>();

    public TaskService(TaskRepository repository)
    {
        this.repository = repository;
    }

    public void addObserver(TaskObserver observer)
    {
        observers.add(observer);
    }

    private void notifyAdded(Task task) { observers.forEach(o -> o.onTaskAdded(task)); }
    private void notifyCompleted(Task task) { observers.forEach(o -> o.onTaskCompleted(task)); }
    private void notifyDeleted(int id) { observers.forEach(o -> o.onTaskDeleted(id)); }

    public Task addTask(Task task)
    {
        repository.save(task);
        notifyAdded(task);
        return task;
    }

    public int nextId() { return repository.nextId(); }

    public void completeTask(int id) throws TaskNotFoundException
    {
        Task task = repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        task.complete();
        repository.update(task);
        notifyCompleted(task);
    }

    public void deleteTask(int id) throws TaskNotFoundException
    {
        repository.delete(id);
        notifyDeleted(id);
    }

    public Optional<Task> getTask(int id)
    {
        return repository.findById(id);
    }

    public List<Task> getAllTasks()
    {
        return repository.findAll();
    }

    public List<Task> getPendingTasks()
    {
        return repository.findAll().stream().filter(t -> !t.isCompleted()).collect(Collectors.toList());
    }

    public List<Task> getOverdueTasks()
    {
        return repository.findAll().stream().filter(Task::isOverdue).collect(Collectors.toList());
    }

    public Map<String, List<Task>> getTasksByCategory()
    {
        return repository.findAll().stream().collect(Collectors.groupingBy(Task::getCategory));
    }

    public List<Task> getTaskSortedByDueDate()
    {
        return repository.findAll().stream().filter(t -> t.getDueDate() != null).sorted(Comparator.comparing(Task::getDueDate)).collect(Collectors.toList());
    }

    public long countPending()
    {
        return repository.findAll().stream().filter(t -> !t.isCompleted()).count();
    }

    public List<WorkTask> getHighPriorityWorkTasks()
    {
        return repository.findAll().stream().filter(t -> t instanceof WorkTask wt && (wt.getUrgency() == WorkTask.Urgency.HIGH
                                                            || wt.getUrgency() == WorkTask.Urgency.CRITICAL)).map(t -> (WorkTask) t).collect(Collectors.toList());
    }

    public String getSummary()
    {
        List<Task> all = repository.findAll();
        long completed = all.stream().filter(Task::isCompleted).count();
        long overdue = all.stream().filter(Task::isOverdue).count();
        return String.format("Total: %d | Completed: %d | Pending: %d | Overdue: %d", all.size(), completed, all.size() - completed, overdue);
    }


}