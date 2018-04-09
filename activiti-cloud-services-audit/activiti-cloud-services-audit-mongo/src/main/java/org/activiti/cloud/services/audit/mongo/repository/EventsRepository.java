package org.activiti.cloud.services.audit.mongo.repository;

import java.util.List;

import org.activiti.cloud.services.audit.mongo.events.ProcessEngineEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(exported = false)
public interface EventsRepository extends MongoRepository<ProcessEngineEventDocument, String>, QuerydslPredicateExecutor<ProcessEngineEventDocument> {
    
    @Override
    @RestResource(exported = false)
    public <S extends ProcessEngineEventDocument> S insert(S entity);
    
    @Override
    @RestResource(exported = false)
    public <S extends ProcessEngineEventDocument> List<S> insert(Iterable<S> entities);
    
    @Override
    @RestResource(exported = false)
    public <S extends ProcessEngineEventDocument> List<S> saveAll(Iterable<S> entities);
    
    @Override
    @RestResource(exported = false)
    public void delete(ProcessEngineEventDocument entity);
    
    @Override
    @RestResource(exported = false)
    public void deleteAll();
    
    @Override
    @RestResource(exported = false)
    public void deleteAll(Iterable<? extends ProcessEngineEventDocument> entities);
    
    @Override
    @RestResource(exported = false)
    public void deleteById(String id);
    
    @Override
    @RestResource(exported = false)
    public <S extends ProcessEngineEventDocument> S save(S entity);
}
