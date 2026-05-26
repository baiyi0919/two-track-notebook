package com.twotrack.notebook.service;

import com.twotrack.notebook.dto.TaskCreateDto;
import com.twotrack.notebook.dto.TaskUpdateDto;
import com.twotrack.notebook.entity.Task;

import java.util.List;

public interface TaskService {

    /** 创建任务 */
    Task create(TaskCreateDto dto);

    /** 查询当前用户的任务列表（支持排序）*/
    List<Task> list(Integer status, String sortBy, String sortOrder);

    /** 查询单个任务详情 */
    Task getById(Long id);

    /** 更新任务 */
    Task update(Long id, TaskUpdateDto dto);

    /** 删除任务（软删除）*/
    void delete(Long id);

    /** 切换任务状态 */
    Task toggleStatus(Long id, Integer status);
}
