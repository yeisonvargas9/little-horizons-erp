package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceStatus;

import java.util.List;
import java.util.Optional;

public interface ResourceDao {

    Resource save(Resource resource);

    Optional<Resource> findByCode(String code);

    List<Resource> findAll();

    List<ResourceCategory> findCategories();

    boolean updateStatus(int resourceId, ResourceStatus status);
}
