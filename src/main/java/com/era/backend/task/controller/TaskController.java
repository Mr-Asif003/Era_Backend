package com.era.backend.task.controller;


import com.era.backend.common.ApiResponse;
import com.era.backend.task.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    TaskService taskService;

   @Autowired
   TaskController(TaskService taskService){
       this.taskService=taskService;
   }
    @GetMapping("")
    public ApiResponse getTasks(@PathVariable String userId){
        return taskService.getUserTasks(userId);
    }
}
