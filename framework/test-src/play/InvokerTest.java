package play;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class InvokerTest {

    @Nested
    class InvocationContextTest {

        @Test void constructorStringShouldCreateInvocationContextWithEmptyAnnotationsCollection() {
            String invocationType = "constructorString";
            var context = new Invoker.InvocationContext(invocationType);
            assertThat(context.getInvocationType()).isSameAs(invocationType);
            assertThat(context.getAnnotations()).isEmpty();
        }

        @Test void constructorStringAndListShouldCreateInvocationContextWithSameAnnotationsCollection() {
            String invocationType = "constructorStringAndList";
            var annotations = List.of(stubAnnotationsFor(A.class, B.class, C.class, D.class));
            var context = new Invoker.InvocationContext(invocationType, annotations);
            assertThat(context.getInvocationType()).isSameAs(invocationType);
            assertThat(context.getAnnotations()).isSameAs(annotations);
        }

        @Test void constructorStringAndOneDimensionArrayShouldCreateInvocationContextWithAnnotationsCollectionFromArray() {
            String invocationType = "constructorStringAndOneDimensionArray";
            var annotations = stubAnnotationsFor(A.class, B.class, C.class, D.class);
            var context = new Invoker.InvocationContext(invocationType, annotations);

            assertThat(context.getInvocationType()).isSameAs(invocationType);
            assertThat(context.getAnnotations()).satisfiesExactly(
                Arrays.stream(annotations)
                    .<Consumer<? super Annotation>>map(annotation -> actual -> assertThat(actual).isSameAs(annotation))
                    .toArray(Consumer[]::new)
            );
        }

        @Test void constructorStringAndTwoDimensionsArrayShouldCreateInvocationContextWithAnnotationsCollectionFromArray() {
            String invocationType = "constructorStringAndTwoDimensionsArray";
            var abAnnotations = stubAnnotationsFor(A.class, B.class);
            var cdAnnotations = stubAnnotationsFor(C.class, D.class);
            var context = new Invoker.InvocationContext(invocationType, abAnnotations, cdAnnotations);

            assertThat(context.getInvocationType()).isSameAs(invocationType);
            assertThat(context.getAnnotations()).satisfiesExactly(
                Stream.concat(Arrays.stream(abAnnotations), Arrays.stream(cdAnnotations))
                    .<Consumer<? super Annotation>>map(annotation -> actual -> assertThat(actual).isSameAs(annotation))
                    .toArray(Consumer[]::new)
            );
        }

        @Test void getAnnotationShouldReturnFirstAnnotationFromList() {
            var annotations = stubAnnotationsFor(A.class, B.class, C.class, B.class);
            var context = new Invoker.InvocationContext("someInvocationType", annotations);
            assertThat(context.getAnnotation(B.class)).isSameAs(annotations[1]);
        }

        @Test void getAnnotationShouldReturnNull() {
            var context = new Invoker.InvocationContext("someInvocationType", stubAnnotationsFor(A.class, B.class, C.class));
            assertThat(context.getAnnotation(D.class)).isNull();
        }

        @Test void isAnnotationPresentShouldReturnTrue() {
            var annotations = stubAnnotationsFor(A.class, B.class, C.class, B.class);
            var context = new Invoker.InvocationContext("someInvocationType", annotations);
            assertThat(context.isAnnotationPresent(C.class)).isTrue();
        }

        @Test void isAnnotationPresentShouldReturnFalse() {
            var context = new Invoker.InvocationContext("someInvocationType", stubAnnotationsFor(A.class, B.class, C.class));
            assertThat(context.isAnnotationPresent(D.class)).isFalse();
        }

        @SafeVarargs
        private Annotation[] stubAnnotationsFor(Class<? extends Annotation>... annotations) {
            return Arrays.stream(annotations).map(this::stubAnnotationFor).toArray(Annotation[]::new);
        }

        private <T extends Annotation> T stubAnnotationFor(Class<T> annotation) {
            InvocationHandler handler = (proxy, method, args) ->
                method.getName().equals("annotationType") ? annotation : method.invoke(proxy, args);

            return (T) Proxy.newProxyInstance(annotation.getClassLoader(), new Class[] { annotation }, handler);
        }

        private @interface A {}
        private @interface B {}
        private @interface C {}
        private @interface D {}

    }


}