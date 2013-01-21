/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.codehaus.org/
 *  Copyright (C) 2007-2011 GeoSolutions S.A.S.
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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.collections.keyvalue.MultiKey;

/**
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class CartesianProduct {

    private Set<String> set0 = new TreeSet<String>();
    private Set<String> set1 = new TreeSet<String>();
    private Set<Pair<String, String>> fullSet = new HashSet<Pair<String, String>>();

    public void addClassificationKeys(MultiKey key) {
        String key0 = String.valueOf(key.getKey(0));
        String key1 = String.valueOf(key.getKey(1));
        set0.add(key0);
        set1.add(key1);

        fullSet.add(new Pair<String, String>(key0, key1));
    }

    public Set<Pair<String, String>> getMissingProducts() {
        Set<Pair<String, String>> missing = new LinkedHashSet<Pair<String, String>>();
        for (String k0 : set0) {
            for (String k1 : set1) {
                Pair<String, String> p = new Pair<String, String>(k0, k1);
                if (!fullSet.contains(p)) {
                    missing.add(p);
                }
            }
        }

        return missing;
    }

    public static class Pair<E0, E1> {

        E0 val0;
        /**
		 * @return the val0
		 */
		public final E0 getVal0() {
			return val0;
		}

		/**
		 * @param val0 the val0 to set
		 */
		public final void setVal0(E0 val0) {
			this.val0 = val0;
		}

		/**
		 * @return the val1
		 */
		public final E1 getVal1() {
			return val1;
		}

		/**
		 * @param val1 the val1 to set
		 */
		public final void setVal1(E1 val1) {
			this.val1 = val1;
		}

		E1 val1;

        public Pair(E0 val0, E1 val1) {
            this.val0 = val0;
            this.val1 = val1;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Pair<E0, E1> other = (Pair<E0, E1>) obj;
            if (this.val0 != other.val0 && (this.val0 == null || !this.val0.equals(other.val0))) {
                return false;
            }
            if (this.val1 != other.val1 && (this.val1 == null || !this.val1.equals(other.val1))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 23 * hash + (this.val0 != null ? this.val0.hashCode() : 0);
            hash = 23 * hash + (this.val1 != null ? this.val1.hashCode() : 0);
            return hash;
        }

        @Override
        public String toString() {
            return "Pair[" + val0 + ", " + val1 + ']';
        }
    }

}
