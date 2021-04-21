# Domino Assistant
An Android app to assist you when playing the Mexican train variant of dominoes. It's able to detect dominoes via phone camera, count up domino pips, and compute the trains possible given your selected dominoes. Check out the demo videos embedded below to see it in action!

### How it works
The app is written primarily in Java. I utilized the Java Native Interface to pass image data (captured in real time from the phone's camera) to C++ code for processing. Within the C++ code, I used OpenCV to identify each of the dominoes in the image. C++ rather than Java was selected for the vision processing in an attempt to keep it performant, with acceptably high frame rates during the live preview of detected dominoes.

<details>
  <summary markdown="span">Click here to learn how the dominoes are detected</summary>
  
![image](https://user-images.githubusercontent.com/6509842/115492918-bee8f480-a230-11eb-84dd-2beae772c6ab.png)
![image](https://user-images.githubusercontent.com/6509842/115492958-d45e1e80-a230-11eb-8639-8a6a75c69b0f.png)
![image](https://user-images.githubusercontent.com/6509842/115492978-dd4ef000-a230-11eb-95ae-79a9a150061a.png)
![image](https://user-images.githubusercontent.com/6509842/115492996-e8098500-a230-11eb-8aef-9c88ee0e3d3d.png)
</details>

## Demo videos
Note that the demo videos were recorded on a phone manufactured in 2016; newer phones would yield higher frame rates when running the vision processing portion of the app.

### Selecting dominoes via camera
https://user-images.githubusercontent.com/6509842/115329964-1ddf3880-a161-11eb-9e0a-6bc282d025c1.mp4

### Adding to preexisting selection via camera
https://user-images.githubusercontent.com/6509842/114468931-388e3c00-9bba-11eb-95e4-2feab153d4af.mp4

### Manually adding/removing selected dominoes
https://user-images.githubusercontent.com/6509842/115329948-16b82a80-a161-11eb-8a7b-acc9deb238c9.mp4

### Generating trains based upon selection
https://user-images.githubusercontent.com/6509842/115329971-22a3ec80-a161-11eb-92e4-39d509d0c486.mp4

