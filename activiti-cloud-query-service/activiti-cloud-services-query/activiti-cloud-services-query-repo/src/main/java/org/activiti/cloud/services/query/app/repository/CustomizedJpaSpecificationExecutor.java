package org.activiti.cloud.services.query.app.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CustomizedJpaSpecificationExecutor<T> extends JpaSpecificationExecutor<T> {}
