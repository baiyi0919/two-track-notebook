package com.twotrack.notebook.service;

import com.twotrack.notebook.entity.Reference;
import java.util.List;

public interface ReferenceService {

    /** 创建引用关系 */
    Reference createReference(String sourceType, Long sourceId, String targetType, Long targetId);

    /** 删除引用 */
    void deleteReference(Long referenceId);

    /** 获取某实体的所有引用（source） */
    List<Reference> getBySource(String sourceType, Long sourceId);

    /** 获取引用某实体的所有反向链接（target） */
    List<Reference> getByTarget(String targetType, Long targetId);
}
