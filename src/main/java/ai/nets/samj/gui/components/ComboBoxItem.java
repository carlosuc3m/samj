/*-
 * #%L
 * Library to call models of the family of SAM (Segment Anything Model) from Java
 * %%
 * Copyright (C) 2024 SAMJ developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ai.nets.samj.gui.components;


import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public abstract class ComboBoxItem {
    final private int id;
    final private Object value;
    private static final String SELECT_IMAGE_STR = "Select image";

    public ComboBoxItem(int uniqueID, Object seq) {
        this.id = uniqueID;
        this.value = seq;
    }

    public ComboBoxItem() {
        this.id = -1;
        this.value = null;
    }

    public int getId() {
        return id;
    }

    public Object getValue() {
        return value;
    }
    
    public abstract String getImageName();
    
    public abstract < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval<T> getImageasImgLib2();

    @Override
    public String toString() {
    	if (value == null) return SELECT_IMAGE_STR;
        return getImageName();
    }
}
