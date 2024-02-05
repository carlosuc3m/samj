package sc.fiji.samj.gui.components;

import java.io.File;

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
