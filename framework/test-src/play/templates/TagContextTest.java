package play.templates;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Deque;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

public class TagContextTest {

    private static ThreadLocal<Deque<TagContext>> CURRENT_STACK;

    @BeforeClass
    public static void getCurrentStackThreadLocal() {
        assertThatNoException().isThrownBy(() -> {
            Field currentStackField = TagContext.class.getDeclaredField("currentStack");
            assertThat(currentStackField)
                .extracting(Field::getType)
                .isEqualTo(ThreadLocal.class);

            currentStackField.setAccessible(true);
            CURRENT_STACK = (ThreadLocal<Deque<TagContext>>) currentStackField.get(null);
        });
    }

    @After
    public void clearCurrentStack() {
        CURRENT_STACK.remove();
    }

    @Test
    public void lifecycle() {
        assertThat(CURRENT_STACK.get())
            .as("before init")
            .isNull();

        TagContext.init();
        assertThat(CURRENT_STACK.get())
            .as("after init()")
            .singleElement()
            .extracting("tagName")
            .isEqualTo("ROOT");

        TagContext.enterTag("new-parent-tag");
        assertThat(CURRENT_STACK.get())
            .as("after enterTag(\"new-parent-tag\")")
            .hasSize(2)
            .first()
            .extracting("tagName")
            .isEqualTo("new-parent-tag");

        TagContext.enterTag("new-child-tag");
        assertThat(CURRENT_STACK.get())
            .as("after enterTag(\"new-child-tag\")")
            .hasSize(3)
            .first()
            .extracting("tagName")
            .isEqualTo("new-child-tag");

        TagContext.exitTag();
        assertThat(CURRENT_STACK.get())
            .as("after exitTag(): new-child-tag")
            .hasSize(2)
            .first()
            .extracting("tagName")
            .isEqualTo("new-parent-tag");

        TagContext.exitTag();
        assertThat(CURRENT_STACK.get())
            .as("after exitTag(): new-parent-tag")
            .singleElement()
            .extracting("tagName")
            .isEqualTo("ROOT");
    }

    @Test
    public void currentShouldReturnLastElement() {
        TagContext.init();

        assertThat(TagContext.current())
            .as("empty stack")
            .extracting("tagName")
            .isEqualTo("ROOT");

        asList("grandfather", "father", "child").forEach(tag -> {
            TagContext.enterTag(tag);
            assertThat(TagContext.current())
                .as("after enterTag(\"%s\")", tag)
                .extracting("tagName")
                .isEqualTo(tag);
        });
    }

    @Test
    public void parentShouldSearchTags() {
        TagContext.init();
        assertThat(TagContext.parent()).isNull();

        asList("grandfather", "father", "child").forEach(TagContext::enterTag);

        assertThat(TagContext.parent())
            .as("parent()")
            .extracting("tagName")
            .isEqualTo("father");

        assertThat(TagContext.parent("grandfather"))
            .as("parent(\"grandfather\")")
            .extracting("tagName")
            .isEqualTo("grandfather");

        assertThat(TagContext.parent("grandfather"))
            .as("parent(\"father\")")
            .extracting("tagName")
            .isEqualTo("grandfather");

        assertThat(TagContext.parent("child"))
            .as("parent(\"child\")")
            .isNull();

        assertThat(TagContext.parent("grandpa"))
            .as("parent(\"grandpa\")")
            .isNull();

        assertThat(TagContext.parent("child", "grandfather", "father"))
            .as("parent(\"child\", \"grandfather\", \"father\")")
            .isNotNull()
            .extracting("tagName")
            .isEqualTo("father");

        assertThat(TagContext.parent("qwer", "asdf", "zxcv"))
            .as("parent(\"qwer\", \"asdf\", \"zxcv\")")
            .isNull();
    }

    @Test
    public void hasParentShouldSearchTags() {
        TagContext.init();
        assertThat(TagContext.hasParentTag("any-tag")).isFalse();

        asList("grandfather", "father", "child").forEach(TagContext::enterTag);

        // assertThat(TagContext.hasParentTag("child")).isFalse();
        assertThat(TagContext.hasParentTag("child")).isTrue();
        assertThat(TagContext.hasParentTag("father")).isTrue();
        assertThat(TagContext.hasParentTag("grandfather")).isTrue();
        assertThat(TagContext.hasParentTag("any-other-tag")).isFalse();
    }

}