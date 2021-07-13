# Domino Assistant

<img align="right" src="https://user-images.githubusercontent.com/6509842/115637347-f3ff5080-a2dd-11eb-97cf-b1478fa0efef.png">

An Android app to assist you when playing the Mexican train variant of dominoes. It's able to detect dominoes via phone camera, count up domino pips, and compute the trains possible given your selected dominoes. Check out the demo videos embedded below to see it in action!

### How it works
The app is written primarily in Java. I utilized the Java Native Interface to pass image data (captured in real time from the phone's camera) to C++ code for processing. Within the C++ code, I used OpenCV to identify each of the dominoes in the image. I went through the JNI to do the vision processing in C++ with the presumption that it'd be a little bit more performant than doing it in Java, in turn increasing the frame rate of the live preview.

<details>
  <summary markdown="span"><b><i>Click here to learn how the dominoes are detected</i></b></summary>
  
Prior to creating this app I did a bit of market research on publicly available domino assistant apps, and I was disappointed in what I found. The apps were basic, simply tallying the number of perceived circles (at a very low frame rate) and not actually identifying dominoes. They were slow and prone to miscounting. This app aimed to isolate and identify each domino at an acceptably fast speed.
  
![image](https://user-images.githubusercontent.com/6509842/115492918-bee8f480-a230-11eb-84dd-2beae772c6ab.png)
The processing begins by converting the received image to grayscale, reducing the color channels from 3 to 1, thereby reducing computational complexity. Next, a Gaussian blur is applied, reducing noise in the image. After that, the image is slightly dilated then eroded (it's "closed"). Closing the image reduces light glare within the pips since the glare gets overwhelmed upon dilation of the pip coloration. Once the image has been closed, the Canny edge detection algorithm is run, providing a vector of contours it found.

![image](https://user-images.githubusercontent.com/6509842/115492958-d45e1e80-a230-11eb-8639-8a6a75c69b0f.png)
After acquiring the vector of contours, the bounding rectangle of each contour is found. These bounding rectangles are filtered to eliminate obvious non-domino contours. Filter conditions include surpassing a minimum area, having an aspect ratio near 2:1, having bounds within the image frame, having an area similar to that of the median area of the possible dominoes, and containing a bisecting line.  
  
![image](https://user-images.githubusercontent.com/6509842/115492978-dd4ef000-a230-11eb-95ae-79a9a150061a.png)
Bisecting lines are critical landmarks that every domino contains, so it makes sense to check for that when identifying a contour as a possible domino. The first step of checking for a bisecting line is to isolate the domino, which is done by utilizing coordinates derived from the contour's bounding rectangle. Then, a rectangular mask is created over the center quarter of the domino. Pixels inside the masked area are checked for contours, then those contours are filtered based on characteristics indicative of a domino's bisecting line, such as aspect ratio, area, and circularity. If a bisecting line is not detected, the possible domino is discarded and the program checks the next one.
  
![image](https://user-images.githubusercontent.com/6509842/115492996-e8098500-a230-11eb-8aef-9c88ee0e3d3d.png)
If a possible domino has not been filtered out by this point, then it's assumed to be an actual domino and needs to have its pips counted. The process of counting pips is similar to how the bisecting line is detected: an area is isolated (half a domino), a mask is applied, then contours are filtered based on signature characteristics of the pips (high circularity, an aspect ratio near 1:1, and greater than some minimum area).
</details>

## Demo videos
Note that the demo videos were recorded on a phone manufactured in 2016; newer phones would yield higher frame rates when running the vision processing portion of the app. Additionally, <b>the demo videos look best when maximized/fullscreen.</b>

### Selecting dominoes via camera
https://user-images.githubusercontent.com/6509842/115329964-1ddf3880-a161-11eb-9e0a-6bc282d025c1.mp4

Here dominoes are automatically detected and added to the user's selection via the camera.

### Adding to preexisting selection via camera
https://user-images.githubusercontent.com/6509842/114468931-388e3c00-9bba-11eb-95e4-2feab153d4af.mp4

Here the user adds dominoes to their preexisting selection via the camera. Note that pre-selected dominoes are not duplicated by being detected by the camera again.

### Manually adding/removing selected dominoes
https://user-images.githubusercontent.com/6509842/115329948-16b82a80-a161-11eb-8a7b-acc9deb238c9.mp4

Here the user manually adds and removes dominoes from their selection.

### Generating trains based upon selection
https://user-images.githubusercontent.com/6509842/115329971-22a3ec80-a161-11eb-92e4-39d509d0c486.mp4

Here the user, with dominoes already selected, tells the app which pip-count their train must start with. Then, the app generates the possible trains that may be constructed given the selected dominoes, and it presents the top 3 trains to the user. After looking at the trains, the user returns to the domino selection screen and presses the "clear selection" button (trashcan icon). 

Train generation was basically a tree-building problem, where normal dominoes may each have 1 child and "doubles" (dominoes such as 0-0, 1-1, 2-2, etc.) are each able to have 3 children as per the rules of Mexican train. Trains are sorted by total length, then number of doubles, then by the total number of pips in the train. This sorting order yields an effective game strategy.

Displaying the trains was a tricky task. Dynamically showing the trains took a little finagling to figure out, since I had to accomodate different train lengths and widths. I decided to use scrollable views so the user may scroll around to see an entire train. The physical size of the dominoes within a train changes for every 10 dominoes within the train (as seen in the demo video). This somewhat helps in keeping most of the train in the default, non-scrolled view.
