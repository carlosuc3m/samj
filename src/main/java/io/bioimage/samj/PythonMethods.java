package io.bioimage.samj;

/**
 * Class that declares the Python methods that can be used then in the script for several tasks
 * such as getting the edges from the segmentation mask and many others
 * 
 * @author Carlos Garcia Lopez de Haro
 */
public class PythonMethods {

	protected static String TRACE_EDGES = ""
			+ "def is_edge_pixel(image, cx,cy):\n"
			+ "    # assuming image[cy,cx] != 0\n"
			+ "    h,w = image.shape\n"
			+ "    if cx < 0 or cx >= w or cy < 0 or cy >= h:\n"
			+ "        return False\n"
			+ "    # NB: cx,cy are valid coords\n"
			+ "    return cy == 0 or image[cy-1,cx] == 0 or cx == 0 or image[cy,cx-1] == 0 or cx == w-1 or image[cy,cx+1] == 0 or cy == h-1 or image[cy+1,cx] == 0\n"
			+ "\n"
			+ "def find_contour_neighbors(image, cx,cy, last_forward_dir):\n"
			+ "    ccw_dir = [8,9,6,3,2,1,4,7] # \"numpad directions\"\n"
			+ "    # so, directions are coded, and numbers 1-4,6-9 are used,\n"
			+ "    # let's use them as indices to the arrays below:\n"
			+ "    dir_idx = [0, 5,4,3, 6,0,2, 7,0,1]             # where is the code in the ccw_dir\n"
			+ "    counter_shifted_dir = [0, 6,9,8, 3,0,7, 2,1,4] # opposite code plus one in ccw\n"
			+ "    dir_dx = [0, -1,0,+1, -1,0,+1, -1,0,1]         # code translated to shift in x-axis\n"
			+ "    dir_dy = [0, +1,+1,+1, 0,0,0,  -1,-1,-1]       # code translated to shift in y-axis\n"
			+ "    #\n"
			+ "    # find first bro in the ccw direction starting from the direction\n"
			+ "    # from which we have come to this position\n"
			+ "    test_dir = counter_shifted_dir[last_forward_dir]\n"
			+ "    #print(f\"starting pos [{cx},{cy}], forward dir code {last_forward_dir}, thus examine code {test_dir}\")\n"
			+ "    nx = cx+dir_dx[test_dir]\n"
			+ "    ny = cy+dir_dy[test_dir]\n"
			+ "    while not (is_edge_pixel(image,nx,ny) and image[ny,nx] != 0):\n"
			+ "        #print(f\"  not contour at [{nx},{ny}], examined dir code {test_dir}\")\n"
			+ "        test_dir = ccw_dir[ (dir_idx[test_dir]+1)%8 ]\n"
			+ "        nx = cx+dir_dx[test_dir]\n"
			+ "        ny = cy+dir_dy[test_dir]\n"
			+ "    #print(f\"  happy at [{nx},{ny}], examined dir code {test_dir}\")\n"
			+ "    return nx,ny,test_dir\n"
			+ "\n"
			+ "def trace_contour(image, max_iters, offset_x = 0, offset_y = 0):\n"
			+ "    sy = 0\n"
			+ "    sx = np.where(image[0] != 0)[0][0]\n"
			+ "    last_forward_dir = 1\n"
			+ "    #\n"
			+ "    x_coords = [int(sx+offset_x)]\n"
			+ "    y_coords = [int(sy+offset_y)]\n"
			+ "    x,y,last_forward_dir = find_contour_neighbors(image, sx,sy,last_forward_dir)\n"
			+ "    cnt = 1\n"
			+ "    while not (x == sx and y == sy) and cnt < max_iters:\n"
			+ "        x_coords.append(int(x+offset_x))\n"
			+ "        y_coords.append(int(y+offset_y))\n"
			+ "        x,y,last_forward_dir = find_contour_neighbors(image, x,y,last_forward_dir)\n"
			+ "        cnt += 1\n"
			+ "    #\n"
			+ "    return x_coords,y_coords\n"
			+ "\n"
			+ "def get_polygons_from_binary_mask(sam_result, at_least_of_this_size = 3):\n"
			+ "    labels = measure.regionprops( measure.label(sam_result,connectivity=1) )\n"
			+ "    x_contours = []\n"
			+ "    y_contours = []\n"
			+ "    for obj in labels:\n"
			+ "        if obj.num_pixels >= at_least_of_this_size:\n"
			+ "            x_coords,y_coords = trace_contour(obj.image, obj.num_pixels, obj.bbox[1],obj.bbox[0])\n"
			+ "            x_contours.append(x_coords)\n"
			+ "            y_contours.append(y_coords)\n"
			+ "    return x_contours,y_contours" + System.lineSeparator()
			+ "globals()['is_edge_pixel'] = is_edge_pixel" + System.lineSeparator()
			+ "globals()['find_contour_neighbors'] = find_contour_neighbors" +  System.lineSeparator()
			+ "globals()['trace_contour'] = trace_contour" +  System.lineSeparator()
			+ "globals()['get_polygons_from_binary_mask'] = get_polygons_from_binary_mask" +  System.lineSeparator();
}
