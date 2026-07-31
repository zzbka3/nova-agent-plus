package com.cs.online.runtime;

import com.cs.online.context.Context;
import com.cs.online.execution.Execution;
import com.cs.online.resource.Resource;

public interface Runtime {

    Execution execute(Resource resource, Context context);
}
