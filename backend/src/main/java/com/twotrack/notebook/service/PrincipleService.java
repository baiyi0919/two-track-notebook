package com.twotrack.notebook.service;

import com.twotrack.notebook.dto.PrincipleCreateDto;
import com.twotrack.notebook.dto.PrincipleUpdateDto;
import com.twotrack.notebook.entity.Principle;
import com.twotrack.notebook.entity.Task;

import java.util.List;

public interface PrincipleService {

    /** 创建原则（手动从议题提取）*/
    Principle create(PrincipleCreateDto dto);

    /** 查询当前用户的个人宪法（原则列表）*/
    List<Principle> list(String keyword);

    /** 查询单个原则 */
    Principle getById(Long id);

    /** 更新原则 */
    Principle update(Long id, PrincipleUpdateDto dto);

    /** 删除原则 */
    void delete(Long id);

    /** 查询关联到此原则的任务列表（反向关联）*/
    List<Task> getRelatedTasks(Long principleId);
}
