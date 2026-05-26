package com.twotrack.notebook.service;

import com.twotrack.notebook.dto.MessageCreateDto;
import com.twotrack.notebook.entity.Message;

import java.util.List;

public interface MessageService {

    /** 在议题下发送消息 */
    Message send(Long threadId, MessageCreateDto dto);

    /** 查询议题下的消息列表 */
    List<Message> listByThread(Long threadId);
}
