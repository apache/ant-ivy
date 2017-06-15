/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.util;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        private final List<Housing> _housings = new ArrayList<>();

        private final List<Street> _streets = new ArrayList<>();

        private String _name;

        public String getName() {
            return _name;
        }

        public void setName(String name) {
            _name = name;
        }

        public List<Housing> getHousings() {
            return _housings;
        }

        public List<Street> getStreets() {
            return _streets;
        }

        public void add(Housing h) {
            _housings.add(h);
        }

        public void add(Street s) {
            _streets.add(s);
        }
    }

    public static class Street {
        private Class<?> _clazz;

        private final List<Tree> _trees = new ArrayList<>();

        private final List<Person> _walkers = new ArrayList<>();

        public List<Tree> getTrees() {
            return _trees;
        }

        public void addConfiguredTree(Tree tree) {
            _trees.add(tree);
        }

        public List<Person> getWalkers() {
            return _walkers;
        }

        public void addConfiguredWalker(Map<String, String> walkerAttributes) {
            _walkers.add(new Person(walkerAttributes.get("name")));
        }

        public Class<?> getClazz() {
            return _clazz;
        }

        public void setClazz(Class<?> clazz) {
            _clazz = clazz;
        }
    }

    public static abstract class Housing {
        private final List<Room> _rooms = new ArrayList<>();

        private boolean _isEmpty;

        private Person _proprietary;

        public List<Room> getRooms() {
            return _rooms;
        }

        public void addRoom(Room r) {
            _rooms.add(r);
        }

        public boolean isEmpty() {
            return _isEmpty;
        }

        public void setEmpty(boolean isEmpty) {
            _isEmpty = isEmpty;
        }

        public Person getProprietary() {
            return _proprietary;
        }

        public void setProprietary(Person proprietary) {
            _proprietary = proprietary;
        }
    }

    public static class House extends Housing {
    }

    public static class Tree {
        private short _age;

        public short getAge() {
            return _age;
        }

        public void setAge(short age) {
            _age = age;
        }
    }

    public static class Flat extends Housing {
        private int _stage;

        public int getStage() {
            return _stage;
        }

        public void setStage(int stage) {
            _stage = stage;
        }
    }

    public static class Room {
        private short _surface;

        public short getSurface() {
            return _surface;
        }

        public void setSurface(short surface) {
            _surface = surface;
        }
    }

    public static class Person {
        private final String _name;

        public Person(String name) {
            _name = name;
        }

        public String getName() {
            return _name;
        }
    }

    private Configurator _conf;

    @Before
    public void setUp() {
        _conf = new Configurator();
    }

    @Test
    public void testSetRoot() {
        City city = new City();
        _conf.setRoot(city);
        assertEquals(city, _conf.getCurrent());
    }

    @Test
    public void testStringAttribute() {
        City city = new City();
        _conf.setRoot(city);
        _conf.setAttribute("name", "bordeaux");
        assertEquals("bordeaux", city.getName());
    }

    @Test
    public void testIntAttribute() {
        Flat flat = new Flat();
        _conf.setRoot(flat);
        _conf.setAttribute("stage", "4");
        assertEquals(4, flat.getStage());
    }

    @Test
    public void testBooleanAttribute() {
        Housing housing = new House();
        _conf.setRoot(housing);
        _conf.setAttribute("empty", "true");
        assertEquals(true, housing.isEmpty());
        _conf.setAttribute("empty", "false");
        assertEquals(false, housing.isEmpty());
        _conf.setAttribute("empty", "yes");
        assertEquals(true, housing.isEmpty());
        _conf.setAttribute("empty", "no");
        assertEquals(false, housing.isEmpty());
        _conf.setAttribute("empty", "on");
        assertEquals(true, housing.isEmpty());
        _conf.setAttribute("empty", "off");
        assertEquals(false, housing.isEmpty());
    }

    @Test
    public void testClassAttribute() {
        Street street = new Street();
        _conf.setRoot(street);
        _conf.setAttribute("clazz", getClass().getName());
        assertEquals(getClass(), street.getClazz());
    }

    @Test
    public void testPersonAttribute() {
        Housing housing = new House();
        _conf.setRoot(housing);
        _conf.setAttribute("proprietary", "jean");
        assertEquals("jean", housing.getProprietary().getName());
    }

    @Test
    public void testAddRoom() {
        Housing housing = new House();
        _conf.setRoot(housing);
        _conf.startCreateChild("room");
        assertEquals(1, housing.getRooms().size());
        _conf.setAttribute("surface", "24");
        assertEquals(24, housing.getRooms().get(0).getSurface());
        _conf.endCreateChild();
        assertEquals(housing, _conf.getCurrent());
    }

    @Test
    public void testAddConfiguredTree() {
        Street street = new Street();
        _conf.setRoot(street);
        _conf.startCreateChild("tree");
        assertTrue(street.getTrees().isEmpty());
        _conf.setAttribute("age", "400");
        _conf.endCreateChild();
        assertEquals(1, street.getTrees().size());
        assertEquals(400, street.getTrees().get(0).getAge());
        assertEquals(street, _conf.getCurrent());
    }

    @Test
    public void testAddConfiguredWalker() {
        Street street = new Street();
        _conf.setRoot(street);
        _conf.startCreateChild("walker");
        assertTrue(street.getWalkers().isEmpty());
        _conf.setAttribute("name", "xavier");
        _conf.endCreateChild();
        assertEquals(1, street.getWalkers().size());
        assertEquals("xavier", street.getWalkers().get(0).getName());
        assertEquals(street, _conf.getCurrent());
    }

    @Test
    public void testAddWithTypeDef() throws Exception {
        City city = new City();
        _conf.typeDef("house", House.class.getName());
        _conf.typeDef("flat", Flat.class.getName());
        _conf.typeDef("street", Street.class.getName());
        _conf.setRoot(city);
        _conf.startCreateChild("house");
        assertEquals(1, city.getHousings().size());
        assertTrue(city.getHousings().get(0) instanceof House);
        _conf.endCreateChild();
        _conf.startCreateChild("flat");
        assertEquals(2, city.getHousings().size());
        assertTrue(city.getHousings().get(1) instanceof Flat);
        _conf.endCreateChild();
        _conf.startCreateChild("street");
        assertEquals(1, city.getStreets().size());
        _conf.endCreateChild();
        assertEquals(city, _conf.getCurrent());
    }

    @Test
    public void testNested() throws Exception {
        City city = new City();
        _conf.typeDef("house", House.class.getName());
        _conf.setRoot(city);
        _conf.startCreateChild("house");
        _conf.startCreateChild("room");
        _conf.setAttribute("surface", "20");
        _conf.endCreateChild();
        _conf.startCreateChild("room");
        _conf.setAttribute("surface", "25");
        _conf.endCreateChild();
        _conf.endCreateChild();
        assertEquals(city, _conf.getCurrent());
        assertEquals(2, city.getHousings().get(0).getRooms().size());
        assertEquals(20, city.getHousings().get(0).getRooms().get(0).getSurface());
        assertEquals(25, city.getHousings().get(0).getRooms().get(1).getSurface());
    }

    @Test
    public void testMacro() throws Exception {
        City city = new City();
        _conf.typeDef("house", House.class.getName());

        _conf.startMacroDef("castle");
        _conf.addMacroAttribute("surface", "40");
        _conf.addMacroElement("addroom", true);
        _conf.startCreateChild("house");
        _conf.startCreateChild("room");
        _conf.setAttribute("surface", "@{surface}");
        _conf.endCreateChild();
        _conf.startCreateChild("room");
        _conf.setAttribute("surface", "@{surface}");
        _conf.endCreateChild();
        _conf.startCreateChild("addroom");
        _conf.endCreateChild();
        _conf.endCreateChild();
        _conf.endMacroDef();

        _conf.setRoot(city);
        _conf.startCreateChild("castle");
        _conf.setAttribute("surface", "10");
        _conf.endCreateChild();

        _conf.startCreateChild("castle");
        _conf.startCreateChild("addroom");
        _conf.startCreateChild("room");
        _conf.setAttribute("surface", "20");
        _conf.endCreateChild();
        _conf.endCreateChild();
        _conf.endCreateChild();

        assertEquals(city, _conf.getCurrent());
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
        _conf.setRoot(root);
        _conf.setAttribute("file", "path/to/file.txt");

        String filePath = root.getFile().getPath();
        filePath = filePath.replace('\\', '/');

        assertEquals("path/to/file.txt", filePath);
    }
}
