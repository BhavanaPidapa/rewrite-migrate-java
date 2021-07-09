/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.migrate

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Parser
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.config.Environment
import org.openrewrite.java.ChangeType
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.maven.AddDependency
import org.openrewrite.maven.MavenParser
import java.util.*

class JavaxToJakartaTest : JavaRecipeTest {
    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.migrate")
        .build()
        .activateRecipes("org.openrewrite.java.migrate.JavaxMigrationToJakarta")

    companion object {
        private val javax: String = """
            package javax.xml.bind.annotation;
            public class A {
                public static void stat() {}
                public void foo() {}
            }
        """.trimIndent()

        private val jakarta: String = """
            package jakarta.xml.bind.annotation;
            public class A {
                public static void stat() {}
                public void foo() {}
            }
        """.trimIndent()
    }

    @Test
    fun dontAddImportWhenNoChangesWereMade() = assertUnchanged(
        before = "public class B {}"
    )

    @Test
    fun changeImport() = assertChanged(
        dependsOn = arrayOf(javax, jakarta),
        before = """
            import javax.xml.bind.annotation.A;

            public class B {
            }
        """,
        after = """
            import jakarta.xml.bind.annotation.A;

            public class B {
            }
        """
    )

    @Test
    fun fullyQualifiedName() = assertChanged(
        dependsOn = arrayOf(javax, jakarta),
        before = "public class B extends javax.xml.bind.annotation.A {}",
        after = "public class B extends jakarta.xml.bind.annotation.A {}"
    )

    @Test
    fun annotation() = assertChanged(
        dependsOn = arrayOf("""
            package javax.xml.bind.annotation;
            public @interface A {}
        """.trimIndent(), """
            package jakarta.xml.bind.annotation;
            public @interface A {}
        """.trimIndent()),
        before = "@javax.xml.bind.annotation.A public class B {}",
        after = "@jakarta.xml.bind.annotation.A public class B {}"
    )

    // array types and new arrays
    @Test
    fun array() = assertChanged(
        dependsOn = arrayOf(javax, jakarta),
        before = """
            public class B {
               javax.xml.bind.annotation.A[] a = new javax.xml.bind.annotation.A[0];
            }
        """,
        after = """
            public class B {
               jakarta.xml.bind.annotation.A[] a = new jakarta.xml.bind.annotation.A[0];
            }
        """
    )

    @Test
    fun classDecl() = assertChanged(
        dependsOn = arrayOf(
            javax, jakarta,
            "public interface I1 {}",
            "public interface I2 {}"
        ),
        recipe = recipe.doNext(ChangeType("I1", "I2")),
        before = """
            public class B extends javax.xml.bind.annotation.A implements I1 {}
        """,
        after = """
            public class B extends jakarta.xml.bind.annotation.A implements I2 {}
        """
    )

    @Test
    fun method() = assertChanged(
        dependsOn = arrayOf(
            javax, jakarta,
            "package javax.xml.bind.annotation; public class NewException extends Throwable {}",
            "package jakarta.xml.bind.annotation; public class NewException extends Throwable {}"),
        before = """
            public class B {
               public javax.xml.bind.annotation.A foo() throws javax.xml.bind.annotation.NewException { return null; }
            }
        """,
        after = """
            public class B {
               public jakarta.xml.bind.annotation.A foo() throws jakarta.xml.bind.annotation.NewException { return null; }
            }
        """
    )

    @Test
    fun methodInvocationTypeParametersAndWildcard() = assertChanged(
        dependsOn = arrayOf(javax, jakarta),
        before = """
            import java.util.List;
            public class B {
               public <T extends javax.xml.bind.annotation.A> T generic(T n, List<? super javax.xml.bind.annotation.A> in);
               public void test() {
                   javax.xml.bind.annotation.A.stat();
                   this.<javax.xml.bind.annotation.A>generic(null, null);
               }
            }
        """,
        after = """
            import java.util.List;
            public class B {
               public <T extends jakarta.xml.bind.annotation.A> T generic(T n, List<? super jakarta.xml.bind.annotation.A> in);
               public void test() {
                   jakarta.xml.bind.annotation.A.stat();
                   this.<jakarta.xml.bind.annotation.A>generic(null, null);
               }
            }
        """
    )

    @Test
    fun multiCatch() = assertChanged(
        dependsOn = arrayOf(
            "package javax.xml.bind.annotation; public class NewException extends Throwable {}",
            "package jakarta.xml.bind.annotation; public class NewException extends Throwable {}"
        ),
        before = """
            public class B {
               public void test() {
                   try {}
                   catch(javax.xml.bind.annotation.NewException | RuntimeException e) {}
               }
            }
        """,
        after = """
            public class B {
               public void test() {
                   try {}
                   catch(jakarta.xml.bind.annotation.NewException | RuntimeException e) {}
               }
            }
        """
    )

    @Test
    fun multiVariable() = assertChanged(
        dependsOn = arrayOf(javax, jakarta),
        before = """
            public class B {
               javax.xml.bind.annotation.A f1, f2;
            }
        """,
        after = """
            public class B {
               jakarta.xml.bind.annotation.A f1, f2;
            }
        """
    )

    @Test
    fun newClass() = assertChanged(
        dependsOn = arrayOf(javax, jakarta),
        before = """
            public class B {
               javax.xml.bind.annotation.A a = new javax.xml.bind.annotation.A();
            }
        """,
        after = """
            public class B {
               jakarta.xml.bind.annotation.A a = new jakarta.xml.bind.annotation.A();
            }
        """
    )

    @Test
    fun parameterizedType() = assertChanged(
        dependsOn = arrayOf(javax, jakarta),
        before = """
            import java.util.Map;
            public class B {
               Map<javax.xml.bind.annotation.A, javax.xml.bind.annotation.A> m;
            }
        """,
        after = """
            import java.util.Map;
            public class B {
               Map<jakarta.xml.bind.annotation.A, jakarta.xml.bind.annotation.A> m;
            }
        """
    )

    @Test
    fun typeCast() = assertChanged(
        dependsOn = arrayOf(javax, jakarta),
        before = """
            public class B {
               javax.xml.bind.annotation.A a = (javax.xml.bind.annotation.A) null;
            }
        """,
        after = """
            public class B {
               jakarta.xml.bind.annotation.A a = (jakarta.xml.bind.annotation.A) null;
            }
        """
    )

    @Test
    fun classReference() = assertChanged(
        dependsOn = arrayOf(javax, jakarta),
        before = """
            public class A {
                Class<?> clazz = javax.xml.bind.annotation.A.class;
            }
        """,
        after = """
            public class A {
                Class<?> clazz = jakarta.xml.bind.annotation.A.class;
            }
        """
    )

    @Test
    fun methodSelect() = assertChanged(
        dependsOn = arrayOf(javax, jakarta),
        before = """
            public class B {
               javax.xml.bind.annotation.A a = null;
               public void test() { a.foo(); }
            }
        """,
        after = """
            public class B {
               jakarta.xml.bind.annotation.A a = null;
               public void test() { a.foo(); }
            }
        """
    )

    @Test
    fun staticImport() = assertChanged(
        dependsOn = arrayOf(javax, jakarta),
        before = """
            import static javax.xml.bind.annotation.A.stat;
            public class B {
                public void test() {
                    stat();
                }
            }
        """,
        after = """
            import static jakarta.xml.bind.annotation.A.stat;
            public class B {
                public void test() {
                    stat();
                }
            }
        """
    )

    @Test
    fun onlyIfUsing() {
        val recipe = AddDependency(
            "jakarta.xml.bind",
            "jakarta.xml.bind-api",
            "3.0.0",
            null,
            true,
            null,
            null,
            null,
            null,
            null,
            listOf("jakarta.xml.bind.*")
        )
        val javaSource = JavaParser.fromJavaVersion()
            .dependsOn(Collections.singletonList(Parser.Input.fromString(
            """
                    package jakarta.xml.bind;
                    public class MarshalException extends Exception {
                    }
                """)))
            .build().parse("""
                package org.openrewrite.java.testing;
                import jakarta.xml.bind.MarshalException;
                public class A {
                    MarshalException getMap() {
                        return new MarshalException();
                    }
                }
        """)[0]
        val mavenSource = MavenParser.builder().build().parse("""
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
              </dependencies>
            </project>
        """.trimIndent())[0]

        val sources: List<SourceFile> = listOf(javaSource, mavenSource)
        val results = recipe.run(sources, InMemoryExecutionContext{ error: Throwable -> throw error})
        val mavenResult = results.find { it.before === mavenSource }
        Assertions.assertThat(mavenResult).isNotNull

        Assertions.assertThat(mavenResult?.after?.print()).isEqualTo("""
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
                <dependency>
                  <groupId>jakarta.xml.bind</groupId>
                  <artifactId>jakarta.xml.bind-api</artifactId>
                  <version>3.0.0</version>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent())
    }
}
