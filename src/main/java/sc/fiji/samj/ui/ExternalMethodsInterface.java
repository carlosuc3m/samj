package sc.fiji.samj.ui;

import java.io.File;
import java.util.List;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import sc.fiji.samj.gui.components.ComboBoxItem;

/**
 * @author Carlos Javier Garcia Lopez de Haro
 */
public interface ExternalMethodsInterface {

	/**
	 * 
	 * @param <T>
	 * @param file
	 * @return
	 */
	public < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval<T> getImageMask(File file);
	
	/**
	 * 
	 * @return
	 */
	public List<ComboBoxItem> getListOfOpenImages();
	
	
	
	
	
}