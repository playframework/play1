package play.libs.f;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import play.libs.F;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;

class PromiseTest {

    static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    @AfterAll static void shutdownExecutorService() throws Exception {
        EXECUTOR.shutdown();
        if (!EXECUTOR.awaitTermination(10, SECONDS)) {
            assertThat(EXECUTOR.shutdownNow()).isEmpty();
        }
    }

    @Test void newIncompleteFutureShouldReturnPromise() {
        assertThat(new F.Promise<>().newIncompleteFuture())
            .isInstanceOf(F.Promise.class)
            .isNotDone();
    }

    @Test void simpleCompletedPromise() throws Exception {
        var promise = new F.Promise<>();
        var value = new Object();

        assertThat(promise.isDone()).isFalse();
        assertThat(promise.getOrNull()).isNull();

        // complete
        promise.invoke(value);

        assertThat(promise.isDone()).isTrue();
        assertThat(promise.getOrNull()).isSameAs(value);
        assertThat(promise.get()).isSameAs(value);
        assertThat(promise.get(10, MILLISECONDS)).isSameAs(value);
    }

    @Test void simpleCompletedExceptionallyPromise() {
        var promise = new F.Promise<>();
        var throwable = new Throwable();

        assertThat(promise.isDone()).isFalse();
        assertThat(promise.getOrNull()).isNull();

        // complete exceptionally
        promise.completeExceptionally(throwable);

        assertThat(promise.isDone()).isTrue();
        assertThat(promise.getOrNull()).isNull();
        assertThatThrownBy(promise::get)
            .isExactlyInstanceOf(ExecutionException.class)
            .hasCause(throwable);
        assertThatThrownBy(() -> promise.get(10, SECONDS))
            .isExactlyInstanceOf(ExecutionException.class)
            .hasCause(throwable);
    }

    @Test void getShouldReturnValueWhenPromiseCompletedInOtherThread() throws Exception {
        var promise = new F.Promise<>();
        var value = new Object();

        var latch = new CountDownLatch(1);
        EXECUTOR.submit(() -> assertThatNoException().isThrownBy(() -> {
            latch.await();
            promise.invoke(value);
        }));

        assertThat(promise.isDone()).isFalse();
        assertThat(promise.getOrNull()).isNull();

        latch.countDown();
        assertThat(promise.get()).isSameAs(value);

        assertThat(promise.isDone()).isTrue();
        assertThat(promise.getOrNull()).isSameAs(value);
    }

    @Test void getWithTimeoutShouldReturnValueWhenPromiseCompletedInOtherThread() throws Exception {
        var promise = new F.Promise<>();
        var value = new Object();

        var latch = new CountDownLatch(1);
        EXECUTOR.submit(() -> assertThatNoException().isThrownBy(() -> {
            latch.await();
            Thread.sleep(1_000L);
            promise.invoke(value);
        }));

        assertThat(promise.isDone()).isFalse();
        assertThat(promise.getOrNull()).isNull();

        latch.countDown();
        assertThat(promise.get(2, SECONDS)).isSameAs(value);

        assertThat(promise.isDone()).isTrue();
        assertThat(promise.getOrNull()).isSameAs(value);
    }

    @Test void getShouldThrowExecutionExceptionWhenPromiseCompletedExceptionallyInOtherThread() {
        var promise = new F.Promise<>();
        var throwable = new Throwable();

        var latch = new CountDownLatch(1);
        EXECUTOR.submit(() -> assertThatNoException().isThrownBy(() -> {
            latch.await();
            promise.completeExceptionally(throwable);
        }));

        assertThat(promise.isDone()).isFalse();
        assertThat(promise.getOrNull()).isNull();

        latch.countDown();
        assertThatThrownBy(promise::get)
            .isInstanceOf(ExecutionException.class)
            .hasCause(throwable);

        assertThat(promise.isDone()).isTrue();
    }

    @Test void getWithTimeoutShouldThrowExecutionExceptionWhenPromiseCompletedExceptionallyInOtherThread() {
        var promise = new F.Promise<>();
        var throwable = new Throwable();

        var latch = new CountDownLatch(1);
        EXECUTOR.submit(() -> assertThatNoException().isThrownBy(() -> {
            latch.await();
            Thread.sleep(1_000L);
            promise.completeExceptionally(throwable);
        }));

        assertThat(promise.isDone()).isFalse();
        assertThat(promise.getOrNull()).isNull();

        latch.countDown();
        assertThatThrownBy(() -> promise.get(2, SECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasCause(throwable);

        assertThat(promise.isDone()).isTrue();
    }

    @Test void getWithTimeoutShouldThrowTimeoutExceptionWhenPromiseDoesNotCompletedInTimeInOtherThread() {
        var promise = new F.Promise<>();
        var value = new Object();

        var completed = CompletableFuture.runAsync(() -> assertThatNoException().isThrownBy(() -> {
            Thread.sleep(3_000L);
            promise.invoke(value);
        }), EXECUTOR);

        assertThat(promise.isDone()).isFalse();
        assertThat(promise.getOrNull()).isNull();

        assertThatThrownBy(() -> promise.get(2, SECONDS))
            .isInstanceOf(TimeoutException.class);

        completed.join();
        assertThat(promise.isDone()).isTrue();
        assertThat(promise.getOrNull()).isSameAs(value);
    }

    @Test void onRedeemShouldInvokeGivenActionWhenPromiseIsCompleted() {
        var promise = new F.Promise<>();
        promise.invoke(new Object());

        var counter = new AtomicInteger();
        promise.onRedeem(p -> {
            counter.incrementAndGet();

            assertThat(p).isSameAs(promise);
        });

        assertThat(counter).hasValue(1);
    }

    @Test void onRedeemShouldNotInvokeGivenActionUntilPromiseIsCompleted() {
        var promise = new F.Promise<>();

        var counter = new AtomicInteger();
        promise.onRedeem(p -> {
            counter.incrementAndGet();

            assertThat(p).isSameAs(promise);
        });
        assertThat(counter).hasValue(0);

        promise.invoke(new Object());
        assertThat(counter).hasValue(1);
    }

    @Test void waitAllWithArrayShouldBeCompletedOnlyWhenAllGivenPromisesAreCompleted() {
        F.Promise<Object>[] promises = Stream.generate(() -> new F.Promise<>())
            .limit(5)
            .toArray(F.Promise[]::new);
        CompletableFuture<List<Object>> all = F.Promise.waitAll(promises);
        assertThat(all.isDone()).isFalse();

        var lastPromise = promises[promises.length - 1];
        for (F.Promise<Object> promise : promises) {
            promise.invoke(new Object());

            assertThat(all.isDone()).isEqualTo(promise == lastPromise);
        }

        assertThat(all).satisfies(
            f -> assertThat(f.join())
                .containsExactly(Stream.of(promises).map(F.Promise::getOrNull).toArray())
        );
    }

    @Test void waitAllWithCollectionShouldBeCompletedOnlyWhenAllGivenPromisesAreCompleted() {
        var promises = Stream.generate(() -> new F.Promise<>())
            .limit(5)
            .toList();
        var all = F.Promise.waitAll(promises);
        assertThat(all.isDone()).isFalse();

        var lastPromise = promises.get(promises.size() - 1);
        for (F.Promise<Object> promise : promises) {
            promise.invoke(new Object());

            assertThat(all.isDone()).isEqualTo(promise == lastPromise);
        }

        assertThat(all).isCompletedWithValue(promises.stream().map(F.Promise::getOrNull).toList());
    }

    @Test void wait2ShouldBeCompletedOnlyWhenAllGivenPromisesAreCompleted() {
        var intPromise = new F.Promise<Integer>();
        var longPromise = new F.Promise<Long>();

        var all = F.Promise.wait2(intPromise, longPromise);
        assertThat(all.isDone()).isFalse();

        intPromise.invoke(ThreadLocalRandom.current().nextInt());
        assertThat(all.isDone()).isFalse();

        longPromise.invoke(ThreadLocalRandom.current().nextLong());
        assertThat(all.isDone()).isTrue();

        assertThat(all).satisfies(
            f -> assertThat(f.join())
                .returns(intPromise.getOrNull(), v -> v._1)
                .returns(longPromise.getOrNull(), v -> v._2)
        );
    }

    @Test void wait3ShouldBeCompletedOnlyWhenAllGivenPromisesAreCompleted() {
        var intPromise = new F.Promise<Integer>();
        var longPromise = new F.Promise<Long>();
        var stringPromise = new F.Promise<String>();

        var all = F.Promise.wait3(intPromise, longPromise, stringPromise);
        assertThat(all.isDone()).isFalse();

        intPromise.invoke(ThreadLocalRandom.current().nextInt());
        assertThat(all.isDone()).isFalse();

        longPromise.invoke(ThreadLocalRandom.current().nextLong());
        assertThat(all.isDone()).isFalse();

        stringPromise.invoke("value");
        assertThat(all.isDone()).isTrue();

        assertThat(all).satisfies(
            f -> assertThat(f.join())
                .returns(intPromise.getOrNull(), v -> v._1)
                .returns(longPromise.getOrNull(), v -> v._2)
                .returns(stringPromise.getOrNull(), v -> v._3)
        );
    }

    @Test void wait4ShouldBeCompletedOnlyWhenAllGivenPromisesAreCompleted() {
        var intPromise = new F.Promise<Integer>();
        var longPromise = new F.Promise<Long>();
        var stringPromise = new F.Promise<String>();
        var bytePromise = new F.Promise<Byte>();

        var all = F.Promise.wait4(intPromise, longPromise, stringPromise, bytePromise);
        assertThat(all.isDone()).isFalse();

        intPromise.invoke(ThreadLocalRandom.current().nextInt());
        assertThat(all.isDone()).isFalse();

        longPromise.invoke(ThreadLocalRandom.current().nextLong());
        assertThat(all.isDone()).isFalse();

        stringPromise.invoke("value");
        assertThat(all.isDone()).isFalse();

        bytePromise.invoke((byte) ThreadLocalRandom.current().nextInt());
        assertThat(all.isDone()).isTrue();

        assertThat(all).satisfies(
            f -> assertThat(f.join())
                .returns(intPromise.getOrNull(), v -> v._1)
                .returns(longPromise.getOrNull(), v -> v._2)
                .returns(stringPromise.getOrNull(), v -> v._3)
                .returns(bytePromise.getOrNull(), v -> v._4)
        );
    }

    @Test void wait5ShouldBeCompletedOnlyWhenAllGivenPromisesAreCompleted() {
        var intPromise = new F.Promise<Integer>();
        var longPromise = new F.Promise<Long>();
        var stringPromise = new F.Promise<String>();
        var bytePromise = new F.Promise<Byte>();
        var objectPromise = new F.Promise<Object>();

        var all = F.Promise.wait5(intPromise, longPromise, stringPromise, bytePromise, objectPromise);
        assertThat(all.isDone()).isFalse();

        intPromise.invoke(ThreadLocalRandom.current().nextInt());
        assertThat(all.isDone()).isFalse();

        longPromise.invoke(ThreadLocalRandom.current().nextLong());
        assertThat(all.isDone()).isFalse();

        stringPromise.invoke("value");
        assertThat(all.isDone()).isFalse();

        bytePromise.invoke((byte) ThreadLocalRandom.current().nextInt());
        assertThat(all.isDone()).isFalse();

        objectPromise.invoke(new Object());
        assertThat(all.isDone()).isTrue();

        assertThat(all).satisfies(
            f -> assertThat(f.join())
                .returns(intPromise.getOrNull(), v -> v._1)
                .returns(longPromise.getOrNull(), v -> v._2)
                .returns(stringPromise.getOrNull(), v -> v._3)
                .returns(bytePromise.getOrNull(), v -> v._4)
                .returns(objectPromise.getOrNull(), v -> v._5)
        );
    }

    @Test void waitAnyShouldBeCompletedWhenAnyGivenPromisesIsCompleted() {
        F.Promise<Object>[] promises = Stream.generate(() -> new F.Promise<>())
            .limit(5)
            .toArray(F.Promise[]::new);
        CompletableFuture<Object> all = F.Promise.waitAny(promises);
        assertThat(all.isDone()).isFalse();

        var completed = promises[ThreadLocalRandom.current().nextInt(promises.length)];
        completed.invoke(new Object());

        assertThat(all.isDone()).isTrue();
        assertThat(all).satisfies(
            f -> assertThat(f.join()).isSameAs(completed.getOrNull())
        );

    }

    @Test void waitEither2ShouldBeCompletedWhenAnyGivenPromisesIsCompleted() {
        {
            var intPromise = new F.Promise<Integer>();
            var longPromise = new F.Promise<Long>();

            var all = F.Promise.waitEither(intPromise, longPromise);
            assertThat(all.isDone()).isFalse();

            intPromise.invoke(ThreadLocalRandom.current().nextInt());
            assertThat(all.isDone()).isTrue();

            assertThat(all).satisfies(
                f -> assertThat(f.join())
                    .returns(intPromise.getOrNull(), v -> v._1.get())
            );
        }
        {
            var intPromise = new F.Promise<Integer>();
            var longPromise = new F.Promise<Long>();

            var all = F.Promise.waitEither(intPromise, longPromise);
            assertThat(all.isDone()).isFalse();

            longPromise.invoke(ThreadLocalRandom.current().nextLong());
            assertThat(all.isDone()).isTrue();

            assertThat(all).satisfies(
                f -> assertThat(f.join())
                    .returns(longPromise.getOrNull(), v -> v._2.get())
            );
        }
    }

    @Test void waitEither3ShouldBeCompletedWhenAnyGivenPromisesIsCompleted() {
        {
            var intPromise = new F.Promise<Integer>();
            var longPromise = new F.Promise<Long>();
            var stringPromise = new F.Promise<String>();

            var all = F.Promise.waitEither(intPromise, longPromise, stringPromise);
            assertThat(all.isDone()).isFalse();

            intPromise.invoke(ThreadLocalRandom.current().nextInt());
            assertThat(all.isDone()).isTrue();

            assertThat(all).satisfies(
                f -> assertThat(f.join())
                    .returns(intPromise.getOrNull(), v -> v._1.get())
            );
        }
        {
            var intPromise = new F.Promise<Integer>();
            var longPromise = new F.Promise<Long>();
            var stringPromise = new F.Promise<String>();

            var all = F.Promise.waitEither(intPromise, longPromise, stringPromise);
            assertThat(all.isDone()).isFalse();

            longPromise.invoke(ThreadLocalRandom.current().nextLong());
            assertThat(all.isDone()).isTrue();

            assertThat(all).satisfies(
                f -> assertThat(f.join())
                    .returns(longPromise.getOrNull(), v -> v._2.get())
            );
        }
        {
            var intPromise = new F.Promise<Integer>();
            var longPromise = new F.Promise<Long>();
            var stringPromise = new F.Promise<String>();

            var all = F.Promise.waitEither(intPromise, longPromise, stringPromise);
            assertThat(all.isDone()).isFalse();

            stringPromise.invoke("value");
            assertThat(all.isDone()).isTrue();

            assertThat(all).satisfies(
                f -> assertThat(f.join())
                    .returns(stringPromise.getOrNull(), v -> v._3.get())
            );
        }
    }

    @Test void waitEither4ShouldBeCompletedWhenAnyGivenPromisesIsCompleted() {
        {
            var intPromise = new F.Promise<Integer>();
            var longPromise = new F.Promise<Long>();
            var stringPromise = new F.Promise<String>();
            var objectPromise = new F.Promise<Object>();

            var all = F.Promise.waitEither(intPromise, longPromise, stringPromise, objectPromise);
            assertThat(all.isDone()).isFalse();

            intPromise.invoke(ThreadLocalRandom.current().nextInt());
            assertThat(all.isDone()).isTrue();

            assertThat(all).satisfies(
                f -> assertThat(f.join())
                    .returns(intPromise.getOrNull(), v -> v._1.get())
            );
        }
        {
            var intPromise = new F.Promise<Integer>();
            var longPromise = new F.Promise<Long>();
            var stringPromise = new F.Promise<String>();
            var objectPromise = new F.Promise<Object>();

            var all = F.Promise.waitEither(intPromise, longPromise, stringPromise, objectPromise);
            assertThat(all.isDone()).isFalse();

            longPromise.invoke(ThreadLocalRandom.current().nextLong());
            assertThat(all.isDone()).isTrue();

            assertThat(all).satisfies(
                f -> assertThat(f.join())
                    .returns(longPromise.getOrNull(), v -> v._2.get())
            );
        }
        {
            var intPromise = new F.Promise<Integer>();
            var longPromise = new F.Promise<Long>();
            var stringPromise = new F.Promise<String>();
            var objectPromise = new F.Promise<Object>();

            var all = F.Promise.waitEither(intPromise, longPromise, stringPromise, objectPromise);
            assertThat(all.isDone()).isFalse();

            stringPromise.invoke("value");
            assertThat(all.isDone()).isTrue();

            assertThat(all).satisfies(
                f -> assertThat(f.join())
                    .returns(stringPromise.getOrNull(), v -> v._3.get())
            );
        }
        {
            var intPromise = new F.Promise<Integer>();
            var longPromise = new F.Promise<Long>();
            var stringPromise = new F.Promise<String>();
            var objectPromise = new F.Promise<>();

            var all = F.Promise.waitEither(intPromise, longPromise, stringPromise, objectPromise);
            assertThat(all.isDone()).isFalse();

            objectPromise.invoke(new Object());
            assertThat(all.isDone()).isTrue();

            assertThat(all).satisfies(
                f -> assertThat(f.join())
                    .returns(objectPromise.getOrNull(), v -> v._4.get())
            );
        }
    }


    @Test void waitEither5ShouldBeCompletedWhenAnyGivenPromisesIsCompleted() {
        {
            var intPromise = new F.Promise<Integer>();
            var longPromise = new F.Promise<Long>();
            var stringPromise = new F.Promise<String>();
            var objectPromise = new F.Promise<Object>();
            var bytePromise = new F.Promise<Byte>();

            var all = F.Promise.waitEither(intPromise, longPromise, stringPromise, objectPromise, bytePromise);
            assertThat(all.isDone()).isFalse();

            intPromise.invoke(ThreadLocalRandom.current().nextInt());
            assertThat(all.isDone()).isTrue();

            assertThat(all).satisfies(
                f -> assertThat(f.join())
                    .returns(intPromise.getOrNull(), v -> v._1.get())
            );
        }
        {
            var intPromise = new F.Promise<Integer>();
            var longPromise = new F.Promise<Long>();
            var stringPromise = new F.Promise<String>();
            var objectPromise = new F.Promise<Object>();
            var bytePromise = new F.Promise<Byte>();

            var all = F.Promise.waitEither(intPromise, longPromise, stringPromise, objectPromise, bytePromise);
            assertThat(all.isDone()).isFalse();

            longPromise.invoke(ThreadLocalRandom.current().nextLong());
            assertThat(all.isDone()).isTrue();

            assertThat(all).satisfies(
                f -> assertThat(f.join())
                    .returns(longPromise.getOrNull(), v -> v._2.get())
            );
        }
        {
            var intPromise = new F.Promise<Integer>();
            var longPromise = new F.Promise<Long>();
            var stringPromise = new F.Promise<String>();
            var objectPromise = new F.Promise<Object>();
            var bytePromise = new F.Promise<Byte>();

            var all = F.Promise.waitEither(intPromise, longPromise, stringPromise, objectPromise, bytePromise);
            assertThat(all.isDone()).isFalse();

            stringPromise.invoke("value");
            assertThat(all.isDone()).isTrue();

            assertThat(all).satisfies(
                f -> assertThat(f.join())
                    .returns(stringPromise.getOrNull(), v -> v._3.get())
            );
        }
        {
            var intPromise = new F.Promise<Integer>();
            var longPromise = new F.Promise<Long>();
            var stringPromise = new F.Promise<String>();
            var objectPromise = new F.Promise<>();
            var bytePromise = new F.Promise<Byte>();

            var all = F.Promise.waitEither(intPromise, longPromise, stringPromise, objectPromise, bytePromise);
            assertThat(all.isDone()).isFalse();

            objectPromise.invoke(new Object());
            assertThat(all.isDone()).isTrue();

            assertThat(all).satisfies(
                f -> assertThat(f.join())
                    .returns(objectPromise.getOrNull(), v -> v._4.get())
            );
        }
        {
            var intPromise = new F.Promise<Integer>();
            var longPromise = new F.Promise<Long>();
            var stringPromise = new F.Promise<String>();
            var objectPromise = new F.Promise<>();
            var bytePromise = new F.Promise<Byte>();

            var all = F.Promise.waitEither(intPromise, longPromise, stringPromise, objectPromise, bytePromise);
            assertThat(all.isDone()).isFalse();

            bytePromise.invoke((byte) ThreadLocalRandom.current().nextInt());
            assertThat(all.isDone()).isTrue();

            assertThat(all).satisfies(
                f -> assertThat(f.join())
                    .returns(bytePromise.getOrNull(), v -> v._5.get())
            );
        }
    }

}
