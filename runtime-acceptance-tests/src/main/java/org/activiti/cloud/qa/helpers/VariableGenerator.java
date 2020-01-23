package org.activiti.cloud.qa.helpers;

import java.util.HashMap;
import java.util.Map;

public class VariableGenerator {

    public static Map<String, Object> variables = new HashMap<String, Object>() {{
        put("var1", "one");
        put("var2", 2);
    }};

}
