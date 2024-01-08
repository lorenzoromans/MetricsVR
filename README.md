# MetricsVR

Android mobile application developed for my Master's Thesis in Engineering in Computer Science at "La Sapienza" University of Rome (A.Y. 2022/2023).

## Idea
The application, installed on an Android device ( a Meta Quest 2 VR headset in my case, due to the thesis' study ) has the aim to start a background service that will retrieve and write inside a .txt file, every 1 seconds, battery data related to the device. Specifically the application was developed in order to retrieve Voltage, Current ( in mA ) and calculate the Wattage (in mW) coming from the device's battery, writing those 3 values in a text file each time with the related timestamp to which the data are related to. The application is developed with a simple user interface that allows the user to start the background service that will continue running and retrieve data until stopped by the user.


## Author
- [@lorenzoromans](https://github.com/lorenzoromans)