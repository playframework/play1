import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import models.City;
import models.Entity2;

import org.junit.Before;
import org.junit.Test;

import play.Logger;
import play.data.binding.ParamNode;
import play.data.validation.Validation;
import play.libs.WS;
import play.libs.WS.FileParam;
import play.libs.WS.HttpResponse;
import play.mvc.Http.Header;
import play.mvc.Scope.Params;
import play.test.UnitTest;

import com.google.gson.JsonObject;

import controllers.Rest;


public class DataBindingUnitTest extends UnitTest {
    
    
    @Test
    public void testByteBinding() throws Exception{
        File fileToSend = new File(new URLDecoder().decode(getClass().getResource("/kiki.txt").getFile(), "UTF-8"));
        assertEquals("b.ba.length=749", WS.url("http://localhost:9003/DataBinding/bindBeanWithByteArray").files(new FileParam(fileToSend, "b.ba")).post().getString());  
    }
    
    @Test
    public void testEntity2StandardBinding() {
        City city = null;
        Entity2 entity2 = null;
        try {
            entity2 = new Entity2();

            Params params = new Params();
            params.put("entity2.a", "testNewEntity2");
            params.put("entity2.b", "true");
            params.put("entity2.c", "1");

            city = new City();
            city.name = "Name";
            city.save();
            params.put("entity2.city.id", city.getId().toString());
            params.put("entity2.city.name", "changeNameOfA");

            params.put("entity2.cities.id", city.getId().toString());

            Validation.clear();
            ParamNode rootParamNode = ParamNode.convert(params.all());
            entity2.edit(rootParamNode, "entity2");
            assertFalse(Validation.hasErrors());

            entity2.save();
            entity2.refresh();

            assertEquals("testNewEntity2", entity2.a);
            assertEquals(true, entity2.b);
            assertEquals(1, entity2.c);
            assertEquals(city.getId(), entity2.city.id);
            assertEquals("changeNameOfA", entity2.city.name);
            assertNotNull(entity2.cities);
            assertEquals(1, entity2.cities.size());
            assertEquals(city.name, entity2.cities.get(0).name);

        } finally {
            if (entity2 != null && entity2.id != null) {
                entity2.delete();
            }
            if (city != null && city.id != null) {
                city.delete();
            }
        }
    }

    @Test
    public void testEntity2NoBindingCascading() {
	City city = null;
        Entity2 entity2 = null;
        try {
            entity2 = new Entity2();

            Params params = new Params();
            params.put("entity2.a", "testNewEntity2");
            params.put("entity2.b", "true");
            params.put("entity2.c", "1");

            city = new City();
            city.name = "Name";
            city.save();
            params.put("entity2.city.id", city.getId().toString());
            params.put("entity2.city.name", "changeNameOfA");

            params.put("entity2.cities.id", city.getId().toString());

            Validation.clear();
            ParamNode rootParamNode = ParamNode.convert(params.all());
            entity2.edit(rootParamNode, "entity2");
            assertFalse(Validation.hasErrors());

            entity2.save();

            assertEquals("testNewEntity2", entity2.a);
            assertEquals(true, entity2.b);
            assertEquals(1, entity2.c);
            assertEquals(city.getId(), entity2.city.id);
            assertEquals("changeNameOfA", entity2.city.name);
            assertNotNull(entity2.cities);
            assertEquals(1, entity2.cities.size());
            assertEquals(city.name, entity2.cities.get(0).name);

            // Check that modification Company has not been save
            City dbCity = City.findById(city.getId());
            // Refresh to avoid cash issue
            dbCity.refresh();
            assertEquals(city.name, dbCity.name);
            assertNotEquals("changeNameOfA", dbCity.name);
            assertEquals("Name", dbCity.name);

        } finally {
            if (entity2 != null && entity2.id != null) {
                entity2.delete();
            }
            if (city != null && city.id != null) {
                city.delete();
            }
        }
    }

    @Test
    public void testEntity2NoBindingNoCollectionKey() {
        City city = null;
        Entity2 entity2 = null;
        try {
            entity2 = new Entity2();

            entity2.a = "testNewEntity2";
            entity2.b = true;
            entity2.c = 1;

            city = new City();
            city.name = "Name";
            city.save();

            entity2.city = city;
            entity2.cities = null;
            entity2.save();

            // edit gateway
            Params params = new Params();
            params.put("entity2.cities", city.getId().toString());

            Validation.clear();
            ParamNode rootParamNode = ParamNode.convert(params.all());
            entity2.edit(rootParamNode, "entity2");

            // Here we have a problem has the payment gateways can be modified
            assertFalse(Validation.hasErrors());

            entity2.save();
            entity2.refresh();

            assertEquals(0, entity2.cities.size());
        } finally {
            if (entity2 != null && entity2.id != null) {
                entity2.delete();
            }
            if (city != null && city.id != null) {
                city.delete();
            }
        }
    }

    @Test
    public void testEntity2NoBindingNoCollectionKeyNull() {
        City city = null;
        Entity2 entity2 = null;
        try {
            entity2 = new Entity2();

            entity2.a = "testNewEntity2";
            entity2.b = true;
            entity2.c = 1;

            city = new City();
            city.name = "Name";
            city.save();

            entity2.city = city;
            entity2.cities = new ArrayList<City>();
            entity2.cities.add(city);
            entity2.save();

            // edit gateway
            Params params = new Params();
            params.put("entity2.cities", (String) null);

            Validation.clear();
            ParamNode rootParamNode = ParamNode.convert(params.all());
            entity2.edit(rootParamNode, "entity2");

            // Here we have a problem has the payment gateways can be modified
            assertFalse(Validation.hasErrors());

            entity2.save();
            entity2.refresh();

            assertEquals(1, entity2.cities.size());
            assertEquals("Name", entity2.cities.get(0).name);
        } finally {
            if (entity2 != null && entity2.id != null) {
                entity2.delete();
            }
            if (city != null && city.id != null) {
                city.delete();
            }
        }
    }

    @Test
    public void testEntity2NoBindingCollectionKeyUndefined() {
        City city = null;
        Entity2 entity2 = null;
        try {
            entity2 = new Entity2();

            entity2.a = "testNewEntity2";
            entity2.b = true;
            entity2.c = 1;

            city = new City();
            city.name = "Name";
            city.save();

            entity2.city = city;
            entity2.cities = new ArrayList<City>();
            entity2.cities.add(city);
            entity2.save();

            // Try with id but set to empty
            Params params = new Params();
            params.put("entity2.cities.id", "");

            Validation.clear();
            ParamNode rootParamNode = ParamNode.convert(params.all());
            entity2.edit(rootParamNode, "entity2");
            assertFalse(Validation.hasErrors());

            entity2.save();
            entity2.refresh();

            assertEquals(0, entity2.cities.size());
        } finally {
            if (entity2 != null && entity2.id != null) {
                entity2.delete();
            }
            if (city != null && city.id != null) {
                city.delete();
            }
        }
    }

    @Test
    public void testEntity2NoBindingCollectionKeyNull() {
        City city = null;
        Entity2 entity2 = null;
        try {
            entity2 = new Entity2();

            entity2.a = "testNewEntity2";
            entity2.b = true;
            entity2.c = 1;

            city = new City();
            city.name = "Name";
            city.save();

            entity2.city = city;
            entity2.cities = new ArrayList<City>();
            entity2.cities.add(city);
            entity2.save();

            // Try with id but set to null
            Params params = new Params();
            params.put("entity2.cities.id", (String) null);

            Validation.clear();
            ParamNode rootParamNode = ParamNode.convert(params.all());
            entity2.edit(rootParamNode, "entity2");
            assertFalse(Validation.hasErrors());

            entity2.save();
            entity2.refresh();

            assertEquals(0, entity2.cities.size());
        } finally {
            if (entity2 != null && entity2.id != null) {
                entity2.delete();
            }
            if (city != null && city.id != null) {
                city.delete();
            }
        }
    }

    @Test
    public void testEntity2NoBindingCollectionKeyOther() {
        City city = null;
        Entity2 entity2 = null;
        try {

            entity2 = new Entity2();

            entity2.a = "testNewEntity2";
            entity2.b = true;
            entity2.c = 1;

            city = new City();
            city.name = "Name";
            city.save();

            entity2.city = city;
            entity2.cities = null;
            entity2.save();

            // Try to bind with other field
            Validation.clear();
            Params params = new Params();
            params.put("entity2.cities.a", city.name);

            ParamNode rootParamNode = ParamNode.convert(params.all());
            entity2.edit(rootParamNode, "entity2");
            assertTrue(Validation.hasErrors());

            entity2.save();
            entity2.refresh();

            assertNotNull(entity2.cities);
            assertEquals(0, entity2.cities.size());
        } finally {
            if (entity2 != null && entity2.id != null) {
                entity2.delete();
            }
            if (city != null && city.id != null) {
                city.delete();
            }
        }
    }

    @Test
    public void testEntity2NoBindingCollectionWithIndex() {
        City city = null;
        Entity2 entity2 = null;
        try {
            entity2 = new Entity2();

            entity2.a = "testNewEntity2";
            entity2.b = true;
            entity2.c = 1;

            city = new City();
            city.name = "Name";
            city.save();

            entity2.city = city;
            entity2.cities = null;
            entity2.save();

            // Try to bind with other field
            Validation.clear();
            Params params = new Params();
            params.put("entity2.cities.id.1", city.getId().toString());

            ParamNode rootParamNode = ParamNode.convert(params.all());
            entity2.edit(rootParamNode, "entity2");
            assertTrue(Validation.hasErrors());

            entity2.save();
            entity2.refresh();

            assertNotNull(entity2.cities);
            assertEquals(0, entity2.cities.size());
        } finally {
            if (entity2 != null && entity2.id != null) {
                entity2.delete();
            }
            if (city != null && city.id != null) {
                city.delete();
            }
        }
    }

    @Test
    public void testEntity2NoBindingCollectionNoParams() {
        City city = null;
        Entity2 entity2 = null;
        try {
            entity2 = new Entity2();

            entity2.a = "testNewEntity2";
            entity2.b = true;
            entity2.c = 1;

            city = new City();
            city.name = "Name";
            city.save();

            entity2.city = city;
            entity2.cities = new ArrayList<City>();
            entity2.cities.add(city);
            entity2.save();

            Params params = new Params();

            Validation.clear();
            ParamNode rootParamNode = ParamNode.convert(params.all());
            entity2.edit(rootParamNode, "entity2");
            assertFalse(Validation.hasErrors());

            entity2.save();
            entity2.refresh();

            assertEquals(1, entity2.cities.size());
            assertEquals(city.name, entity2.cities.get(0).name);
        } finally {
            if (entity2 != null && entity2.id != null) {
                entity2.delete();
            }
            if (city != null && city.id != null) {
                city.delete();
            }
        }
    }

    @Test
    public void testEntity2NoBindingCollectionRootKeyNull() {
        City city = null;
        Entity2 entity2 = null;
        try {
            entity2 = new Entity2();

            entity2.a = "testNewEntity2";
            entity2.b = true;
            entity2.c = 1;

            city = new City();
            city.name = "Name";
            city.save();

            entity2.city = city;
            entity2.cities = new ArrayList<City>();
            entity2.cities.add(city);
            entity2.save();

            Params params = new Params();
            params.put("entity2", (String) null);

            Validation.clear();
            ParamNode rootParamNode = ParamNode.convert(params.all());
            entity2.edit(rootParamNode, "entity2");
            assertFalse(Validation.hasErrors());

            entity2.save();
            entity2.refresh();

            assertEquals(1, entity2.cities.size());
            assertEquals(city.name, entity2.cities.get(0).name);
        } finally {
            if (entity2 != null && entity2.id != null) {
                entity2.delete();
            }
            if (city != null && city.id != null) {
                city.delete();
            }
        }
    }

}
