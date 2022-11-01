#pragma once

#include "opencv4/opencv2/opencv.hpp"

#include "application_state.h"
#include "point_landmark.h"
#include "camera_model.h"
#include "keyframe.h"


class VisualOdometry
{
   public:
      VisualOdometry(ApplicationState& app);
      
      void Initialize(cv::Mat& leftImageCur, cv::Mat& rightImageCur);
      bool UpdateStereo(const cv::Mat& leftImage, const cv::Mat& rightImage);
      void UpdateMonocular(const cv::Mat& image);
      void UpdateStereoExternal(cv::Mat& leftImageCur, cv::Mat& rightImageCur);

      void ExtractPoseLinear();
      void ExtractKeypoints_FAST(cv::Mat img_1, std::vector<cv::Point2f>& points1);
      void ExtractKeypoints(cv::Mat img, std::vector<cv::KeyPoint>& points, cv::Mat& desc);
      void TrackKeypoints(cv::Mat prev, cv::Mat cur, std::vector<cv::Point2f>& prev_pts, std::vector<cv::Point2f>& cur_pts);
      void MatchKeypoints(cv::Mat& desc1, cv::Mat& desc2, std::vector<cv::DMatch>& matches);
      void GridSampleKeypoints(std::vector<cv::KeyPoint>& keypoints, std::vector<cv::DMatch>& matches);
      void ExtractMatchesAsPoints(const std::vector<cv::KeyPoint>& keypoints, std::vector<cv::Point2f>& points);
      void FilterMatchesByDistance(std::vector<cv::DMatch>& matches, const std::vector<cv::KeyPoint>& kpTrain, const std::vector<cv::KeyPoint>& kpQuery, float distanceThreshold);
      void ExtractFinalSet(std::vector<cv::DMatch> leftMatches, std::vector<cv::KeyPoint> curLeftKp, std::vector<PointLandmark>& points3D);
      void CalculateOdometry_ORB(Keyframe& kf, cv::Mat leftImage, cv::Mat rightImage, cv::Mat& cvPose, std::vector<PointLandmark>& points3D);
      void CalculateOdometry_FAST(Eigen::Matrix4f& transform);
      void TriangulateStereoNormal(std::vector<cv::KeyPoint>& pointsTrain, std::vector<cv::KeyPoint>& pointsQuery, std::vector<cv::DMatch>& matches,
                                   std::vector<PointLandmark>& points3D);
      void TriangulateKeypointsByDisparity(const std::vector<cv::KeyPoint>& kp, const cv::Mat& disparity, std::vector<Eigen::Vector3f>& points3d);
      void ExtractMatchesAsPoints(const std::vector<cv::KeyPoint>& kpTrain, const std::vector<cv::KeyPoint>& kpQuery, const std::vector<cv::DMatch>& matches, std::vector<cv::Point2f>& pointsTrain, std::vector<cv::Point2f>& pointsQuery);


      cv::Mat EstimateMotion(std::vector<cv::Point2f>& prevFeatures, std::vector<cv::Point2f>& curFeatures, cv::Mat& mask, const CameraModel& cam);
      cv::Mat TriangulatePoints(std::vector<cv::Point2f>& prevPoints, std::vector<cv::Point2f>& curPoints, const CameraModel& cam, cv::Mat relativePose);
      cv::Mat CalculateStereoDepth(cv::Mat left, cv::Mat right);
      

      void DrawLandmarks(cv::Mat& img, std::vector<PointLandmark>& landmarks);
      void DrawAllMatches(cv::Mat& image);
      void Display(cv::Mat& image);
      void Show(int delay = 1);

   private:
      ApplicationState _appState;
      Eigen::Matrix4f cameraPose;

      bool _initialized = false;
      float scalar = 0.03f;
      uint32_t count = 0;
      uint32_t kFeatures = 1200;
      uint32_t kMinFeatures = 1000;
      uint32_t width = 0;
      uint32_t height = 0;
      uint32_t xGridCount = 60;
      uint32_t yGridCount = 30;


      cv::Ptr<cv::StereoBM> stereo = cv::StereoBM::create();
      cv::Ptr<cv::ORB> _orb = cv::ORB::create(kFeatures);

      std::vector<Keyframe> _keyframes;
      std::vector<cv::DMatch> matchesLeft, matchesRight, prevMatchesStereo, curMatchesStereo;
      std::vector<cv::KeyPoint> kp_prevLeft, kp_prevRight, kp_curLeft, kp_curRight;
      std::vector<PointLandmark> _prevPoints3D, _curPoints3D;
      std::vector<cv::Point2f> prevFeaturesLeft, curFeaturesLeft;
      std::vector<cv::Point2f> prevPoints2D, curPoints2D;
      
      cv::Mat curDisparity;
      cv::Mat curFinalDisplay, prevFinalDisplay;
      cv::Mat curPoseLeft, prevPosLeft, curPoseRight, prevPoseRight;
      cv::Mat desc_prevRight, desc_prevLeft, desc_curRight, desc_curLeft;
      cv::Mat prevLeft, prevRight, curLeft, curRight, leftImage, rightImage;
      
      cv::Mat cvCurPose = cv::Mat::eye(4,4, CV_32F);

      CameraModel leftCamera;
      CameraModel rightCamera;

      double baselineDistance = 0.5;

};
