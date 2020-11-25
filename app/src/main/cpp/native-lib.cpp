//#include <jni.h>
//#include <string>
//
//extern "C" JNIEXPORT jstring JNICALL
//Java_com_example_dominoassistant_MainActivity_stringFromJNI(
//        JNIEnv* env,
//        jobject /* this */) {
//    std::string hello = "Hello from C++";
//    return env->NewStringUTF(hello.c_str());
//}

#include <jni.h>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#define TAG "NativeLib"

//using namespace std;
//using namespace cv;
/*
extern "C" {
void JNICALL
Java_com_example_dominoassistant_MainActivity_adaptiveThresholdFromJNI(JNIEnv *env,
                                                                                   jobject instance,
                                                                                   jlong matAddr) {

        // get Mat from raw address
        Mat &mat = *(Mat *) matAddr;

        clock_t begin = clock();

        cv::adaptiveThreshold(mat, mat, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY_INV, 21, 5);

        // log computation time to Android Logcat
        double totalTime = double(clock() - begin) / CLOCKS_PER_SEC;
        __android_log_print(ANDROID_LOG_INFO, TAG, "adaptiveThreshold computation time = %f seconds\n",
                            totalTime);
    }
}
*/
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/features2d.hpp>
#include <iostream>
#include <iomanip>
#include <sstream>

struct Domino {
    int numberA;
    int numberB;
};

std::vector<std::vector<cv::RotatedRect>> sliceDominoes(std::vector<cv::RotatedRect> dominoes);
int processDominoHalf(cv::Mat& inputImage, cv::RotatedRect& dominoHalfRect, std::string windowName);

extern "C" {
void JNICALL
Java_com_example_dominoassistant_MainActivity_adaptiveThresholdFromJNI(JNIEnv *env,
                                                                       jobject instance,
                                                                       jlong matAddr) {
    cv::Mat &image = *(cv::Mat *) matAddr;
    // for some reason the image retrieved from camera is rotated 90deg CCW, so we have to rotate it to be normal
    cv::rotate(image, image, cv::ROTATE_90_CLOCKWISE);
    cv::Mat element5(3, 3, CV_8U, cv::Scalar(1));

    cv::Mat grayDomino;
    cv::cvtColor(image, grayDomino, cv::COLOR_BGR2GRAY);
    cv::Mat grayGauss;
    cv::GaussianBlur(grayDomino, grayGauss, cv::Size(3, 3), 1.0);

    cv::Mat closed;
    cv::morphologyEx(grayGauss, closed, cv::MORPH_CLOSE, element5);

    cv::Mat cannyImage;
    cv::Canny(closed, cannyImage, 50, 182);

    std::vector<cv::Vec4i> contourHierarchy;
    std::vector<std::vector<cv::Point>> contoursTree;
    cv::findContours(cannyImage, contoursTree, contourHierarchy, cv::RETR_TREE,
                     cv::CHAIN_APPROX_NONE);

    // Only process if there are some contours found.
    if (contoursTree.size() > 0){
        // Draw some green rotated rectangles around contours
        //std::vector<std::vector<cv::Point>> possibleDominoContours;
        std::vector<cv::RotatedRect> possibleDominoRotRects;
        // traverse first level of hierarchy
        for (int i = 0; i != -1; i = contourHierarchy[i][0]) {
            cv::RotatedRect rect = cv::minAreaRect(contoursTree[i]);
            double rectAspectRatio = rect.size.aspectRatio();

            // Filtering for potential dominoes
            if (((rectAspectRatio > 1.8 && rectAspectRatio < 2.6) || (rectAspectRatio < 0.57 && rectAspectRatio > 0.35)) && rect.size.area() > 3000) {
                // Passed preliminary filter, now check bounds of rectangle to ensure they don't exceed image
                cv::Point2f rectPoints[4];
                rect.points(rectPoints);
                int minX = std::min({rectPoints[0].x, rectPoints[1].x, rectPoints[2].x, rectPoints[3].x});
                int maxX = std::max({rectPoints[0].x, rectPoints[1].x, rectPoints[2].x, rectPoints[3].x});
                int minY = std::min({rectPoints[0].y, rectPoints[1].y, rectPoints[2].y, rectPoints[3].y});
                int maxY = std::max({rectPoints[0].y, rectPoints[1].y, rectPoints[2].y, rectPoints[3].y});
                if (minX > 0 && minY > 0 && maxX < image.cols && maxY < image.rows) {
                    // This is a possible domino, so add its contour and RotatedRect to a list
                    possibleDominoRotRects.push_back(rect);
                }
            }

        }

        // App was crashing if no potential dominoes detected,
        // so only analyze dominoes if there are any potential.
        if (possibleDominoRotRects.size() > 0){
            // Filter possible dominoes via a threshold of median area
            // First, get median
            std::vector<float> possibleDominoAreas;
            for (int i = 0; i < possibleDominoRotRects.size(); ++i) {
                possibleDominoAreas.push_back(possibleDominoRotRects[i].size.area());
            }
            std::sort(possibleDominoAreas.begin(), possibleDominoAreas.end());
            float medianArea;
            if (possibleDominoAreas.size() % 2 == 0) {
                medianArea = (possibleDominoAreas[possibleDominoAreas.size() / 2] +
                              possibleDominoAreas[(possibleDominoAreas.size() / 2) - 1]) / 2;
            } else {
                medianArea = possibleDominoAreas[possibleDominoAreas.size() / 2];
            }
            // Now, filter to within 75% of median
            std::vector<cv::RotatedRect> dominoRotRectsMedianFiltered;
            for (int i = 0; i < possibleDominoRotRects.size(); ++i) {
                if (std::abs(medianArea - possibleDominoRotRects[i].size.area()) < medianArea * 0.75) {
                    dominoRotRectsMedianFiltered.push_back(possibleDominoRotRects[i]);
                }
            }


            // Separate the suspected dominoes into halves
            //std::vector<std::vector<cv::RotatedRect>> slicedDominoes = sliceDominoes(possibleDominoRotRects);
            std::vector<std::vector<cv::RotatedRect>> slicedDominoes = sliceDominoes(
                    dominoRotRectsMedianFiltered);
            std::vector<Domino> dominoes;
            // For each domino, process each half
            for (int i = 0; i < slicedDominoes.size(); ++i) {
                Domino currentDomino;
                currentDomino.numberA = processDominoHalf(cannyImage, slicedDominoes[i][0],
                                                          "Domino " + std::to_string(i) + "A");
                currentDomino.numberB = processDominoHalf(cannyImage, slicedDominoes[i][1],
                                                          "Domino " + std::to_string(i) + "B");
                if (currentDomino.numberA > currentDomino.numberB) {
                    std::swap(currentDomino.numberA,
                              currentDomino.numberB); // ensure A is the smaller number (style choice)

                }
                dominoes.push_back(currentDomino);
            }
            std::cout << "Dominoes found: " << dominoes.size() << std::endl;

            // Output the original picture with overlaid contours and domino identifiers
            cv::Mat originalWithText = image.clone();
            for (int i = 0; i < dominoes.size(); ++i) {
                cv::Point2f rectPoints[4];
                dominoRotRectsMedianFiltered[i].points(rectPoints);
                for (int j = 0; j < 4; ++j) {
                    cv::line(image, rectPoints[j], rectPoints[(j + 1) % 4], cv::Scalar(0, 255, 0), 2);
                }
            }
            // Write text after drawing all rectangles so that text is on top.
            for (int i = 0; i < dominoes.size(); ++i) {
                std::stringstream stream;
                stream << "(" << dominoes[i].numberA << "," << dominoes[i].numberB << ")";
                std::string dominoInfo = stream.str();
                cv::putText(image, dominoInfo, dominoRotRectsMedianFiltered[i].center,
                            cv::FONT_HERSHEY_PLAIN, 2, cv::Scalar(0, 0, 0), 4, cv::LINE_AA);
                cv::putText(image, dominoInfo, dominoRotRectsMedianFiltered[i].center,
                            cv::FONT_HERSHEY_PLAIN, 2, cv::Scalar(0, 165, 255), 2);
            }
        }
    }

}
}

// Returns vector of vectors, each containing 2 RotatedRects belonging to respective halves of a domino
std::vector<std::vector<cv::RotatedRect>> sliceDominoes(std::vector<cv::RotatedRect> dominoes) {
    std::vector<std::vector<cv::RotatedRect>> results;
    // Determine rotated rectangle orientation, then divide it up into two new rotated rectangles, then do the stuff I did below (for processing a single domino) on each half
    for (int i = 0; i < dominoes.size(); ++i) {
        cv::Point2f origRectPoints[4];
        dominoes[i].points(origRectPoints);
        // rectA and rectB are the separate halves of the original rotated rectangle
        cv::RotatedRect rectA;
        cv::RotatedRect rectB;
        cv::Point2f midpoint; // midpoint of long side of original rect
        // Point order is bottomLeft, topLeft, topRight, bottomRight
        // Domino is vertical orientation, so points 0 and 3 are vertices of one (short) side, 1 and 2 are vertices of other short side
        if (dominoes[i].size.height > dominoes[i].size.width) {
            midpoint.x = (origRectPoints[0].x + origRectPoints[1].x) / 2;
            midpoint.y = (origRectPoints[0].y + origRectPoints[1].y) / 2;
            // rect A is bottom half, rect B is top half
            rectA = cv::RotatedRect(midpoint, origRectPoints[0], origRectPoints[3]);
            rectB = cv::RotatedRect(midpoint, origRectPoints[1], origRectPoints[2]);
        }
            // Domino is horizontal orientation, so points 0 and 1 are vertices of one (short) side, 2 and 3 are vertices of other short side
        else {
            midpoint.x = (origRectPoints[0].x + origRectPoints[3].x) / 2;
            midpoint.y = (origRectPoints[0].y + origRectPoints[3].y) / 2;
            // rect A is left side, rect B is right side
            rectA = cv::RotatedRect(midpoint, origRectPoints[0], origRectPoints[1]);
            rectB = cv::RotatedRect(midpoint, origRectPoints[3], origRectPoints[2]);
        }
        std::vector<cv::RotatedRect> dominoHalves;
        dominoHalves.push_back(rectA);
        dominoHalves.push_back(rectB);
        results.push_back(dominoHalves);
    }

    return results;
}

/* Determines the number of pips in half a domino.
 * params: inputImage: binary image (of the entire scene) where features are in white and everything else is black
 *		   dominoHalfRect: the RotatedRect of where we should be looking in the image of the full scene
 *		   windowName: just for testing right now, name of window to be opened showing result
 * returns: int indicating the number of pips detected on the input domino half
*/
int
processDominoHalf(cv::Mat &inputImage, cv::RotatedRect &dominoHalfRect, std::string windowName) {
    // Process contents of a single domino
    cv::Point2f rectPoints[4];
    dominoHalfRect.points(rectPoints);
    int minX = std::min({rectPoints[0].x, rectPoints[1].x, rectPoints[2].x, rectPoints[3].x});
    int maxX = std::max({rectPoints[0].x, rectPoints[1].x, rectPoints[2].x, rectPoints[3].x});
    int minY = std::min({rectPoints[0].y, rectPoints[1].y, rectPoints[2].y, rectPoints[3].y});
    int maxY = std::max({rectPoints[0].y, rectPoints[1].y, rectPoints[2].y, rectPoints[3].y});
    /*
    // Some RotatedRects have boundaries outside the image
    if (minX < 0 || minY < 0 || maxX > inputImage.cols || maxY > inputImage.rows){
        return 0;
    }
     */
    int boundingWidth = maxX - minX;
    int boundingHeight = maxY - minY;

    cv::Mat dominoMask(boundingHeight, boundingWidth, CV_8U, cv::Scalar(0));
    cv::Point polyVertices[4];
    for (int i = 0; i < 4; ++i) {
        polyVertices[i].x = rectPoints[i].x - minX;
        polyVertices[i].y = rectPoints[i].y - minY;
    }
    cv::fillConvexPoly(dominoMask, polyVertices, 4, cv::Scalar(255));
    // Mask finished, time to get image to process
    cv::Mat dominoROI(inputImage, cv::Range(minY, maxY), cv::Range(minX, maxX));
    cv::Mat modifiableDomino = dominoROI.clone();
    cv::bitwise_and(modifiableDomino, dominoMask, modifiableDomino);

    // Detect the dots (circles) via SimpleBlobDetector
    // Blobs filtered by parameters, stored in Keypoints vector
    cv::SimpleBlobDetector::Params params;
    params.filterByCircularity = true;
    params.minCircularity = 0.5;
    params.filterByInertia = false;
    params.filterByArea = true;
    params.minArea = 40;
    params.maxArea = 2000;
    params.filterByColor = true;
    params.blobColor = 0;
    params.filterByConvexity = false;
    cv::Ptr<cv::SimpleBlobDetector> detector = cv::SimpleBlobDetector::create(params);
    std::vector<cv::KeyPoint> keypoints;
    detector->detect(modifiableDomino, keypoints);
    int numberPips = keypoints.size();

    return numberPips;
}
