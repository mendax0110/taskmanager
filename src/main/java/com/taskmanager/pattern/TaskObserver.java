package com.taskmanager.pattern;

import com.taskmanager.model.Task;

/**
 * Observer pattern - one to many deps
 */
public interface TaskObserver
{
    void onTaskAdded(Task task);

    void onTaskCompleted(Task task);

    void onTaskDeleted(int taskId);
}