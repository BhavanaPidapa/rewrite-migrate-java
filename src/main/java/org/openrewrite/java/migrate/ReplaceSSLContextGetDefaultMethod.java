/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate;

import org.openrewrite.*;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

public class ReplaceSSLContextGetDefaultMethod extends Recipe {
    @Override
    public String getDisplayName() {
        return "Replace `SSLContext.getDefault()` with `JSSEHelper.getInstance().getSSLContext(null, null, null)`";
    }

    @Override
    public String getDescription() {
        return "This recipe replaces calls to `SSLContext.getDefault()` with `JSSEHelper.getInstance().getSSLContext(null, null, null)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher sslContextGetDefaultMatcher = new MethodMatcher("javax.net.ssl.SSLContext getDefault()");
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                if (sslContextGetDefaultMatcher.matches(method)) {
                    JavaTemplate template = JavaTemplate.builder("JSSEHelper.getInstance().getSSLContext(null, null, null)")
                            .build();
                    return template.apply(getCursor(), method.getCoordinates().replace());
                }
                return super.visitMethodInvocation(method, executionContext);
            }
        };
    }
}
