package io.bioimage.samj;

/**
 * Class that declares the Python methods that can be used then in the script for several tasks
 * such as getting the edges from the segmentation mask and many others
 * 
 * @author Carlos Garcia Lopez de Haro
 */
public class PythonMethods {
	
	protected static String TRACE_EDGES = ""
			+ "def inside(mask, x, y, direction):" + System.lineSeparator()
			+ "    # Check if the pixel in a given direction is inside the mask" + System.lineSeparator()
			+ "    if direction == 0: return x + 1 < mask.shape[1] and mask[y, x + 1]" + System.lineSeparator()
			+ "    if direction == 1: return y - 1 >= 0 and mask[y - 1, x]" + System.lineSeparator()
			+ "    if direction == 2: return x - 1 >= 0 and mask[y, x - 1]" + System.lineSeparator()
			+ "    if direction == 3: return y + 1 < mask.shape[0] and mask[y + 1, x]" + System.lineSeparator()
			+ "    return False\n" + System.lineSeparator()
			+ System.lineSeparator()
			+ "def add_point(points, x, y):" + System.lineSeparator()
			+ "    # Add a point to the contour" + System.lineSeparator()
			+ "    points.append((x, y))" + System.lineSeparator()
			+ System.lineSeparator()
			+ "def trace_edge(mask, start_x, start_y, four_connected=False):" + System.lineSeparator()
			+ "    if inside(mask, start_x, start_y, 1):" + System.lineSeparator()
			+ "        start_direction = 1  # Starting up" + System.lineSeparator()
			+ "    else:\n" + System.lineSeparator()
			+ "        start_direction = 3  # Starting down" + System.lineSeparator()
			+ "        start_y += 1\n" + System.lineSeparator()
			+ System.lineSeparator()
			+ "    x, y = start_x, start_y" + System.lineSeparator()
			+ "    direction = start_direction" + System.lineSeparator()
			+ "    points = []" + System.lineSeparator()
			+ System.lineSeparator()
			+ "    while True:" + System.lineSeparator()
			+ "        if four_connected:" + System.lineSeparator()
			+ "            new_direction = direction" + System.lineSeparator()
			+ "            while new_direction < direction + 2:" + System.lineSeparator()
			+ "                if not inside(mask, x, y, new_direction):" + System.lineSeparator()
			+ "                    break" + System.lineSeparator()
			+ "                new_direction += 1" + System.lineSeparator()
			+ "            new_direction -= 1" + System.lineSeparator()
			+ "        else:\n" + System.lineSeparator()
			+ "            new_direction = direction + 1" + System.lineSeparator()
			+ "            while new_direction >= direction:" + System.lineSeparator()
			+ "                if inside(mask, x, y, new_direction):" + System.lineSeparator()
			+ "                    break" + System.lineSeparator()
			+ "                new_direction -= 1" + System.lineSeparator()
			+ System.lineSeparator()
			+ "        if new_direction != direction:" + System.lineSeparator()
			+ "            add_point(points, x, y)" + System.lineSeparator()
			+ System.lineSeparator()
			+ "        direction = new_direction" + System.lineSeparator()
			+ "        if direction % 4 == 0: x += 1" + System.lineSeparator()
			+ "        elif direction % 4 == 1: y -= 1" + System.lineSeparator()
			+ "        elif direction % 4 == 2: x -= 1" + System.lineSeparator()
			+ "        elif direction % 4 == 3: y += 1" + System.lineSeparator()
			+ System.lineSeparator()
			+ "        if (x, y) == (start_x, start_y) and direction % 4 == start_direction % 4:" + System.lineSeparator()
			+ "            if not points or points[0] != (x, y):" + System.lineSeparator()
			+ "                add_point(points, x, y)" + System.lineSeparator()
			+ "            break" + System.lineSeparator()
			+ System.lineSeparator()
			+ "    return points, direction <= 0" + System.lineSeparator()
			+ "globals()['inside'] = inside" + System.lineSeparator()
			+ "globals()['add_point'] = add_point" +  System.lineSeparator()
			+ "globals()['trace_edge'] = trace_edge" +  System.lineSeparator();

}
