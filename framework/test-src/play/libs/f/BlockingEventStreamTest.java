package play.libs.f;

import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import play.libs.F;

import static org.assertj.core.api.Assertions.assertThat;

class BlockingEventStreamTest {

    @Test void eventIsConsumedOnlyByAnActualRead() throws Exception {
        // ctx.channel().config().setAutoRead(...) is driven from
        // LazyTask.markAsRead, so deep stubs provide a plausible call chain
        // without extra stubbing
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class,
            Mockito.withSettings().defaultAnswer(Mockito.RETURNS_DEEP_STUBS));

        var stream = new F.BlockingEventStream<String>(10, ctx);

        var pending = stream.nextEvent();
        assertThat(pending).isNotDone();

        stream.publish("event");
        // delivering the event to a waiting subscriber does not consume it
        // (isDone() checks the state without reading the value through join())
        assertThat(pending).isDone();

        // reading consumes the event — exactly what Promise.waitAny and
        // Promise.waitEither do internally through join() on the winner
        assertThat(pending.join()).isEqualTo("event");

        // flow control resumes reading after the consumption
        Mockito.verify(ctx, Mockito.atLeastOnce()).channel();

        // regression for duplicated WebSocket messages: the consumed event
        // must not be handed out again
        assertThat(stream.nextEvent()).isNotDone();
    }

}
