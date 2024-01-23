package io.bioimage.samj;

/**
 * Class that declares the Python methods that can be used then in the script for several tasks
 * such as getting the edges from the segmentation mask and many others
 * 
 * @author Carlos Garcia Lopez de Haro
 */
public class PythonMethods {

	protected static String TRACE_EDGES = ""
			+ "def inside(mask, x, y, direction):\n"
			+ "    # Check if the pixel in a given direction is inside the mask\n"
			+ "    if direction == 0: return x + 1 < mask.shape[1] and mask[y, x + 1]\n"
			+ "    if direction == 1: return y - 1 >= 0 and mask[y - 1, x]\n"
			+ "    if direction == 2: return x - 1 >= 0 and mask[y, x - 1]\n"
			+ "    if direction == 3: return y + 1 < mask.shape[0] and mask[y + 1, x]\n"
			+ "    return False\n"
			+ "\n"
			+ "def add_point(points, x, y):\n"
			+ "    # Add a point to the contour\n"
			+ "    points.append((x, y))\n"
			+ "\n"
			+ "def trace_edge(mask, start_x, start_y, four_connected=True):\n"
			+ "    start_direction = 3\n"
			+ "    start_y += 1\n"
			+ "\n"
			+ "    x, y = start_x, start_y\n"
			+ "    direction = start_direction\n"
			+ "    points = []\n"
			+ "\n"
			+ "    while True:\n"
			+ "        new_direction = None\n"
			+ "        if four_connected:\n"
			+ "            for i in range((direction - 1), (direction + 2) + 1):\n"
			+ "                if (inside(mask, x, y, i % 4)):\n"
			+ "                    new_direction = i % 4\n"
			+ "                    break\n"
			+ "        else:\n"
			+ "            new_direction = direction + 1\n"
			+ "            while new_direction >= direction:\n"
			+ "                if inside(mask, x, y, new_direction):\n"
			+ "                    break\n"
			+ "                new_direction -= 1\n"
			+ "        if new_direction is None:\n"
			+ "            break\n"
			+ "        if new_direction != direction:\n"
			+ "            add_point(points, x, y)\n"
			+ "\n"
			+ "        direction = new_direction\n"
			+ "        if direction % 4 == 0: x += 1\n"
			+ "        elif direction % 4 == 1: y -= 1\n"
			+ "        elif direction % 4 == 2: x -= 1\n"
			+ "        elif direction % 4 == 3: y += 1\n"
			+ "\n"
			+ "        if (x, y) == (start_x, start_y):\n"
			+ "            if not points or points[0] != (x, y):\n"
			+ "                add_point(points, x, y)\n"
			+ "            break\n"
			+ "    return points, direction <= 0\n"
			+ "\n"
			+ "def get_polygons_from_binary_mask(sam_result, at_least_of_this_size = 3):\n"
			+ "    labels = measure.regionprops( measure.label(sam_result,connectivity=1) )\n"
			+ "    x_contours = []\n"
			+ "    y_contours = []\n"
			+ "    for obj in labels:\n"
			+ "        if obj.num_pixels >= at_least_of_this_size:\n"
			+ "            non_zero = np.where(obj.image != 0)\n"
			+ "            local_contours, _ = trace_edge(obj.image, non_zero[1][0], non_zero[0][0])\n"
			+ "            img_offset_y = obj.bbox[0]\n"
			+ "            img_offset_x = obj.bbox[1]\n"
			+ "            global_x_contours = [ int(coord[0]+img_offset_x) for coord in local_contours ]\n"
			+ "            global_y_contours = [ int(coord[1]+img_offset_y) for coord in local_contours ]\n"
			+ "            x_contours.append(global_x_contours)\n"
			+ "            y_contours.append(global_y_contours)\n"
			+ "    return x_contours,y_contours" + System.lineSeparator()
			+ "globals()['inside'] = inside" + System.lineSeparator()
			+ "globals()['add_point'] = add_point" +  System.lineSeparator()
			+ "globals()['trace_edge'] = trace_edge" +  System.lineSeparator()
			+ "globals()['get_polygons_from_binary_mask'] = get_polygons_from_binary_mask" +  System.lineSeparator();


	protected static final String  FIND_CONTOURS = ""
			+"def find_contours(mask):" + System.lineSeparator()
			+ "    # Ensure mask is binary" + System.lineSeparator()
			+ "    mask = np.where(mask > 0, 1, 0)" + System.lineSeparator()
			+ "    # Find edges: shift the mask in each direction and find where it differs from the original" + System.lineSeparator()
			+ "    edges = np.zeros_like(mask)" + System.lineSeparator()
			+ "    edges[:-1, :] |= (mask[1:, :] != mask[:-1, :])  # vertical edges" + System.lineSeparator()
			+ "    edges[:, :-1] |= (mask[:, 1:] != mask[:, :-1])  # horizontal edges" + System.lineSeparator()
			+ "    return edges" + System.lineSeparator()
			+ "globals()['find_contours'] = find_contours" + System.lineSeparator();

}
