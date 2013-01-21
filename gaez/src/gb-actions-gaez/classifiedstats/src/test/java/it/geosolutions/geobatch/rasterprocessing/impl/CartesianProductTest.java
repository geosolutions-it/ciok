/*
 *  Copyright (C) 2007 - 2011 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * 
 *  GPLv3 + Classpath exception
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.geobatch.rasterprocessing.impl;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class CartesianProductTest {

    public CartesianProductTest() {
    }


    @Test
    public void testEmpty() {
        CartesianProduct cp = new CartesianProduct();

        assertFalse(cp.getMissingProducts().iterator().hasNext());
        assertEquals(0, cp.getMissingProducts().size());
    }

    @Test
    public void testFull2x2() {
        CartesianProduct cp = new CartesianProduct();
        cp.addClassificationKeys(new MultiKey("0", "0"));
        cp.addClassificationKeys(new MultiKey("0", "1"));
        cp.addClassificationKeys(new MultiKey("1", "0"));
        cp.addClassificationKeys(new MultiKey("1", "1"));
        
        assertFalse(cp.getMissingProducts().iterator().hasNext());
        assertEquals(0, cp.getMissingProducts().size());
    }

    @Test
    public void test3x3() {
        CartesianProduct cp = new CartesianProduct();
        cp.addClassificationKeys(new MultiKey("0", "0"));
        cp.addClassificationKeys(new MultiKey("1", "1"));
        cp.addClassificationKeys(new MultiKey("2", "2"));

        System.out.println(cp.getMissingProducts());
        assertEquals(6, cp.getMissingProducts().size());
    }

    @Test
    public void test2x3() {
        CartesianProduct cp = new CartesianProduct();
        cp.addClassificationKeys(new MultiKey("A", "A"));
        cp.addClassificationKeys(new MultiKey("B", "B"));
        cp.addClassificationKeys(new MultiKey("B", "C"));

        System.out.println(cp.getMissingProducts());
        assertEquals(3, cp.getMissingProducts().size());
    }


}
