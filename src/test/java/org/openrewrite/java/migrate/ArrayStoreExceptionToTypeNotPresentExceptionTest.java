/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ArrayStoreExceptionToTypeNotPresentExceptionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.MethodExceptionReplacerRecipe");
    }

    @DocumentExample
    @Test
    void replaceCaughtException() {
        rewriteRun(
          //language=java
          java(
            """
              import java.lang.annotation.*;
              import java.util.*;

              public class Test {
                  public void testMethod() {
                      try {
                          Object o = "test";
                          o.getClass().getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          System.out.println("Caught Exception");
                      }
                      try {
                          Object.class.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          System.out.println("Caught ArrayStoreException");
                      }
                  }
              }
              """,
            """
              import java.lang.annotation.*;
              import java.util.*;

              public class Test {
                  public void testMethod() {
                      try {
                          Object o = "test";
                          o.getClass().getAnnotation(Override.class);
                      } catch (TypeNotPresentException e) {
                          System.out.println("Caught Exception");
                      }
                      try {
                          Object.class.getAnnotation(Override.class);
                      } catch (TypeNotPresentException e) {
                          System.out.println("Caught ArrayStoreException");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void retainOtherCaughtExceptions() {
        rewriteRun(
          //language=java
          java(
            """
              public class Test {
                  public void testMethod() {
                      try {
                          Object o = "test";
                          o.getClass().getAnnotation(Override.class);
                      } catch (NullPointerException e) {
                          System.out.println("Caught Exception");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void retainArrayStoreExceptionWithoutClassGetAnnotation() {
        rewriteRun(
          //language=java
          java(
            """
              public class Test {
                  public void testMethod() {
                      try {
                          Object o = "test";
                      } catch (ArrayStoreException e) {
                          System.out.println("Caught Exception");
                      }
                  }
              }
              """
          )
        );
    }
}
