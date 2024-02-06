package ai.nets.samj.communication.model;

import java.util.ArrayList;

/**
 * A static list of SAM networks available in this installation.
 */
public class SAMModels extends ArrayList<SAMModel> {

	private static final long serialVersionUID = -6037502816438646853L;

	public  SAMModels() {
		super();
		add(new EfficientSAM());
		//add(new MicroSAM());
		add(new SAMViTHuge());
		//add(new SAMViTLarge());
	}
}
