package com.twotrack.notebook.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twotrack.notebook.entity.Reference;
import com.twotrack.notebook.mapper.ReferenceMapper;
import com.twotrack.notebook.service.ReferenceService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReferenceServiceImpl implements ReferenceService {

    private final ReferenceMapper referenceMapper;

    public ReferenceServiceImpl(ReferenceMapper referenceMapper) {
        this.referenceMapper = referenceMapper;
    }

    @Override
    public Reference createReference(String sourceType, Long sourceId, String targetType, Long targetId) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 不能自引用
        if (sourceType.equals(targetType) && sourceId.equals(targetId)) {
            throw new IllegalArgumentException("不能自引用");
        }

        // 查重
        Reference exist = referenceMapper.selectOne(
                new LambdaQueryWrapper<Reference>()
                        .eq(Reference::getUserId, userId)
                        .eq(Reference::getSourceType, sourceType)
                        .eq(Reference::getSourceId, sourceId)
                        .eq(Reference::getTargetType, targetType)
                        .eq(Reference::getTargetId, targetId)
        );
        if (exist != null) {
            return exist;
        }

        Reference ref = new Reference();
        ref.setUserId(userId);
        ref.setSourceType(sourceType);
        ref.setSourceId(sourceId);
        ref.setTargetType(targetType);
        ref.setTargetId(targetId);
        referenceMapper.insert(ref);
        return ref;
    }

    @Override
    public void deleteReference(Long referenceId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Reference ref = referenceMapper.selectById(referenceId);
        if (ref == null) return;
        if (!ref.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除");
        }
        referenceMapper.deleteById(referenceId);
    }

    @Override
    public List<Reference> getBySource(String sourceType, Long sourceId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return referenceMapper.selectList(
                new LambdaQueryWrapper<Reference>()
                        .eq(Reference::getUserId, userId)
                        .eq(Reference::getSourceType, sourceType)
                        .eq(Reference::getSourceId, sourceId)
        );
    }

    @Override
    public List<Reference> getByTarget(String targetType, Long targetId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return referenceMapper.selectList(
                new LambdaQueryWrapper<Reference>()
                        .eq(Reference::getUserId, userId)
                        .eq(Reference::getTargetType, targetType)
                        .eq(Reference::getTargetId, targetId)
        );
    }
}
