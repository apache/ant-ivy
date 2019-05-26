/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ConfiguratorTest {

    public static class FileTester {
        private File file;

        public void setFile(File file) {
            this.file = file;
        }

        public File getFile() {
            return file;
        }
    }

    public static class City {
        private final List<Housing> housings = new ArrayList<>();

        private final List<Street> streets = new ArrayList<>();

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Housing> getHousings() {
            return housings;
        }

        public List<Street> getStreets() {
            return streets;
        }

        public void add(Housing h) {
            housings.add(h);
        }

        public void add(Street s) {
            streets.add(s);
        }
    }

    public static class Street {
        private Class<?> clazz;

        private final List<Tree> trees = new ArrayList<>();

        private final List<Person> walkers = new ArrayList<>();

        public List<Tree> getTrees() {
            return trees;
        }

        public void addConfiguredTree(Tree tree) {
            trees.add(tree);
        }

        public List<Person> getWalkers() {
            return walkers;
        }

        public void addConfiguredWalker(Map<String, String> walkerAttributes) {
            walkers.add(new Person(walkerAttributes.get("name")));
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public void setClazz(Class<?> clazz) {
            this.clazz = clazz;
        }
    }

    public abstract static class Housing {
        private final List<Room> rooms = new ArrayList<>();

        private boolean isEmpty;

        private Person proprietary;

        public List<Room> getRooms() {
            return rooms;
        }

        public void addRoom(Room r) {
            rooms.add(r);
        }

        public boolean isEmpty() {
            return isEmpty;
        }

        public void setEmpty(boolean isEmpty) {
            this.isEmpty = isEmpty;
        }

        public Person getProprietary() {
            return proprietary;
        }

        public void setProprietary(Person proprietary) {
            this.proprietary = proprietary;
        }
    }

    public static class House extends Housing {
    }

    public static class Tree {
        private short age;

        public short getAge() {
            return age;
        }

        public void setAge(short age) {
            this.age = age;
        }
    }

    public static class Flat extends Housing {
        private int stage;

        public int getStage() {
            return stage;
        }

        public void setStage(int stage) {
            this.stage = stage;
        }
    }

    public static class Room {
        private short surface;

        public short getSurface() {
            return surface;
        }

        public void setSurface(short surface) {
            this.surface = surface;
        }
    }

    public static class Person {
        private final String name;

        public Person(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private Configurator conf;

    @Before
    public void setUp() {
        conf = new Configurator();
    }

    @Test
    public void testSetRoot() {
        City city = new City();
        conf.setRoot(city);
        assertEquals(city, conf.getCurrent());
    }

    @Test
    public void testStringAttribute() {
        City city = new City();
        conf.setRoot(city);
        conf.setAttribute("name", "bordeaux");
        assertEquals("bordeaux", city.getName());
    }

    @Test
    public void testIntAttribute() {
        Flat flat = new Flat();
        conf.setRoot(flat);
        conf.setAttribute("stage", "4");
        assertEquals(4, flat.getStage());
    }

    @Test
    public void testBooleanAttribute() {
        Housing housing = new House();
        conf.setRoot(housing);
        conf.setAttribute("empty", "true");
        assertTrue(housing.isEmpty());
        conf.setAttribute("empty", "false");
        assertFalse(housing.isEmpty());
        conf.setAttribute("empty", "yes");
        assertTrue(housing.isEmpty());
        conf.setAttribute("empty", "no");
        assertFalse(housing.isEmpty());
        conf.setAttribute("empty", "on");
        assertTrue(housing.isEmpty());
        conf.setAttribute("empty", "off");
        assertFalse(housing.isEmpty());
    }

    @Test
    public void testClassAttribute() {
        Street street = new Street();
        conf.setRoot(street);
        conf.setAttribute("clazz", getClass().getName());
        assertEquals(getClass(), street.getClazz());
    }

    @Test
    public void testPersonAttribute() {
        Housing housing = new House();
        conf.setRoot(housing);
        conf.setAttribute("proprietary", "jean");
        assertEquals("jean", housing.getProprietary().getName());
    }

    @Test
    public void testAddRoom() {
        Housing housing = new House();
        conf.setRoot(housing);
        conf.startCreateChild("room");
        assertEquals(1, housing.getRooms().size());
        conf.setAttribute("surface", "24");
        assertEquals(24, housing.getRooms().get(0).getSurface());
        conf.endCreateChild();
        assertEquals(housing, conf.getCurrent());
    }

    @Test
    public void testAddConfiguredTree() {
        Street street = new Street();
        conf.setRoot(street);
        conf.startCreateChild("tree");
        assertTrue(street.getTrees().isEmpty());
        conf.setAttribute("age", "400");
        conf.endCreateChild();
        assertEquals(1, street.getTrees().size());
        assertEquals(400, street.getTrees().get(0).getAge());
        assertEquals(street, conf.getCurrent());
    }

    @Test
    public void testAddConfiguredWalker() {
        Street street = new Street();
        conf.setRoot(street);
        conf.startCreateChild("walker");
        assertTrue(street.getWalkers().isEmpty());
        conf.setAttribute("name", "xavier");
        conf.endCreateChild();
        assertEquals(1, street.getWalkers().size());
        assertEquals("xavier", street.getWalkers().get(0).getName());
        assertEquals(street, conf.getCurrent());
    }

    @Test
    public void testAddWithTypeDef() throws Exception {
        City city = new City();
        conf.typeDef("house", House.class.getName());
        conf.typeDef("flat", Flat.class.getName());
        conf.typeDef("street", Street.class.getName());
        conf.setRoot(city);
        conf.startCreateChild("house");
        assertEquals(1, city.getHousings().size());
        assertTrue(city.getHousings().get(0) instanceof House);
        conf.endCreateChild();
        conf.startCreateChild("flat");
        assertEquals(2, city.getHousings().size());
        assertTrue(city.getHousings().get(1) instanceof Flat);
        conf.endCreateChild();
        conf.startCreateChild("street");
        assertEquals(1, city.getStreets().size());
        conf.endCreateChild();
        assertEquals(city, conf.getCurrent());
    }

    @Test
    public void testNested() throws Exception {
        City city = new City();
        conf.typeDef("house", House.class.getName());
        conf.setRoot(city);
        conf.startCreateChild("house");
        conf.startCreateChild("room");
        conf.setAttribute("surface", "20");
        conf.endCreateChild();
        conf.startCreateChild("room");
        conf.setAttribute("surface", "25");
        conf.endCreateChild();
        conf.endCreateChild();
        assertEquals(city, conf.getCurrent());
        assertEquals(2, city.getHousings().get(0).getRooms().size());
        assertEquals(20, city.getHousings().get(0).getRooms().get(0).getSurface());
        assertEquals(25, city.getHousings().get(0).getRooms().get(1).getSurface());
    }

    @Test
    public void testMacro() throws Exception {
        City city = new City();
        conf.typeDef("house", House.class.getName());

        conf.startMacroDef("castle");
        conf.addMacroAttribute("surface", "40");
        conf.addMacroElement("addroom", true);
        conf.startCreateChild("house");
        conf.startCreateChild("room");
        conf.setAttribute("surface", "@{surface}");
        conf.endCreateChild();
        conf.startCreateChild("room");
        conf.setAttribute("surface", "@{surface}");
        conf.endCreateChild();
        conf.startCreateChild("addroom");
        conf.endCreateChild();
        conf.endCreateChild();
        conf.endMacroDef();

        conf.setRoot(city);
        conf.startCreateChild("castle");
        conf.setAttribute("surface", "10");
        conf.endCreateChild();

        conf.startCreateChild("castle");
        conf.startCreateChild("addroom");
        conf.startCreateChild("room");
        conf.setAttribute("surface", "20");
        conf.endCreateChild();
        conf.endCreateChild();
        conf.endCreateChild();

        assertEquals(city, conf.getCurrent());
        assertEquals(2, city.getHousings().size());

        // first castle : 2 default rooms of 10 of surface
        assertEquals(2, city.getHousings().get(0).getRooms().size());
        assertEquals(10, city.getHousings().get(0).getRooms().get(0).getSurface());
        assertEquals(10, city.getHousings().get(0).getRooms().get(1).getSurface());

        // second castle : 2 default rooms of default surface 40, + one addroom of surface 20
        assertEquals(3, city.getHousings().get(1).getRooms().size());
        assertEquals(40, city.getHousings().get(1).getRooms().get(0).getSurface());
        assertEquals(40, city.getHousings().get(1).getRooms().get(1).getSurface());
        assertEquals(20, city.getHousings().get(1).getRooms().get(2).getSurface());
    }

    @Test
    public void testFileAttribute() {
        FileTester root = new FileTester();
        conf.setRoot(root);
        conf.setAttribute("file", "path/to/file.txt");

        String filePath = root.getFile().getPath();
        filePath = filePath.replace('\\', '/');

        assertEquals("path/to/file.txt", filePath);
    }
}
