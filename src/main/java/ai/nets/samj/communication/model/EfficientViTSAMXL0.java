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
package ai.nets.samj.communication.model;

import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;

import java.awt.Polygon;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ai.nets.samj.AbstractSamJ;
import ai.nets.samj.EfficientViTSamJ;
import ai.nets.samj.SamEnvManager;
import ai.nets.samj.ui.SAMJLogger;

public class EfficientViTSAMXL0 implements SAMModel {

	private static final Polygon EMPTY_POLYGON = new Polygon(new int[0], new int[0], 0);

	private EfficientViTSamJ efficientSamJ;
	private SAMJLogger log;
	private Boolean installed = false;
	public static final String FULL_NAME = "EfficientViTSAM-xl0";
	public static final String INPUT_IMAGE_AXES = "xyc";
	
	public EfficientViTSAMXL0() {
		
	}

	@Override
	public String getName() {
		return FULL_NAME;
	}

	@Override
	public String getDescription() {
		return "Bla bla Efficient SAM";
	}

	@Override
	public boolean isInstalled() {
		return installed;
	}

	@Override
	public SAMModel instantiate(final RandomAccessibleInterval<?> image, final SAMJLogger useThisLoggerForIt) {
		try {
			return new EfficientViTSAMXL0(image,useThisLoggerForIt);
		} catch (IOException | InterruptedException | RuntimeException e) {
			useThisLoggerForIt.error(FULL_NAME + " experienced an error: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void setInstalled(boolean installed) {
		this.installed = installed;		
	}

	public EfficientViTSAMXL0(final RandomAccessibleInterval<?> image,
	                              final SAMJLogger log)
	throws IOException, RuntimeException, InterruptedException {
		this.log = log;
		AbstractSamJ.DebugTextPrinter filteringLogger = text -> {
			int idx = text.indexOf("contours_x");
			if (idx > 0) this.log.info( text.substring(0,idx) );
			else this.log.info( text );
		};
		efficientSamJ = EfficientViTSamJ.initializeSam("xl0",
				SamEnvManager.create(), Cast.unchecked(image),
				filteringLogger, false);
	}

	@Override
	public List<Polygon> fetch2dSegmentation(List<Localizable> listOfPoints2D, List<Localizable> listOfNegPoints2D) {
		try {
			List<int[]> list = listOfPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]}).collect(Collectors.toList());
			List<int[]> negList = listOfNegPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]}).collect(Collectors.toList());
			if (negList.size() == 0) return efficientSamJ.processPoints(list);
			else return efficientSamJ.processPoints(list, negList);
		} catch (IOException | RuntimeException | InterruptedException e) {
			log.error(FULL_NAME+", providing empty result because of some trouble: "+e.getMessage());
			e.printStackTrace();
			List<Polygon> retList = new ArrayList<>(1);
			retList.add( EMPTY_POLYGON );
			return retList;
		}
	}

	@Override
	public List<Polygon> fetch2dSegmentation(Localizable lineStartPoint2D, Localizable lineEndPoint2D) {
		log.info(FULL_NAME+": NOT SUPPORTED YET");
		List<Polygon> retList = new ArrayList<>(1);
		retList.add( EMPTY_POLYGON );
		return retList;
	}

	@Override
	public List<Polygon> fetch2dSegmentation(Interval boundingBox2D) {
		try {
			//order to processBox() should be: x0,y0, x1,y1
			final int bbox[] = {
				(int)boundingBox2D.min(0),
				(int)boundingBox2D.min(1),
				(int)boundingBox2D.max(0),
				(int)boundingBox2D.max(1)
			};
			return efficientSamJ.processBox(bbox);
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(FULL_NAME+", providing empty result because of some trouble: "+e.getMessage());
			e.printStackTrace();
			List<Polygon> retList = new ArrayList<>(1);
			retList.add( EMPTY_POLYGON );
			return retList;
		}
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> List<Polygon> fetch2dSegmentationFromMask(RandomAccessibleInterval<T> rai) {
		try {
			return efficientSamJ.processMask(rai);
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(FULL_NAME+", providing empty result because of some trouble: "+e.getMessage());
			e.printStackTrace();
			List<Polygon> retList = new ArrayList<>(1);
			retList.add( EMPTY_POLYGON );
			return retList;
		}
	}

	@Override
	public void notifyUiHasBeenClosed() {
		log.info(FULL_NAME+": OKAY, I'm closing myself...");
		closeProcess();
	}

	@Override
	public void closeProcess() {
		efficientSamJ.close();
	}

	@Override
	public String getInputImageAxes() {
		return INPUT_IMAGE_AXES;
	}
}