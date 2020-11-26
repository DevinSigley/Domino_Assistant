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
#include <set>

struct Domino {
    int numberA;
    int numberB;
};

std::vector<std::vector<cv::RotatedRect>> sliceDominoes(std::vector<cv::RotatedRect> dominoes);
int processDominoHalf(cv::Mat& inputImage, cv::RotatedRect& dominoHalfRect);
bool checkForBisectLine(cv::Mat& inputImage, cv::RotatedRect& dominoRect);

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
        std::vector<int> dominoContourIndicesPreMedian; // used for drawing convex hulls of domino contours on final image
        // traverse all contours
        for (int i = 0; i < contoursTree.size(); ++i) {
            cv::RotatedRect rect = cv::minAreaRect(contoursTree[i]);
            double rectAspectRatio = rect.size.aspectRatio();

            // Filtering for potential dominoes
            if ((abs(rectAspectRatio - 2) < 0.7 || abs(rectAspectRatio - 0.5) < 0.2) && rect.size.area() > 3000) {
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
                    dominoContourIndicesPreMedian.push_back(i); // used for drawing convex hulls of domino contours on final image
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
            std::vector<int> dominoContourIndicesPrePoint;
            for (int i = 0; i < possibleDominoRotRects.size(); ++i) {
                if (std::abs(medianArea - possibleDominoRotRects[i].size.area()) < medianArea * 0.75) {
                    dominoRotRectsMedianFiltered.push_back(possibleDominoRotRects[i]);
                    dominoContourIndicesPrePoint.push_back(dominoContourIndicesPreMedian[i]);
                }
            }

            // Now, filter out RotRects with nearly the same bottom-left point, since some contours are almost duplicated
            std::vector<cv::RotatedRect> dominoRotRectsPointFiltered;
            std::set<int> bottomLeftDominoRects;
            std::vector<int> dominoContourIndicesPointFiltered;
            for (int i = 0; i < dominoRotRectsMedianFiltered.size(); ++i) {
                cv::Point2f rectPoints[4];
                dominoRotRectsMedianFiltered[i].points(rectPoints);
                int xPointInt = ((int)rectPoints[0].x) / 15 * 15;
                int yPointInt = ((int)rectPoints[0].y) / 15 * 15;
                int setKey = (xPointInt * 100000) + yPointInt;
                if (bottomLeftDominoRects.find(setKey) != bottomLeftDominoRects.end()) {
                    // Already a contour with a similar bottom-left point, so don't add it to our list
                }
                else {
                    bottomLeftDominoRects.insert(setKey);
                    dominoRotRectsPointFiltered.push_back(dominoRotRectsMedianFiltered[i]);
                    dominoContourIndicesPointFiltered.push_back(dominoContourIndicesPrePoint[i]);
                }
            }

            // Filter by whether or not the "domino" has a bisecting line present
            std::vector<cv::RotatedRect> dominoRotRectsFinalFiltered;
            std::vector<int> dominoContourIndicesFinal;
            for (int i = 0; i < dominoRotRectsPointFiltered.size(); ++i) {
                if (checkForBisectLine(cannyImage, dominoRotRectsPointFiltered[i])) {
                    dominoRotRectsFinalFiltered.push_back(dominoRotRectsPointFiltered[i]);
                    dominoContourIndicesFinal.push_back(dominoContourIndicesPointFiltered[i]);
                }
            }

            // Separate the suspected dominoes into halves
            //std::vector<std::vector<cv::RotatedRect>> slicedDominoes = sliceDominoes(possibleDominoRotRects);
            std::vector<std::vector<cv::RotatedRect>> slicedDominoes = sliceDominoes(dominoRotRectsFinalFiltered);
            std::vector<Domino> dominoes;
            // For each domino, process each half
            for (int i = 0; i < slicedDominoes.size(); ++i) {
                Domino currentDomino;
                currentDomino.numberA = processDominoHalf(cannyImage, slicedDominoes[i][0]);
                currentDomino.numberB = processDominoHalf(cannyImage, slicedDominoes[i][1]);
                if (currentDomino.numberA > currentDomino.numberB) {
                    std::swap(currentDomino.numberA,
                              currentDomino.numberB); // ensure A is the smaller number (style choice)

                }
                dominoes.push_back(currentDomino);
            }

            // Output the original picture with overlaid contours and domino identifiers
            //cv::Mat originalWithText = image.clone();
            for (int i = 0; i < dominoes.size(); ++i) {
                //std::vector<std::vector<cv::Point>> convexHull(1);
                //cv::convexHull(contoursTree[dominoContourIndicesFinal[i]], convexHull[0]);
                //cv::drawContours(image, convexHull, 0, cv::Scalar(0, 255, 0), 2);

                // draw rotated rectangles around the dominoes
                 cv::Point2f rectPoints[4];
                dominoRotRectsFinalFiltered[i].points(rectPoints);
                for (int j = 0; j < 4; ++j) {
                    cv::line(image, rectPoints[j], rectPoints[(j + 1) % 4], cv::Scalar(0, 255, 0), 2);
                }
            }
            // Write text after drawing all rectangles so that text is on top.
            for (int i = 0; i < dominoes.size(); ++i) {
                std::stringstream stream;
                stream << "(" << dominoes[i].numberA << "," << dominoes[i].numberB << ")";
                std::string dominoInfo = stream.str();
                cv::putText(image, dominoInfo, dominoRotRectsFinalFiltered[i].center,
                            cv::FONT_HERSHEY_PLAIN, 2, cv::Scalar(0, 0, 0), 4, cv::LINE_AA);
                cv::putText(image, dominoInfo, dominoRotRectsFinalFiltered[i].center,
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
int processDominoHalf(cv::Mat &inputImage, cv::RotatedRect &dominoHalfRect) {
    // Process contents of a single domino
    cv::Point2f rectPoints[4];
    dominoHalfRect.points(rectPoints);
    int minX = std::min({ rectPoints[0].x, rectPoints[1].x, rectPoints[2].x, rectPoints[3].x });
    int maxX = std::max({ rectPoints[0].x, rectPoints[1].x, rectPoints[2].x, rectPoints[3].x });
    int minY = std::min({ rectPoints[0].y, rectPoints[1].y, rectPoints[2].y, rectPoints[3].y });
    int maxY = std::max({ rectPoints[0].y, rectPoints[1].y, rectPoints[2].y, rectPoints[3].y });
    int boundingWidth = maxX - minX;
    int boundingHeight = maxY - minY;

    cv::Mat dominoROI(inputImage, cv::Range(minY, maxY), cv::Range(minX, maxX));
    cv::Mat modifiableDomino = dominoROI.clone();

    cv::Mat dominoMask(modifiableDomino.rows, modifiableDomino.cols, CV_8U, cv::Scalar(0));
    cv::Point polyVertices[4];
    // mask a little bit larger than the our specific RotRect
    for (int i = 0; i < 4; ++i) {
        if (i == 0) { // bottom-left point
            polyVertices[i].x = (rectPoints[i].x - minX) * 0.90;
            polyVertices[i].y = (rectPoints[i].y - minY) * 1.10;
        }
        else if (i == 1) { // top-left point
            polyVertices[i].x = (rectPoints[i].x - minX) * 0.90;
            polyVertices[i].y = (rectPoints[i].y - minY) * 0.90;
        }
        else if (i == 2) { // top-right point
            polyVertices[i].x = (rectPoints[i].x - minX) * 1.10;
            polyVertices[i].y = (rectPoints[i].y - minY) * 0.90;
        }
        else if (i == 3) { // bottom-right point
            polyVertices[i].x = (rectPoints[i].x - minX) * 1.10;
            polyVertices[i].y = (rectPoints[i].y - minY) * 1.10;
        }

        //polyVertices[i].x = rectPoints[i].x - minX;
        //polyVertices[i].y = rectPoints[i].y - minY;
    }
    cv::fillConvexPoly(dominoMask, polyVertices, 4, cv::Scalar(255));

    cv::bitwise_and(modifiableDomino, dominoMask, modifiableDomino);
    //cv::bitwise_not(modifiableDomino, modifiableDomino); // inverts the colors

    // try to outline the convex hulls of contours?
    cv::Mat hullsImage(modifiableDomino.rows, modifiableDomino.cols, CV_8U, cv::Scalar(0));
    std::vector<std::vector<cv::Point>> contoursTree;
    cv::findContours(modifiableDomino, contoursTree, cv::RETR_TREE, cv::CHAIN_APPROX_NONE);
    std::vector<std::vector<cv::Point>> contoursTreeFiltered;
    for (int i = 0; i < contoursTree.size(); ++i) {
        cv::RotatedRect rect = cv::minAreaRect(contoursTree[i]);
        if (abs(rect.size.aspectRatio() - 1) < 0.5) {
            if (rect.size.area() > 80 && rect.size.area() < modifiableDomino.rows * modifiableDomino.cols / 4) {
                contoursTreeFiltered.push_back(contoursTree[i]);
            }

        }
        /*double area = cv::contourArea(contoursTree[i]);
        double areaRatio = area / (boundingHeight * boundingWidth);
        if (areaRatio > .006 && areaRatio < .012) {
            contoursTreeFiltered.push_back(contoursTree[i]);
        }*/
    }
    for (int i = 0; i < contoursTreeFiltered.size(); ++i) {
        std::vector<std::vector<cv::Point>> convexHull(1);
        cv::convexHull(contoursTreeFiltered[i], convexHull[0]);
        cv::drawContours(hullsImage, convexHull, 0, 255, 1);
    }

    // Detect the dots (circles) via SimpleBlobDetector
    // Blobs filtered by parameters, stored in Keypoints vector
    cv::SimpleBlobDetector::Params params;
    params.filterByCircularity = true;
    params.minCircularity = 0.75;
    params.filterByInertia = false;
    params.filterByConvexity = false;
    params.filterByArea = true;
    params.minArea = 80;
    params.maxArea = 2000;
    params.filterByColor = true;
    params.blobColor = 0;
    cv::Ptr<cv::SimpleBlobDetector> detector = cv::SimpleBlobDetector::create(params);
    std::vector<cv::KeyPoint> keypoints;
    //detector->detect(modifiableDomino, keypoints);
    detector->detect(hullsImage, keypoints);

    int numberPips = keypoints.size();
    return numberPips;
}

bool checkForBisectLine(cv::Mat& inputImage, cv::RotatedRect& dominoRect) {
    // Process contents of a single domino
    cv::Point2f rectPoints[4];
    dominoRect.points(rectPoints);
    int minX = std::min({ rectPoints[0].x, rectPoints[1].x, rectPoints[2].x, rectPoints[3].x });
    int maxX = std::max({ rectPoints[0].x, rectPoints[1].x, rectPoints[2].x, rectPoints[3].x });
    int minY = std::min({ rectPoints[0].y, rectPoints[1].y, rectPoints[2].y, rectPoints[3].y });
    int maxY = std::max({ rectPoints[0].y, rectPoints[1].y, rectPoints[2].y, rectPoints[3].y });
    int boundingWidth = maxX - minX;
    int boundingHeight = maxY - minY;

    cv::Mat dominoROI(inputImage, cv::Range(minY, maxY), cv::Range(minX, maxX));
    cv::Mat modifiableDomino = dominoROI.clone();

    cv::Mat dominoMask(modifiableDomino.rows, modifiableDomino.cols, CV_8U, cv::Scalar(0));
    cv::Point polyVertices[4];

    cv::Point2f midpointA, midpointB, midpointC; // points along the long sides of the domino RotRec
    float slope;
    cv::RotatedRect centerThirdRect;
    // Point order is bottomLeft, topLeft, topRight, bottomRight
    // Domino is vertical orientation, so points 0 and 3 are vertices of one (short) side, 1 and 2 are vertices of other short side
    // Get the center 1/4 slice of the domino
    if (dominoRect.size.height > dominoRect.size.width) {
        slope = (rectPoints[0].y - rectPoints[1].y) / (rectPoints[0].x - rectPoints[1].x);
        // slope infinite, bad, but now we know the RotRec isn't rotated at all
        if (std::isinf(slope)) {
            midpointA.x = minX;
            midpointA.y = minY + 3 * (maxY - minY) / 8;
            midpointB.x = minX;
            midpointB.y = minY + 5 * (maxY - minY) / 8;
            midpointC.x = maxX;
            midpointC.y = midpointA.y;
            centerThirdRect = cv::RotatedRect(midpointC, midpointA, midpointB);
        }
        else {
            midpointA.x = ((rectPoints[0].x * 5) + rectPoints[1].x * 3) / 8;
            midpointA.y = rectPoints[1].y + (slope * abs(midpointA.x - rectPoints[1].x));
            midpointB.x = (rectPoints[0].x * 3 + (rectPoints[1].x * 5)) / 8;
            midpointB.y = rectPoints[1].y + (slope * abs(midpointB.x - rectPoints[1].x));
            midpointC.x = ((rectPoints[3].x * 5) + rectPoints[2].x * 3) / 8;
            midpointC.y = rectPoints[2].y + (slope * abs(midpointC.x - rectPoints[2].x));
            centerThirdRect = cv::RotatedRect(midpointB, midpointA, midpointC);
        }
    }
        // Domino is horizontal orientation, so points 0 and 1 are vertices of one (short) side, 2 and 3 are vertices of other short side
    else {
        slope = (rectPoints[1].x - rectPoints[2].x) / (rectPoints[1].y - rectPoints[2].y);
        // slope infinite, bad, but now we know the RotRect isn't rotated at all
        if (std::isinf(slope)) {
            midpointA.y = minY;
            midpointA.x = minX + 3 * (maxX - minX) / 8;
            midpointB.y = minY;
            midpointB.x = minX + 5 * (maxX - minX) / 8;
            midpointC.y = maxY;
            midpointC.x = midpointA.x;
            centerThirdRect = cv::RotatedRect(midpointC, midpointA, midpointB);
        }
        else {
            midpointA.y = ((rectPoints[1].y * 5) + rectPoints[2].y * 3) / 8;
            midpointA.x = rectPoints[1].x - (slope * abs(midpointA.y - rectPoints[1].y));
            midpointB.y = ((rectPoints[1].y * 3) + rectPoints[2].y * 5) / 8;
            midpointB.x = rectPoints[1].x - (slope * abs(midpointB.y - rectPoints[1].y));
            midpointC.y = ((rectPoints[0].y * 5) + rectPoints[3].y * 3) / 8;
            midpointC.x = rectPoints[0].x - (slope * abs(midpointC.y - rectPoints[0].y));
            centerThirdRect = cv::RotatedRect(midpointC, midpointA, midpointB);
        }
    }

    // apply mask so we're only looking at center 1/4 of domino
    cv::Point2f maskRectPoints[4];
    centerThirdRect.points(maskRectPoints);
    for (int i = 0; i < 4; ++i) {
        polyVertices[i].x = maskRectPoints[i].x - minX;
        polyVertices[i].y = maskRectPoints[i].y - minY;
    }
    cv::fillConvexPoly(dominoMask, polyVertices, 4, cv::Scalar(255));
    cv::bitwise_and(modifiableDomino, dominoMask, modifiableDomino);

    // Define a new ROI that only encompasses the bounding-box of the masked region (so fewer pixels to compute later)
    int minXMask = std::min({ maskRectPoints[0].x, maskRectPoints[1].x, maskRectPoints[2].x, maskRectPoints[3].x }) - minX;
    if (minXMask < 0) { minXMask = 0; }
    int maxXMask = std::max({ maskRectPoints[0].x, maskRectPoints[1].x, maskRectPoints[2].x, maskRectPoints[3].x }) - minX;
    if (maxXMask > modifiableDomino.cols) { maxXMask = modifiableDomino.cols; }
    int minYMask = std::min({ maskRectPoints[0].y, maskRectPoints[1].y, maskRectPoints[2].y, maskRectPoints[3].y }) - minY;
    if (minYMask < 0) { minYMask = 0; }
    int maxYMask = std::max({ maskRectPoints[0].y, maskRectPoints[1].y, maskRectPoints[2].y, maskRectPoints[3].y }) - minY;
    if (maxYMask > modifiableDomino.rows) { maxYMask= modifiableDomino.rows; }
    cv::Mat maskedROI(modifiableDomino, cv::Range(minYMask, maxYMask), cv::Range(minXMask, maxXMask));

    // filter out unwanted contours
    cv::Mat hullsImage(maskedROI.rows, maskedROI.cols, CV_8U, cv::Scalar(0));
    std::vector<std::vector<cv::Point>> contoursTree;
    cv::findContours(maskedROI, contoursTree, cv::RETR_TREE, cv::CHAIN_APPROX_NONE);
    std::vector<std::vector<cv::Point>> contoursTreeFiltered;
    for (int i = 0; i < contoursTree.size(); ++i) {
        cv::RotatedRect rect = cv::minAreaRect(contoursTree[i]);
        if (rect.size.aspectRatio() > 6 || rect.size.aspectRatio() < 0.125) {
            if (rect.size.area() > 250 && rect.size.area() < maskedROI.rows * maskedROI.cols / 7) {
                contoursTreeFiltered.push_back(contoursTree[i]);
            }
        }
    }
    for (int i = 0; i < contoursTreeFiltered.size(); ++i) {
        std::vector<std::vector<cv::Point>> convexHull(1);
        cv::convexHull(contoursTreeFiltered[i], convexHull[0]);
        cv::drawContours(hullsImage, convexHull, 0, 255, 1);
    }

    // Detect the bisecting line via SimpleBlobDetector
    cv::SimpleBlobDetector::Params params;
    params.filterByCircularity = true; // very non-circular, good filter!
    params.minCircularity = 0.0;
    params.maxCircularity = 0.35;
    params.filterByInertia = false;
    params.filterByConvexity = false;
    params.filterByArea = false;
    //params.minArea = 100;
    //params.maxArea = 6000;
    params.filterByColor = true;
    params.blobColor = 0;
    cv::Ptr<cv::SimpleBlobDetector> detector = cv::SimpleBlobDetector::create(params);
    std::vector<cv::KeyPoint> keypoints;
    detector->detect(hullsImage, keypoints);

    return (keypoints.size() > 0);
}