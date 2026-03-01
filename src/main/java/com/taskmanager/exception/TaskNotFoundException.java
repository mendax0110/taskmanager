package com.taskmanager.exception;

/**
 * Exception handling: custom backend exception
 */
public class TaskNotFoundException extends Exception
{
    private final int taskId;

    public TaskNotFoundException(int taskId)
    {
        super("Task not foudn with ID: " + taskId);
        this.taskId = taskId;
    }

    public int getTaskId() { return taskId; }
}