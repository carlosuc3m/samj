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
package ai.nets.samj.ui;

import java.util.List;

import ai.nets.samj.gui.components.ComboBoxItem;

/**
 * Interface to be implemented by the imaging software that wants to use the default SAMJ UI.
 * Provides a list of the images open to the SAMJ GUI
 * @author Carlos Garcia
 */
public interface UtilityMethods {

	/**
	 * Method to be implemented in the softwar that wants to use the SAMJ default GUI.
	 * This method should return a list of {@link ComboBoxItem} where each instance contains
	 * a reference to an image object in the consumer software (ImagePlus in ImageJ or 
	 * Sequence in Icy) with an unique identifier
	 * 
	 * @return a list of the open images in the consumer software
	 */
	public List<ComboBoxItem> getListOfOpenImages(); 
}
