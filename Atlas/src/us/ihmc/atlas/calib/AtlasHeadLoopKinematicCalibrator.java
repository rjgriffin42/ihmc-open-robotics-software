package us.ihmc.atlas.calib;

import static java.lang.Double.parseDouble;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.media.j3d.Transform3D;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.optimization.UtilOptimize;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearanceRGBColor;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.humanoidRobot.partNames.LimbName;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFramePose;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.io.UtilIO;
import boofcv.struct.calib.IntrinsicParameters;

import com.yobotics.simulationconstructionset.IndexChangedListener;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicCoordinateSystem;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;

public class AtlasHeadLoopKinematicCalibrator extends AtlasKinematicCalibrator
{   
   public static String TARGET_TO_CAMERA_KEY = "targetToCamera";
   public static String CAMERA_IMAGE_KEY = "cameraImage";
   public static String CHESSBOARD_DETECTIONS_KEY = "chessboardDetections";
   

   public static final boolean useLeftArm = false;

   //YoVariables for Display
   private final YoFramePoint ypLeftEE, ypRightEE;
   private final YoFramePose yposeLeftEE, yposeRightEE, yposeBoard, yposeLeftCamera;
   private final ArrayList<Map<String, Object>> metaData;
   final ReferenceFrame cameraFrame;

   public static final RobotSide activeSide = useLeftArm ? RobotSide.LEFT : RobotSide.RIGHT;

   Transform3D targetToEE = new Transform3D();

   protected final Map<String, Double> qbias = new HashMap<>();

   private ImageIcon iiDisplay = null;
   private boolean alignCamera = true;

   private IntrinsicParameters intrinsic;
   private PlanarCalibrationTarget calibGrid = FactoryPlanarCalibrationTarget.gridChess(
         DetectChessboardInKinematicsData.boardWidth, DetectChessboardInKinematicsData.boardHeight, 0.03);

   public AtlasHeadLoopKinematicCalibrator(DRCRobotModel robotModel)
   {
      super(robotModel);
      ypLeftEE = new YoFramePoint("leftEE", ReferenceFrame.getWorldFrame(), registry);
      ypRightEE = new YoFramePoint("rightEE", ReferenceFrame.getWorldFrame(), registry);
      yposeLeftEE = new YoFramePose("leftPoseEE", "", ReferenceFrame.getWorldFrame(), registry);
      yposeRightEE = new YoFramePose("rightPoseEE", "", ReferenceFrame.getWorldFrame(), registry);
      yposeBoard = new YoFramePose("board", "", ReferenceFrame.getWorldFrame(), registry);
      yposeLeftCamera = new YoFramePose("leftCamera", "", ReferenceFrame.getWorldFrame(), registry);

      cameraFrame = fullRobotModel.getCameraFrame("stereo_camera_left");
      metaData = new ArrayList<>();
   }

   @Override
   protected void setupDynamicGraphicObjects()
   {
      //standard SCS Dynamic Graphics Object - automatically updated to the associated yoVariables
      double transparency = 0.5;
      double scale = 0.02;
      DynamicGraphicPosition dgpLeftEE = new DynamicGraphicPosition("dgpLeftEE", ypLeftEE, scale, new YoAppearanceRGBColor(Color.BLUE, transparency));
      DynamicGraphicPosition dgpRightEE = new DynamicGraphicPosition("dgpRightEE", ypRightEE, scale, new YoAppearanceRGBColor(Color.RED, transparency));

      scs.addDynamicGraphicObject(dgpLeftEE);
      scs.addDynamicGraphicObject(dgpRightEE);

      DynamicGraphicCoordinateSystem dgPoseLeftEE = new DynamicGraphicCoordinateSystem("dgposeLeftEE", yposeLeftEE, 5 * scale);
      DynamicGraphicCoordinateSystem dgPoseRightEE = new DynamicGraphicCoordinateSystem("dgposeRightEE", yposeRightEE, 5 * scale);
      DynamicGraphicCoordinateSystem dgPoseBoard = new DynamicGraphicCoordinateSystem("dgposeBoard", yposeBoard, 5 * scale);
      DynamicGraphicCoordinateSystem dgPoseLeftCamera = new DynamicGraphicCoordinateSystem("dgposeLeftCamera", yposeLeftCamera, 5 * scale);
      scs.addDynamicGraphicObject(dgPoseLeftEE);
      scs.addDynamicGraphicObject(dgPoseRightEE);
      scs.addDynamicGraphicObject(dgPoseBoard);
      scs.addDynamicGraphicObject(dgPoseLeftCamera);

      //Homemade Image Display Panel - updated by the IndexChangedListener 
      iiDisplay = new ImageIcon();
      JPanel panel = new JPanel(new BorderLayout());
      final JLabel lblDisplay = new JLabel("", iiDisplay, JLabel.CENTER);
      panel.add(lblDisplay, BorderLayout.CENTER);
      scs.addExtraJpanel(panel, "Image");
      scs.getStandardSimulationGUI().selectPanel("Image");
      scs.getDataBuffer().attachIndexChangedListener(new IndexChangedListener()
      {
         @Override
         public void indexChanged(int newIndex, double newTime)
         {
            int index = (newIndex + q.size() - 1) % q.size();
            CalibUtil.setRobotModelFromData(fullRobotModel, q.get(index), qbias);
            updateBoard(index);
            lblDisplay.repaint();
            if (alignCamera)
               scsAlignCameraToRobotCamera();
         }
      });
      //scs.getStandardSimulationGUI().selectPanel("Image");

      //Set Camera Info
      String intrinsicFile = "../DarpaRoboticsChallenge/data/calibration_images/intrinsic_ros.xml";
      IntrinsicParameters intrinsic = UtilIO.loadXML(intrinsicFile);
      double fovh = Math.atan(intrinsic.getCx() / intrinsic.getFx()) + Math.atan((intrinsic.width - intrinsic.getCx()) / intrinsic.getFx());
      System.out.println("Set fov to " + Math.toDegrees(fovh) + "degs from " + intrinsicFile);
      scs.setFieldOfView(fovh);
      scs.maximizeMainWindow();

      JCheckBox chkAlignCamera = new JCheckBox("AlignCamera", alignCamera);
      chkAlignCamera.addItemListener(new ItemListener()
      {

         @Override
         public void itemStateChanged(ItemEvent e)
         {
            alignCamera = !alignCamera;
            if (alignCamera)
               scsAlignCameraToRobotCamera();
         }
      });
      scs.addCheckBox(chkAlignCamera);
   }

   @Override
   protected void updateDynamicGraphicsObjects(int index)
   {
      /*put yo-variablized objects here */
      FramePoint leftEE = new FramePoint(fullRobotModel.getEndEffectorFrame(RobotSide.LEFT, LimbName.ARM), 0, 0.13, 0);
      FramePoint rightEE = new FramePoint(fullRobotModel.getEndEffectorFrame(RobotSide.RIGHT, LimbName.ARM), 0, -0.13, 0);

      leftEE.changeFrame(CalibUtil.world);
      rightEE.changeFrame(CalibUtil.world);

      ypLeftEE.set(leftEE);
      ypRightEE.set(rightEE);

      yposeLeftEE.set(leftEE, new FrameOrientation(CalibUtil.world));
      yposeRightEE.set(rightEE, new FrameOrientation(CalibUtil.world));

      updateBoard(index);
   }

   private void scsAlignCameraToRobotCamera()
   {
      //Camera Pos(behind the eye 10cm), Fix(Eye farme origin)
      FramePoint cameraPos = new FramePoint(cameraFrame, -0.01, 0, 0);
      FramePoint cameraFix = new FramePoint(cameraFrame);

      cameraPos.changeFrame(CalibUtil.world);
      cameraFix.changeFrame(CalibUtil.world);
      scs.setCameraPosition(cameraPos.getX(), cameraPos.getY(), cameraPos.getZ());
      scs.setCameraFix(cameraFix.getX(), cameraFix.getY(), cameraFix.getZ());
   }

   private void updateBoard(int index)
   {
      //update camera pose display
      Transform3D imageToCamera = new Transform3D(new double[]{0, 0, 1, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 1});
      ReferenceFrame cameraImageFrame = ReferenceFrame.
            constructBodyFrameWithUnchangingTransformToParent("cameraImage", cameraFrame, imageToCamera);
      FramePose poseLeftCamera = new FramePose(cameraImageFrame);
      poseLeftCamera.changeFrame(CalibUtil.world);
      yposeLeftCamera.set(poseLeftCamera);

      //update board
      Map<String, Object> mEntry = metaData.get(index);
      Transform3D targetToCamera = new Transform3D((Transform3D) mEntry.get(TARGET_TO_CAMERA_KEY)); //in camera frame
//      System.out.println("Original Rot\n"+targetToCamera);

      //update
      FramePose poseRightCamera = new FramePose(cameraImageFrame, targetToCamera);
      poseRightCamera.changeFrame(CalibUtil.world);
      yposeBoard.set(poseRightCamera);
//      System.out.println("Index: "+ index);
//      System.out.println(targetToCamera);

      //image update
      BufferedImage work = renderEEinImage(cameraImageFrame, (BufferedImage) mEntry.get(CAMERA_IMAGE_KEY));

      Transform3D kinematicsTargetToCamera = computeKinematicsTargetToCamera(cameraImageFrame);
      renderCalibrationPoints(kinematicsTargetToCamera, work);

      iiDisplay.setImage(work);
   }

   private BufferedImage renderEEinImage(ReferenceFrame cameraImageFrame, BufferedImage original)
   {
      BufferedImage work = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
      Graphics2D g2 = work.createGraphics();
      g2.drawImage(original, 0, 0, null);

      double magicNumber = useLeftArm ? 0.13 : -0.13;

      FramePoint activeArmEEtoCamera = new FramePoint(fullRobotModel.getEndEffectorFrame(activeSide, LimbName.ARM), 0, magicNumber, 0); // todo look at this later
      activeArmEEtoCamera.changeFrame(cameraImageFrame);
      Point3d activeArmEEinImageFrame = activeArmEEtoCamera.getPoint();

      Point2D_F64 norm = new Point2D_F64(activeArmEEinImageFrame.x / activeArmEEinImageFrame.z, activeArmEEinImageFrame.y / activeArmEEinImageFrame.z);
      Point2D_F64 pixel = new Point2D_F64();

      PerspectiveOps.convertNormToPixel(intrinsic, norm, pixel);

      // visualization
      int r = 10;
      int w = r * 2 + 1;
      int x = (int) (pixel.x + 0.5);
      int y = (int) (pixel.y + 0.5);

      g2.setColor(Color.BLACK);
      g2.fillOval(x - r - 2, y - r - 2, w + 4, w + 4);
      g2.setColor(Color.orange);
      g2.fillOval(x - r, y - r, w, w);

      return work;
   }

   private Transform3D computeKinematicsTargetToCamera(ReferenceFrame cameraImageFrame)
   {

//      DenseMatrix64F rotY = RotationMatrixGenerator.rotY(Math.PI/2,null);
//      DenseMatrix64F rotZ = RotationMatrixGenerator.rotZ(-Math.PI / 2, null);
//      DenseMatrix64F rot = new DenseMatrix64F(3,3);
//      CommonOps.mult(rotZ, rotY, rot);
//
//      System.out.println(rot);

//      targetToEE.setRotation(rot);
//      targetToEE.setTranslation(new Vector3d(-0.061, 0.13, 0.205));

      ReferenceFrame activeArmEEFrame = fullRobotModel.getEndEffectorFrame(activeSide, LimbName.ARM);
      ReferenceFrame boardFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("boardFrame", activeArmEEFrame, targetToEE);
      return boardFrame.getTransformToDesiredFrame(cameraImageFrame);

//      FramePoint leftEEtoCamera=new FramePoint(fullRobotModel.getEndEffectorFrame(RobotSide.LEFT, LimbName.ARM)  ,0, 0.13,0);
//      leftEEtoCamera.changeFrame(cameraImageFrame);
   }

   private void renderCalibrationPoints(Transform3D targetToCamera, BufferedImage output)
   {

      Graphics2D g2 = output.createGraphics();

      // dot size
      int r = 4;
      int w = r * 2 + 1;

      // Points in chessboard frame
      Point2D_F64 norm = new Point2D_F64();
      Point2D_F64 pixel = new Point2D_F64();

      int index = 0;
      for (Point2D_F64 p : calibGrid.points)
      {
         // convert to camera frame
         Point3d p3 = new Point3d(p.x, p.y, 0);
         targetToCamera.transform(p3);

         // convert to pixels
         norm.set(p3.x / p3.z, p3.y / p3.z);
         PerspectiveOps.convertNormToPixel(intrinsic, norm, pixel);

         int x = (int) (pixel.x + 0.5);
         int y = (int) (pixel.y + 0.5);

         if (index++ == 0)
         {
            g2.setColor(Color.CYAN);
         } else
         {
            g2.setColor(Color.BLUE);
         }
         g2.fillOval(x - r, y - r, w, w);
      }


   }

   private ArrayList<OneDoFJoint> getArmJoints()
   {
      ArrayList<OneDoFJoint> armJoints = new ArrayList<OneDoFJoint>();
      for (int i = 0; i < joints.length; i++)
      {
         if (joints[i].getName().matches(".*arm.*"))
         {
            armJoints.add(joints[i]);
            if (DEBUG)
               System.out.println("arm " + i + " " + joints[i].getName());
         }

      }
      return armJoints;
   }

   public void optimizeData()
   {
      KinematicCalibrationHeadLoopResidual function = new KinematicCalibrationHeadLoopResidual(fullRobotModel, useLeftArm, intrinsic, calibGrid, metaData, q);

      UnconstrainedLeastSquares optimizer = FactoryOptimization.leastSquaresLM(1e-3, true);

      double input[] = new double[function.getNumOfInputsN()];

      // give it an initial estimate for the translation
//      input[input.length-4]=-0.061;
//      input[input.length-3]=0.13;
//      input[input.length-2]=0.205;

      optimizer.setFunction(function, null);
      optimizer.initialize(input, 1e-12, 1e-12);

      System.out.println("Initial optimziation error = " + optimizer.getFunctionValue());

      UtilOptimize.process(optimizer, 500);

      double found[] = optimizer.getParameters();

      System.out.println("Final optimziation error =   " + optimizer.getFunctionValue());

      java.util.List<String> jointNames = function.getCalJointNames();

      targetToEE = KinematicCalibrationHeadLoopResidual.computeTargetToEE(found, jointNames.size(), useLeftArm);

      for (int i = 0; i < jointNames.size(); i++)
      {
         qbias.put(jointNames.get(i), found[i]);
         System.out.println(jointNames.get(i) + " bias: " + Math.toDegrees(found[i]));
      }
      System.out.println("board to wrist rotY:" + found[found.length - 1]);
   }

   public void loadData(String directory) throws IOException
   {
      intrinsic = UtilIO.loadXML("../DarpaRoboticsChallenge/data/calibration_images/intrinsic_ros.xml");

      File[] files = new File(directory).listFiles();

      Arrays.sort(files);

//      files = new File[]{files[3],files[20]};
      for (File f : files)
      {
         if (!f.isDirectory())
            continue;
         System.out.println("datafolder:" + f.toString());

         Map<String, Object> mEntry = new HashMap<>();
         Map<String, Double> qEntry = new HashMap<>();
         Map<String, Double> qoutEntry = new HashMap<>();

         if (!loadData(f, mEntry, qEntry, qoutEntry, true))
            continue;

         metaData.add(mEntry);
         q.add(qEntry);

      }
   }

   public static boolean loadData(File f, Map<String, Object> mEntry, Map<String, Double> qEntry, Map<String, Double> qoutEntry,
                                  boolean loadImages) throws IOException
   {
      File fileTarget = new File(f, "target.txt");

      if (!fileTarget.exists() || fileTarget.length() == 0)
         return false;

      // parse targetToCamera transform
      BufferedReader reader = new BufferedReader(new FileReader(fileTarget));

      Se3_F64 targetToCamera = new Se3_F64();

      reader.readLine();         // skip comments

      //read rotation
      String row0[] = reader.readLine().split(" ");
      String row1[] = reader.readLine().split(" ");
      String row2[] = reader.readLine().split(" ");

      for (int col = 0; col < 3; col++)
      {
         targetToCamera.getR().set(0, col, parseDouble(row0[col]));
         targetToCamera.getR().set(1, col, parseDouble(row1[col]));
         targetToCamera.getR().set(2, col, parseDouble(row2[col]));
      }

      //read translation
      reader.readLine();
      String s[] = reader.readLine().split(" ");
      targetToCamera.getT().set(parseDouble(s[0]), parseDouble(s[1]), parseDouble(s[2]));

      // read calibration point stuff
      reader.readLine();
      reader.readLine();
      ArrayList<Point2D_F64> detections = new ArrayList<>();
      while (true)
      {
         String line = reader.readLine();
         if (line == null)
            break;
         s = line.split(" ");
         Point2D_F64 p = new Point2D_F64();
         p.x = Double.parseDouble(s[0]);
         p.y = Double.parseDouble(s[1]);
         detections.add(p);
      }
      mEntry.put(CHESSBOARD_DETECTIONS_KEY, detections);

      //copy Translation and Rotation
      Transform3D transform = new Transform3D();
      Vector3D_F64 T = targetToCamera.T;
      transform.setTranslation(new Vector3d(T.x, T.y, T.z));

      Matrix3d matrix3d = new Matrix3d();
      MatrixTools.denseMatrixToMatrix3d(targetToCamera.getR(), matrix3d, 0, 0);
      transform.setRotation(matrix3d);
      mEntry.put(TARGET_TO_CAMERA_KEY, transform);

      //load image
      if (loadImages)
         mEntry.put(CAMERA_IMAGE_KEY, ImageIO.read(new File(f, "/detected.jpg")));


      // load joint angles
      Properties properties = new Properties();
      properties.load(new FileReader(new File(f, "q.m")));

      for (Map.Entry e : properties.entrySet())
      {
         qEntry.put((String) e.getKey(), Double.parseDouble((String) e.getValue()));
      }

      properties = new Properties();
      properties.load(new FileReader(new File(f, "qout.m")));

      for (Map.Entry e : properties.entrySet())
      {
         qoutEntry.put((String) e.getKey(), Double.parseDouble((String) e.getValue()));
      }

      return true;
   }


   public static void main(String[] arg) throws InterruptedException, IOException
   {
	  final AtlasRobotVersion ATLAS_ROBOT_VERSION = AtlasRobotVersion.DRC_NO_HANDS;
	  final boolean RUNNING_ON_REAL_ROBOT = true;
	  
	  DRCRobotModel robotModel = new AtlasRobotModel(ATLAS_ROBOT_VERSION,RUNNING_ON_REAL_ROBOT, RUNNING_ON_REAL_ROBOT);
	  
      AtlasHeadLoopKinematicCalibrator calib = new AtlasHeadLoopKinematicCalibrator(robotModel);
      calib.loadData("data/armCalibratoin20131209/calibration_right");
      calib.optimizeData();

      // calJointNames order is the prm order
//      KinematicCalibrationWristLoopResidual residualFunc = calib.getArmLoopResidualObject();
//      double[] prm = new double[residualFunc.getN()];
//      double[] residual0 = residualFunc.calcResiduals(prm);
//      calib.calibrate(residualFunc,prm, 100);
//      double[] residual = residualFunc.calcResiduals(prm);
//
//
//      //display prm in readable format
//      Map<String,Double> qoffset= residualFunc.prmArrayToJointMap(prm);
//      for(String jointName: qoffset.keySet())
//      {
//         System.out.println("jointAngleOffsetPreTransmission.put(AtlasJointId.JOINT_" + jointName.toUpperCase()+", "+qoffset.get(jointName)+");");
//         //System.out.println(jointName + " "+ qoffset.get(jointName));
//      }
//      System.out.println("wristSpacing "+prm[prm.length-1]);
//
      //push data to visualizer
      boolean start_scs = true;
      if (start_scs)
      {
         //Yovariables for display
//         YoFramePose yoResidual0 = new YoFramePose("residual0", "", ReferenceFrame.getWorldFrame(),calib.registry);
//         YoFramePose yoResidual = new YoFramePose("residual", "",ReferenceFrame.getWorldFrame(),calib.registry);

         calib.createDisplay(calib.q.size());

         for (int i = 0; i < calib.q.size(); i++)
         {
            CalibUtil.setRobotModelFromData(calib.fullRobotModel, (Map) calib.q.get(i));
//            CalibUtil.setRobotModelFromData(calib.fullRobotModel, CalibUtil.addQ(calib.q.get(i),qoffset));
//            yoResidual0.setXYZYawPitchRoll(Arrays.copyOfRange(residual0, i*RESIDUAL_DOF, i*RESIDUAL_DOF+6));
//            yoResidual.setXYZYawPitchRoll(Arrays.copyOfRange(residual, i*RESIDUAL_DOF, i*RESIDUAL_DOF+6));
            calib.displayUpdate(i);
         }
      } //viz

   }
}
