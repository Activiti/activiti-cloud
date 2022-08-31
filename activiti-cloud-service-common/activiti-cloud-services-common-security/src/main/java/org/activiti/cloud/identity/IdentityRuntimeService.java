package org.activiti.cloud.identity;

import java.util.List;
import org.activiti.cloud.identity.model.User;

public interface IdentityRuntimeService {

    List<User> findUsersByGroupId(String groupId);
}
