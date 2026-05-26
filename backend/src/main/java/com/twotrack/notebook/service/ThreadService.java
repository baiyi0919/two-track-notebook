package com.twotrack.notebook.service;

import com.twotrack.notebook.dto.ThreadCreateDto;
import com.twotrack.notebook.dto.ThreadUpdateDto;
import com.twotrack.notebook.entity.Thread;

import java.util.List;

public interface ThreadService {

    /** 创建议题 */
    Thread create(ThreadCreateDto dto);

    /** 查询当前用户的议题列表 */
    List<Thread> list(Integer status);

    /** 查询议题详情 */
    Thread getById(Long id);

    /** 更新议题（编辑 topic/description） */
    Thread update(Long id, ThreadUpdateDto dto);

    /** 删除议题（同时删除关联消息） */
    void delete(Long id);

    /** 关闭议题（标记为已总结）*/
    Thread close(Long id);
}
