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

import net.imglib2.RandomAccessibleInterval;

import java.util.List;

import ai.nets.samj.communication.model.SAMModel;

import java.awt.Polygon;
import java.io.File;

public interface PromptsResultsDisplay {

	/**
	 * Give the display an image on which it shall operate. It is, however,
	 * not expected that the display will operate on the full image. To
	 * get the potential portion of the image on which the display eventually
	 * operates, use {@link PromptsResultsDisplay#giveProcessedSubImage()}.
	 */
	void switchToThisImg(final RandomAccessibleInterval<?> newImage);

	/**
	 * Returns the actual image on which the display operates, which may
	 * very well be only a portion of the originaly provided image, see
	 * {@link PromptsResultsDisplay#switchToThisImg(RandomAccessibleInterval)}.
	 */
	RandomAccessibleInterval<?> giveProcessedSubImage(SAMModel selectedModel);

	void switchToThisNet(final SAMModel promptsToNetAdapter);
	void notifyNetToClose();

	List<Polygon> getPolygonsFromRoiManager();
	void improveExistingMask(File mask);

	void enableAddingToRoiManager(boolean shouldBeAdding);
	boolean isAddingToRoiManager();

	void switchToUsingRectangles();
	void switchToUsingLines();
	void switchToUsingPoints();
	void switchToNone();
	
	Object getFocusedImage();
}