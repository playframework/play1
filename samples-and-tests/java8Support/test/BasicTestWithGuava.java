import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import models.User;

import org.junit.Test;

import play.test.UnitTest;

import com.google.common.collect.Lists;

public class BasicTestWithGuava extends UnitTest {

    /**
     * Basic Sort with Lambda Support<br/>
     * With the introduction of Lambdas, we can now bypass the anonymous inner
     * class and go achieve the same result with simple, functional semantics:
     */
    @Test
    public void whenSortingEntitiesByNameTest() {
        List<User> users = Lists.newArrayList(new User("Sophie", 10), new User("Bill", 12));
        Collections.sort(users, (final User u1, final User u2) -> u1.name.compareTo(u2.name));
        assertEquals("Bill", users.get(0).name);
        assertEquals(12, users.get(0).age);
    }

    /**
     * Basic Sorting with no Type Definitions<br/>
     * Simplify the expression by not specifying the type definitions – the
     * compiler is capable of inferring these on its own:
     */
    @Test
    public void givenLambdaShortFormWhenSortingEntitiesByNameTest() {
        List<User> users = Lists.newArrayList(new User("Sophie", 10), new User("Bill", 12));
        Collections.sort(users, (u1, u2) -> u1.getName().compareTo(u2.getName()));
        assertEquals("Bill", users.get(0).name);
        assertEquals(12, users.get(0).age);
    }

    /**
     * Sort using Reference to Static Method<br/>
     * Sort using a Lambda Expression with a reference to a static method.
     */
    @Test
    public void givenMethodDefinitionWhenSortingEntitiesByNameThenAgeTest() {
        List<User> users = Lists.newArrayList(new User("Sophie", 10), new User("Bill", 12));
        Collections.sort(users, User::compareByNameThenAge);
        assertEquals("Bill", users.get(0).name);
        assertEquals(12, users.get(0).age);
    }

    /**
     * Sort Extracted Comparators<br/>
     * We can also avoid defining even the comparison logic itself by using an
     * instance method reference and the Comparator.comparing method – which
     * extracts and creates a Comparable based on that function.
     */
    @Test
    public void givenInstanceMethodWhenSortingEntitiesByNameThenAgeTest() {
        List<User> users = Lists.newArrayList(new User("Sophie", 10), new User("Bill", 12));
        Collections.sort(users, Comparator.comparing(User::getName));
        assertEquals("Bill", users.get(0).name);
        assertEquals(12, users.get(0).age);
    }

    /**
     * Reverse Sort <br/>
     * JDK 8 has also introduced a helper method for reversing the comparator –
     * we can make quick use of that to reverse our sort
     */
    @Test
    public void whenSortingEntitiesByNameReversed() {
        List<User> users = Lists.newArrayList(new User("Sophie", 10), new User("Bill", 12));
        Comparator<User> comparator = (u1, u2) -> u1.getName().compareTo(u2.getName());
        Collections.sort(users, comparator.reversed());
        assertEquals("Sophie", users.get(0).name);
        assertEquals(10, users.get(0).age);
    }

    /**
     * Sort with Multiple Conditions<br/>
     * The comparison lambda expressions need not be this simple – we can write
     * more complex expressions as well – for example sorting the entities first
     * by name, and then by age:
     */
    @Test
    public void whenSortingEntitiesByNameThenAge_thenCorrectlySorted() {
        List<User> users = Lists.newArrayList(new User("Sophie", 12), new User("Sophie", 10), new User("Tom", 12));
        Collections.sort(users, (lus, rus) -> {
            if (lus.getName().equals(rus.getName())) {
                return lus.age - rus.age;
            } else {
                return lus.name.compareTo(rus.name);
            }
        });
        assertEquals("Sophie", users.get(0).name);
        assertEquals(10, users.get(0).age);
    }

    /**
     * Sort with Multiple Conditions – Composition<br/>
     * 
     * The same comparison logic – first sorting by name and then, secondarily,
     * by age – can also be implemented by the new composition support for
     * Comparator.
     * 
     * Starting with JDK 8, we can now chain together multiple comparators to
     * build more complex comparison logic:
     */
    @Test
    public void givenComposition_whenSortingEntitiesByNameThenAge_thenCorrectlySorted() {
        List<User> users = Lists.newArrayList(new User("Sophie", 12), new User("Sophie", 10), new User("Tom", 12));
        Comparator<User> byName = (u1, u2) -> u1.name.compareTo(u2.name);
        Comparator<User> byAge = (u1, u2) -> Integer.compare(u1.age, u2.age);
        Collections.sort(users, byName.thenComparing(byAge));
        assertEquals("Sophie", users.get(0).name);
        assertEquals(10, users.get(0).age);
    }

}
