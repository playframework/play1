import org.junit.*;
import play.test.*;
import play.cache.*;
import java.util.*;
import java.math.*;
import static play.data.binding.Binder.directBind;

import models.*;

public class DirectBindingTest extends UnitTest {
    
    @Test
    public void testString() throws Exception  {
        assertTrue(directBind("", String.class).equals(""));
        assertTrue(directBind(null, String.class) == null);
        assertTrue(directBind("10", String.class).equals("10"));
        assertTrue(directBind("test", String.class).equals("test"));
    }
    
    @Test
    public void testInteger() throws Exception  {
        assertTrue(directBind("", int.class).equals(0));
        assertTrue(directBind(null, int.class).equals(0));
        assertTrue(directBind("0", int.class).equals(0));
        assertTrue(directBind("10", int.class).equals(10));
        assertTrue(directBind("-10", int.class).equals(-10));
        assertTrue(directBind("10.25", int.class).equals(10));
        assertTrue(directBind("", Integer.class) == null);
        assertTrue(directBind(null, Integer.class) == null);
        assertTrue(directBind("0", Integer.class).equals(new Integer("0")));
        assertTrue(directBind("10", Integer.class).equals(new Integer("10")));
        assertTrue(directBind("-10", Integer.class).equals(new Integer("-10")));
        assertTrue(directBind("10.25", Integer.class).equals(new Integer("10")));
    }
    
    @Test
    public void testEnum() throws Exception  {
        assertNull(directBind("", Factory.Color.class));
        assertTrue(directBind("RED", Factory.Color.class).equals(Factory.Color.RED));
    }
    
    @Test
    public void testLong() throws Exception  {
        assertTrue(directBind("", long.class).equals(0l));
        assertTrue(directBind(null, long.class).equals(0l));
        assertTrue(directBind("0", long.class).equals(0l));
        assertTrue(directBind("10", long.class).equals(10l));
        assertTrue(directBind("-10", long.class).equals(-10l));
        assertTrue(directBind("10.25", long.class).equals(10l));
        assertTrue(directBind("", Long.class) == null);
        assertTrue(directBind(null, Long.class) == null);
        assertTrue(directBind("0", Long.class).equals(new Long("0")));
        assertTrue(directBind("10", Long.class).equals(new Long("10")));
        assertTrue(directBind("-10", Long.class).equals(new Long("-10")));
        assertTrue(directBind("10.25", Long.class).equals(new Long("10")));
    }
    
    @Test
    public void testByte() throws Exception  {
        assertTrue(directBind("", byte.class).equals((byte)0));
        assertTrue(directBind(null, byte.class).equals((byte)0));
        assertTrue(directBind("0", byte.class).equals((byte)0));
        assertTrue(directBind("10", byte.class).equals((byte)10));
        assertTrue(directBind("-10", byte.class).equals((byte)-10));
        assertTrue(directBind("10.25", byte.class).equals((byte)10));
        assertTrue(directBind("", Byte.class) == null);
        assertTrue(directBind(null, Byte.class) == null);
        assertTrue(directBind("0", Byte.class).equals(new Byte("0")));
        assertTrue(directBind("10", Byte.class).equals(new Byte("10")));
        assertTrue(directBind("-10", Byte.class).equals(new Byte("-10")));
        assertTrue(directBind("10.25", Byte.class).equals(new Byte("10")));
    }
    
    @Test
    public void testShort() throws Exception  {
        assertTrue(directBind("", short.class).equals((short)0));
        assertTrue(directBind(null, short.class).equals((short)0));
        assertTrue(directBind("0", short.class).equals((short)0));
        assertTrue(directBind("10", short.class).equals((short)10));
        assertTrue(directBind("-10", short.class).equals((short)-10));
        assertTrue(directBind("10.25", short.class).equals((short)10));
        assertTrue(directBind("", Short.class) == null);
        assertTrue(directBind(null, Short.class) == null);
        assertTrue(directBind("0", Short.class).equals(new Short("0")));
        assertTrue(directBind("10", Short.class).equals(new Short("10")));
        assertTrue(directBind("-10", Short.class).equals(new Short("-10")));
        assertTrue(directBind("10.25", Short.class).equals(new Short("10")));
    }
    
    @Test
    public void testFloat() throws Exception  {
        assertTrue(directBind("", float.class).equals(0f));
        assertTrue(directBind(null, float.class).equals(0f));
        assertTrue(directBind("0", float.class).equals(0f));
        assertTrue(directBind("10", float.class).equals(10f));
        assertTrue(directBind("-10", float.class).equals(-10f));
        assertTrue(directBind("10.25", float.class).equals(10.25f));
        assertTrue(directBind("", Float.class) == null);
        assertTrue(directBind(null, Float.class) == null);
        assertTrue(directBind("0", Float.class).equals(new Float("0")));
        assertTrue(directBind("10", Float.class).equals(new Float("10")));
        assertTrue(directBind("-10", Float.class).equals(new Float("-10")));
        assertTrue(directBind("10.25", Float.class).equals(new Float("10.25")));
    }
    
    @Test
    public void testDouble() throws Exception  {
        assertTrue(directBind("", double.class).equals(0d));
        assertTrue(directBind(null, double.class).equals(0d));
        assertTrue(directBind("0", double.class).equals(0d));
        assertTrue(directBind("10", double.class).equals(10d));
        assertTrue(directBind("-10", double.class).equals(-10d));
        assertTrue(directBind("10.25", double.class).equals(10.25d));
        assertTrue(directBind("", Double.class) == null);
        assertTrue(directBind(null, Double.class) == null);
        assertTrue(directBind("0", Double.class).equals(new Double("0")));
        assertTrue(directBind("10", Double.class).equals(new Double("10")));
        assertTrue(directBind("-10", Double.class).equals(new Double("-10")));
        assertTrue(directBind("10.25", Double.class).equals(new Double("10.25")));
    }
    
    @Test
    public void testBigDecimal() throws Exception  {        
        assertTrue(directBind("", BigDecimal.class) == null);
        assertTrue(directBind(null, BigDecimal.class) == null);
        assertTrue(directBind("0", BigDecimal.class).equals(new BigDecimal("0")));
        assertTrue(directBind("10", BigDecimal.class).equals(new BigDecimal("10")));
        assertTrue(directBind("-10", BigDecimal.class).equals(new BigDecimal("-10")));
        assertTrue(directBind("10.25", BigDecimal.class).equals(new BigDecimal("10.25")));
    }
    
    @Test
    public void testBoolean() throws Exception  {
        assertTrue(directBind("", boolean.class).equals(false));
        assertTrue(directBind(null, boolean.class).equals(false));
        assertTrue(directBind("nan", boolean.class).equals(false)); // new
        assertTrue(directBind("0", boolean.class).equals(false));
        assertTrue(directBind("1", boolean.class).equals(true));
        assertTrue(directBind("10", boolean.class).equals(false));
        assertTrue(directBind("on", boolean.class).equals(true));
        assertTrue(directBind("yes", boolean.class).equals(true));
        assertTrue(directBind("", Boolean.class) == null);
        assertTrue(directBind(null, Boolean.class) == null);
        assertTrue(directBind("nan", Boolean.class).equals(false)); // new
        assertTrue(directBind("0", Boolean.class).equals(false));
        assertTrue(directBind("1", Boolean.class).equals(true));
        assertTrue(directBind("10", Boolean.class).equals(false));
        assertTrue(directBind("on", Boolean.class).equals(true));
        assertTrue(directBind("yes", Boolean.class).equals(true));
    }
    
    @Test
    public void testLocale() throws Exception  {
        assertTrue(directBind("fr", Locale.class).equals(new Locale("fr")));
        assertTrue(directBind("FR", Locale.class).equals(new Locale("fr")));
        assertTrue(directBind("fr_CA", Locale.class).equals(new Locale("fr", "CA")));
        assertTrue(directBind("fr_ca", Locale.class).equals(new Locale("fr", "CA")));
        assertTrue(directBind("FR_CA", Locale.class).equals(new Locale("fr", "CA")));
        assertTrue(directBind("fr-CA", Locale.class).equals(new Locale("fr", "CA")));
        assertTrue(directBind("xy", Locale.class).equals(new Locale("xy")));	// it even works with unreal locales
        assertTrue(directBind("XY_VW", Locale.class).equals(new Locale("xy", "vw")));
    }
}