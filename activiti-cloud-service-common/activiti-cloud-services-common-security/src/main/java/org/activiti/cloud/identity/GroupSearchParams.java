package org.activiti.cloud.identity;

import java.util.Set;
import org.springframework.data.domain.Pageable;

public class GroupSearchParams {

    private String search;
    private Set<String> roles;
    private Integer page;
    private Integer size;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public void setFromPageable(Pageable pageable) {
        setPage(pageable.getPageNumber());
        setSize(pageable.getPageSize());
    }
}
