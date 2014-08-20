package us.ihmc.graphics3DAdapter.camera;

import java.util.ArrayList;

import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.Graphics3DAdapter;
import us.ihmc.graphics3DAdapter.input.Key;
import us.ihmc.graphics3DAdapter.input.KeyListener;
import us.ihmc.graphics3DAdapter.input.ModifierKeyInterface;
import us.ihmc.graphics3DAdapter.input.MouseButton;
import us.ihmc.graphics3DAdapter.input.MouseListener;
import us.ihmc.graphics3DAdapter.input.SelectedListener;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;

public class ClassicCameraController implements TrackingDollyCameraController, KeyListener, MouseListener, SelectedListener
{
   public static final double MIN_FIELD_OF_VIEW = 0.001;
   public static final double MAX_FIELD_OF_VIEW = 2.0;

   private static final double MIN_CAMERA_POSITION_TO_FIX_DISTANCE = 0.1; // 0.8;

   public final static double CAMERA_START_X = 0.0;
   public final static double CAMERA_START_Y = -6.0;
   public final static double CAMERA_START_Z = 1.0;

   private double fieldOfView = CameraConfiguration.DEFAULT_FIELD_OF_VIEW;
   private double clipDistanceNear = CameraConfiguration.DEFAULT_CLIP_DISTANCE_NEAR;
   private double clipDistanceFar = CameraConfiguration.DEFAULT_CLIP_DISTANCE_FAR;

   private double camX, camY, camZ, fixX, fixY, fixZ;

   private double zoom_factor = 1.0;
   private double rotate_factor = 1.0;
   private double rotate_camera_factor = 1.0;

   private boolean isMounted = false;
   private CameraMountInterface cameraMount;

   private boolean isTracking = true, isTrackingX = true, isTrackingY = true, isTrackingZ = false;
   private boolean isDolly = false, isDollyX = true, isDollyY = true, isDollyZ = false;
   private double trackDX = 0.0, trackDY = 0.0, trackDZ = 0.0;
   private double dollyDX = 2.0, dollyDY = 12.0, dollyDZ = 0.0;

   private final ViewportAdapter viewportAdapter;

   // Flying
   private boolean fly = true;
   private boolean forward = false;
   private boolean backward = false;
   private boolean left = false;
   private boolean right = false;
   private boolean up = false;
   private boolean down = false;

   private ArrayList<Point3d> storedCameraPositions = new ArrayList<Point3d>(0);
   private ArrayList<Point3d> storedFixPositions = new ArrayList<Point3d>(0);
   private int storedPositionIndex = 0;
   private boolean transitioning = false;

   private static double transitionTime = 500;
   private double camXSpeed;
   private double camYSpeed;
   private double camZSpeed;
   private double fixXSpeed;
   private double fixYSpeed;
   private double fixZSpeed;
   private long lastTransitionTime;

   private ArrayList<Point3d> keyFrameCamPos = new ArrayList<Point3d>(0);
   private ArrayList<Point3d> keyFrameFixPos = new ArrayList<Point3d>(0);
   private ArrayList<Integer> keyFrameTimes = new ArrayList<Integer>(0);

   private boolean toggleCameraKeyPoints = false;
   private int cameraKeyPointIndex;
   private ArrayList<Integer> cameraKeyPoints = new ArrayList<Integer>(0);

   private final CameraTrackingAndDollyPositionHolder cameraTrackAndDollyVariablesHolder;

   private Graphics3DAdapter graphics3dAdapter;

   public static ClassicCameraController createClassicCameraControllerAndAddListeners(ViewportAdapter viewportAdapter,
         CameraTrackingAndDollyPositionHolder cameraTrackAndDollyVariablesHolder, Graphics3DAdapter graphics3dAdapter)
   {
      ClassicCameraController classicCameraController = new ClassicCameraController(graphics3dAdapter, viewportAdapter, cameraTrackAndDollyVariablesHolder);
      graphics3dAdapter.addKeyListener(classicCameraController);
      graphics3dAdapter.addMouseListener(classicCameraController);
      graphics3dAdapter.addSelectedListener(classicCameraController);

      return classicCameraController;
   }

   public ClassicCameraController(Graphics3DAdapter graphics3dAdapter, ViewportAdapter viewportAdapter,
         CameraTrackingAndDollyPositionHolder cameraTrackAndDollyVariablesHolder)
   {
      this.graphics3dAdapter = graphics3dAdapter;
      this.viewportAdapter = viewportAdapter;

      this.camX = CAMERA_START_X;
      this.camY = CAMERA_START_Y;
      this.camZ = CAMERA_START_Z;
      this.fixX = 0.0;
      this.fixY = 0.0;
      this.fixZ = 0.6;

      this.cameraTrackAndDollyVariablesHolder = cameraTrackAndDollyVariablesHolder;

      // Don't do this stuff by default
      setTracking(false);
      setDolly(false);
   }

   public void setCameraMount(CameraMountInterface mount)
   {
      this.cameraMount = mount;
   }

   public CameraMountInterface getCameraMount()
   {
      return cameraMount;
   }

   public boolean isMounted()
   {
      return isMounted;
   }

   public CameraTrackingAndDollyPositionHolder getCameraTrackAndDollyVariablesHolder()
   {
      return cameraTrackAndDollyVariablesHolder;
   }

   public void setConfiguration(CameraConfiguration config, CameraMountList mountList)
   {
      if (config == null)
         return;

      this.isMounted = config.isCameraMounted();
      if (isMounted && (mountList != null))
      {
         this.cameraMount = mountList.getCameraMount(config.getCameraMountName());
      }

      this.camX = config.camX;
      this.camY = config.camY;
      this.camZ = config.camZ;
      this.fixX = config.fixX;
      this.fixY = config.fixY;
      this.fixZ = config.fixZ;

      this.isTracking = config.isTracking;
      this.isTrackingX = config.isTrackingX;
      this.isTrackingY = config.isTrackingY;
      this.isTrackingZ = config.isTrackingZ;
      this.isDolly = config.isDolly;
      this.isDollyX = config.isDollyX;
      this.isDollyY = config.isDollyY;
      this.isDollyZ = config.isDollyZ;

      this.trackDX = config.trackDX;
      this.trackDY = config.trackDY;
      this.trackDZ = config.trackDZ;
      this.dollyDX = config.dollyDX;
      this.dollyDY = config.dollyDY;
      this.dollyDZ = config.dollyDZ;

      setFieldOfView(config.fieldOfView);
      this.clipDistanceFar = config.clipDistanceFar;
      this.clipDistanceNear = config.clipDistanceNear;

      // this.update();
   }

   public boolean isTracking()
   {
      return isTracking;
   }

   public boolean isTrackingX()
   {
      return isTrackingX;
   }

   public boolean isTrackingY()
   {
      return isTrackingY;
   }

   public boolean isTrackingZ()
   {
      return isTrackingZ;
   }

   public boolean isDolly()
   {
      return isDolly;
   }

   public boolean isDollyX()
   {
      return isDollyX;
   }

   public boolean isDollyY()
   {
      return isDollyY;
   }

   public boolean isDollyZ()
   {
      return isDollyZ;
   }

   public void setTracking(boolean track, boolean trackX, boolean trackY, boolean trackZ)
   {
      setTracking(track);
      setTrackingX(trackX);
      setTrackingY(trackY);
      setTrackingZ(trackZ);
   }

   public void setDolly(boolean dolly, boolean dollyX, boolean dollyY, boolean dollyZ)
   {
      setDolly(dolly);
      setDollyX(dollyX);
      setDollyY(dollyY);
      setDollyZ(dollyZ);
   }

   public void setTrackingOffsets(double dx, double dy, double dz)
   {
      trackDX = dx;
      trackDY = dy;
      trackDZ = dz;
   }

   public void setDollyOffsets(double dx, double dy, double dz)
   {
      dollyDX = dx;
      dollyDY = dy;
      dollyDZ = dz;
   }

   public void setTracking(boolean track)
   {
      isTracking = track;
   }

   public void setTrackingX(boolean trackX)
   {
      isTrackingX = trackX;
   }

   public void setTrackingY(boolean trackY)
   {
      isTrackingY = trackY;
   }

   public void setTrackingZ(boolean trackZ)
   {
      isTrackingZ = trackZ;
   }

   public void setDolly(boolean dolly)
   {
      isDolly = dolly;
   }

   public void setDollyX(boolean dollyX)
   {
      isDollyX = dollyX;
   }

   public void setDollyY(boolean dollyY)
   {
      isDollyY = dollyY;
   }

   public void setDollyZ(boolean dollyZ)
   {
      isDollyZ = dollyZ;
   }

   public double getTrackingXOffset()
   {
      return trackDX;
   }

   public double getTrackingYOffset()
   {
      return trackDY;
   }

   public double getTrackingZOffset()
   {
      return trackDZ;
   }

   public double getDollyXOffset()
   {
      return dollyDX;
   }

   public double getDollyYOffset()
   {
      return dollyDY;
   }

   public double getDollyZOffset()
   {
      return dollyDZ;
   }

   public void setTrackingXOffset(double dx)
   {
      trackDX = dx;
   }

   public void setTrackingYOffset(double dy)
   {
      trackDY = dy;
   }

   public void setTrackingZOffset(double dz)
   {
      trackDZ = dz;
   }

   public void setDollyXOffset(double dx)
   {
      dollyDX = dx;
   }

   public void setDollyYOffset(double dy)
   {
      dollyDY = dy;
   }

   public void setDollyZOffset(double dz)
   {
      dollyDZ = dz;
   }

   public void update()
   {
      if (graphics3dAdapter.getContextManager().getCurrentViewport() != viewportAdapter)
      {
         forward = false;
         backward = false;
         left = false;
         right = false;
         up = false;
         down = false;
      }

      if (isTracking)
      {
         if (isTrackingX)
         {
            double trackX = cameraTrackAndDollyVariablesHolder.getTrackingX();
            if (!Double.isNaN(trackX))
               fixX = trackX + trackDX;
         }

         if (isTrackingY)
         {
            double trackY = cameraTrackAndDollyVariablesHolder.getTrackingY();
            if (!Double.isNaN(trackY))
               fixY = trackY + trackDY;
         }

         if (isTrackingZ)
         {
            double trackZ = cameraTrackAndDollyVariablesHolder.getTrackingZ();
            if (!Double.isNaN(trackZ))
               fixZ = trackZ + trackDZ;
         }
      }

      if (isDolly)
      {
         double dollyX = cameraTrackAndDollyVariablesHolder.getDollyX();
         if (isDollyX)
         {
            if (!Double.isNaN(dollyX))
               camX = dollyX + dollyDX;
         }

         if (isDollyY)
         {
            double dollyY = cameraTrackAndDollyVariablesHolder.getDollyY();
            if (!Double.isNaN(dollyY))
               camY = dollyY + dollyDY;
         }

         if (isDollyZ)
         {
            double dollyZ = cameraTrackAndDollyVariablesHolder.getDollyZ();
            if (!Double.isNaN(dollyZ))
               camZ = dollyZ + dollyDZ;
         }
      }

      double fieldOfView = cameraTrackAndDollyVariablesHolder.getFieldOfView();
      if (!Double.isNaN(fieldOfView))
         setFieldOfView(fieldOfView);

      // Flying
      if (fly && !isTracking && !isDolly && !transitioning)
      {
         if (forward)
         {
            moveCameraForward(-0.5);
         }

         if (backward)
         {
            moveCameraForward(0.5);
         }

         if (left)
         {
            pan(20, 0);
         }

         if (right)
         {
            pan(-20, 0);
         }

         if (up)
         {
            pan(00, 20);
         }

         if (down)
         {
            pan(00, -20);
         }
      }

      // End Flying

      if (transitioning && !isTracking && !isDolly)
      {
         int numberOfDimensionsThatHaveTransitioned = 0;
         double elapsedTransitionTime = System.currentTimeMillis() - lastTransitionTime;
         lastTransitionTime = System.currentTimeMillis();

         if (Math.abs(camX - storedCameraPositions.get(storedPositionIndex).x) <= Math.abs(camXSpeed * elapsedTransitionTime))
         {
            camX = storedCameraPositions.get(storedPositionIndex).x;
            numberOfDimensionsThatHaveTransitioned++;
         }
         else
         {
            camX += camXSpeed * elapsedTransitionTime;
         }

         if (Math.abs(camY - storedCameraPositions.get(storedPositionIndex).y) <= Math.abs(camYSpeed * elapsedTransitionTime))
         {
            camY = storedCameraPositions.get(storedPositionIndex).y;
            numberOfDimensionsThatHaveTransitioned++;
         }
         else
         {
            camY += camYSpeed * elapsedTransitionTime;
         }

         if (Math.abs(camZ - storedCameraPositions.get(storedPositionIndex).z) <= Math.abs(camZSpeed * elapsedTransitionTime))
         {
            camZ = storedCameraPositions.get(storedPositionIndex).z;
            numberOfDimensionsThatHaveTransitioned++;
         }
         else
         {
            camZ += camZSpeed * elapsedTransitionTime;
         }

         if (Math.abs(fixX - storedFixPositions.get(storedPositionIndex).x) <= Math.abs(fixXSpeed * elapsedTransitionTime))
         {
            fixX = storedFixPositions.get(storedPositionIndex).x;
            numberOfDimensionsThatHaveTransitioned++;
         }
         else
         {
            fixX += fixXSpeed * elapsedTransitionTime;
         }

         if (Math.abs(fixY - storedFixPositions.get(storedPositionIndex).y) <= Math.abs(fixYSpeed * elapsedTransitionTime))
         {
            fixY = storedFixPositions.get(storedPositionIndex).y;
            numberOfDimensionsThatHaveTransitioned++;
         }
         else
         {
            fixY += fixYSpeed * elapsedTransitionTime;
         }

         if (Math.abs(fixZ - storedFixPositions.get(storedPositionIndex).z) <= Math.abs(fixZSpeed * elapsedTransitionTime))
         {
            fixZ = storedFixPositions.get(storedPositionIndex).z;
            numberOfDimensionsThatHaveTransitioned++;
         }
         else
         {
            fixZ += fixZSpeed * elapsedTransitionTime;
         }

         if (numberOfDimensionsThatHaveTransitioned == 6)
         {
            transitioning = false;
         }
      }

   }

   public void addKeyFrame(int time)
   {
      addKeyFrame(keyFrameCamPos.size(), time);
   }

   public void addKeyFrame(int i, int time)
   {
      keyFrameCamPos.add(i, new Point3d(camX, camY, camZ));
      keyFrameFixPos.add(i, new Point3d(fixX, fixY, fixZ));
      keyFrameTimes.add(i, time);
   }

   public int removeKeyFrameByTime(int time)
   {
      for (int i = 0; i < keyFrameTimes.size(); i++)
      {
         if (keyFrameTimes.get(i) == time)
         {
            removeKeyFrameByIndex(i);

            return i;
         }
      }

      return -1;
   }

   public void removeKeyFrameByIndex(int i)
   {
      if ((i >= 0) && (i < keyFrameTimes.size()))
      {
         keyFrameTimes.remove(i);
         keyFrameCamPos.remove(i);
         keyFrameFixPos.remove(i);
      }
   }

   public void setKeyFrameTime(int time)
   {
      for (int i = keyFrameTimes.size() - 1; i >= 0; i--)
      {
         if (time >= keyFrameTimes.get(i))
         {
            if (keyFrameTimes.size() > i + 1)
            {
               double elapsedTime = time - keyFrameTimes.get(i);
               double totalTime = keyFrameTimes.get(i + 1) - keyFrameTimes.get(i);
               camX = keyFrameCamPos.get(i).x + (keyFrameCamPos.get(i + 1).x - keyFrameCamPos.get(i).x) * elapsedTime / totalTime;
               camY = keyFrameCamPos.get(i).y + (keyFrameCamPos.get(i + 1).y - keyFrameCamPos.get(i).y) * elapsedTime / totalTime;
               camZ = keyFrameCamPos.get(i).z + (keyFrameCamPos.get(i + 1).z - keyFrameCamPos.get(i).z) * elapsedTime / totalTime;

               fixX = keyFrameFixPos.get(i).x + (keyFrameFixPos.get(i + 1).x - keyFrameFixPos.get(i).x) * elapsedTime / totalTime;
               fixY = keyFrameFixPos.get(i).y + (keyFrameFixPos.get(i + 1).y - keyFrameFixPos.get(i).y) * elapsedTime / totalTime;
               fixZ = keyFrameFixPos.get(i).z + (keyFrameFixPos.get(i + 1).z - keyFrameFixPos.get(i).z) * elapsedTime / totalTime;
            }

            break;
         }
      }
   }

   public void gotoKey(int index)
   {
      if ((index >= 0) && (index < keyFrameCamPos.size()))
      {
         storedPositionIndex = index;
         camX = keyFrameCamPos.get(index).x;
         camY = keyFrameCamPos.get(index).y;
         camZ = keyFrameCamPos.get(index).z;

         fixX = keyFrameFixPos.get(index).x;
         fixY = keyFrameFixPos.get(index).y;
         fixZ = keyFrameFixPos.get(index).z;
      }
   }

   public ArrayList<Integer> getCameraKeyPoints()
   {
      return cameraKeyPoints;
   }

   public double getFixX()
   {
      return this.fixX;
   }

   public double getFixY()
   {
      return this.fixY;
   }

   public double getFixZ()
   {
      return this.fixZ;
   }

   public double getCamX()
   {
      return this.camX;
   }

   public double getCamY()
   {
      return this.camY;
   }

   public double getCamZ()
   {
      return this.camZ;
   }

   public void setFixX(double fx)
   {
      this.fixX = fx;

   }

   public void setFixY(double fy)
   {
      this.fixY = fy;

   }

   public void setFixZ(double fz)
   {
      this.fixZ = fz;

   }

   public void setCamX(double cx)
   {
      this.camX = cx;

   }

   public void setCamY(double cy)
   {
      this.camY = cy;

   }

   public void setCamZ(double cz)
   {
      this.camZ = cz;

   }

   public void setFixPosition(double fx, double fy, double fz)
   {
      this.fixX = fx;
      this.fixY = fy;
      this.fixZ = fz;

   }

   public void setCameraPosition(double cx, double cy, double cz)
   {
      this.camX = cx;
      this.camY = cy;
      this.camZ = cz;

   }

   private Vector3d v3d = new Vector3d();
   private Transform3D t3d = new Transform3D();
   private Vector3d rotVector = new Vector3d();
   private AxisAngle4d rotAxisAngle4d = new AxisAngle4d();

   public void doMouseDraggedLeft(double dx, double dy)
   {
      // Rotate around fix point:

      double delX0 = camX - fixX, delY0 = camY - fixY, delZ0 = camZ - fixZ;
      v3d.set(delX0, delY0, delZ0);

      // double offsetDistance = v3d.length();

      t3d.rotZ(-dx * rotate_factor);
      t3d.transform(v3d);

      if (!isDolly || (!isDollyX && !isDollyY))
      {
         camX = v3d.x + fixX;
         camY = v3d.y + fixY;
      }

      delX0 = camX - fixX;
      delY0 = camY - fixY;
      delZ0 = camZ - fixZ;

      // v3d.set(delX0, delY0, delZ0);
      rotVector.cross(new Vector3d(0.0, 0.0, -1.0), v3d);
      rotAxisAngle4d.set(rotVector, dy * rotate_factor / 4.0);

      t3d.set(rotAxisAngle4d);
      t3d.transform(v3d);

      if ((v3d.x * delX0 > 0.0) && (v3d.y * delY0 > 0.0))
      {
         if (!isDolly || (!isDollyX && !isDollyY))
         {
            camX = v3d.x + fixX;
            camY = v3d.y + fixY;
         }

         if (!isDolly || !isDollyZ)
         {
            camZ = v3d.z + fixZ;

            /*
             * double factor = elevate_factor * Math.abs(offsetDistance);
             * //camZ-fixZ); if (factor < elevate_factor) factor =
             * elevate_factor; //camZ = v3d.z + fixZ + dy * elevate_factor; camZ
             * = v3d.z + fixZ + dy * factor;
             */
         }
      }

      // transformChanged(currXform);

   }

   public void doMouseDraggedRight(double dx, double dy)
   {
      // Elevate up and down
      double delX0 = camX - fixX, delY0 = camY - fixY, delZ0 = camZ - fixZ;
      v3d.set(delX0, delY0, delZ0);

      // double offsetDistance = v3d.length();

      t3d.rotZ(-dx * rotate_camera_factor);
      t3d.transform(v3d);

      if (!isTracking || (!isTrackingX && !isTrackingY))
      {
         fixX = camX - v3d.x;
         fixY = camY - v3d.y;
      }

      delX0 = camX - fixX;
      delY0 = camY - fixY;
      delZ0 = camZ - fixZ;

      // v3d.set(delX0, delY0, delZ0);

      rotVector.set(-1.0, 0.0, 0.0);
      rotVector.cross(new Vector3d(0.0, 0.0, -1.0), v3d);
      rotAxisAngle4d.set(rotVector, dy * rotate_camera_factor / 4.0);

      t3d.set(rotAxisAngle4d);
      t3d.transform(v3d);

      if ((v3d.x * delX0 > 0.0) && (v3d.y * delY0 > 0.0))
      {
         if (!isTracking || (!isTrackingX && !isTrackingY))
         {
            fixX = camX - v3d.x;
            fixY = camY - v3d.y;
         }

         if (!isTracking || !isTrackingZ)
         {
            fixZ = camZ - v3d.z;

            /*
             * double factor = elevate_camera_factor * offsetDistance;
             * //Math.abs(camZ-fixZ); if (factor < elevate_camera_factor) factor
             * = elevate_camera_factor; fixZ = camZ - v3d.z + dy factor; //fixZ
             * = camZ - v3d.z + dy * elevate_factor;
             */
         }
      }

      // transformChanged(currXform);

   }

   public void setFieldOfView(double fov)
   {
      fieldOfView = fov;

      if (fieldOfView < MIN_FIELD_OF_VIEW)
         fieldOfView = MIN_FIELD_OF_VIEW;
      if (fieldOfView > MAX_FIELD_OF_VIEW)
         fieldOfView = MAX_FIELD_OF_VIEW;
   }

   public void doMouseDraggedMiddle(double dx, double dy)
   {
      // Zooms in and out

      if ((this.isMounted) && (viewportAdapter != null))
      {
         cameraMount.zoom(dy * 0.1);
      }

      else
      {
         Vector3d v3d = new Vector3d(camX - fixX, camY - fixY, camZ - fixZ);

         Vector3d offsetVec = new Vector3d(v3d);

         // offsetVec.normalize();
         offsetVec.scale(dy * zoom_factor);

         // if (offsetVec.length() < v3d.length())
         // {
         if (!isDolly || (!isDollyX && !isDollyY))
         {
            camX += offsetVec.x;
            camY += offsetVec.y;
         }

         if (!isDolly || !isDollyZ)
            camZ += offsetVec.z;

         // }

         v3d.set(camX - fixX, camY - fixY, camZ - fixZ);

         if (v3d.length() < MIN_CAMERA_POSITION_TO_FIX_DISTANCE)
         {
            v3d.normalize();
            v3d.scale(MIN_CAMERA_POSITION_TO_FIX_DISTANCE);
            camX = v3d.x + fixX;
            camY = v3d.y + fixY;
            camZ = v3d.z + fixZ;
         }
      }

      // transformChanged(currXform);

   }

   private void moveCameraForward(double distance)
   {
      double angleXY = Math.atan2(camY - fixY, camX - fixX);
      double angleZ = Math.atan2(camZ - fixZ, Math.hypot(camY - fixY, camX - fixX));

      double distXY = distance * Math.cos(angleZ);

      camX += distXY * Math.cos(angleXY);
      camY += distXY * Math.sin(angleXY);
      camZ += distance * Math.sin(angleZ);

      if (Math.sqrt(Math.pow(camX - fixX, 2) + Math.pow(camY - fixY, 2) + Math.pow(camY - fixY, 2)) < 1)
      {
         fixX += distXY * Math.cos(angleXY);
         fixY += distXY * Math.sin(angleXY);
         fixZ += distance * Math.sin(angleZ);
      }

      // Vector3d v3d = new Vector3d(camX - fixX, camY - fixY, camZ - fixZ);
      //
      // Vector3d offsetVec = new Vector3d(v3d);
      //
      //// offsetVec.normalize();
      // offsetVec.scale(distance * zoom_factor);
      //
      //// if (offsetVec.length() < v3d.length())
      //// {
      // if (!isDolly || (!isDollyX &&!isDollyY))
      // {
      // camX += offsetVec.x;
      // camY += offsetVec.y;
      // }
      //
      // if (!isDolly ||!isDollyZ)
      // camZ += offsetVec.z;
   }

   public void pan(double dx, double dy)
   {
      double distanceFromCameraToFix = Math.sqrt(Math.pow(camX - fixX, 2) + Math.pow(camY - fixY, 2) + Math.pow(camZ - fixZ, 2));
      dx *= distanceFromCameraToFix / viewportAdapter.getPhysicalWidth() * .00023;
      dy *= distanceFromCameraToFix / viewportAdapter.getPhysicalHeight() * .00007;
      double theta = Math.PI / 2 + Math.atan2((camZ - fixZ), Math.hypot(camX - fixX, camY - fixY));
      if (!isTracking || !isTrackingZ)
      {
         camZ += dy * Math.sin(theta);
         fixZ += dy * Math.sin(theta);
      }

      double d = dy * Math.cos(theta);
      theta = Math.atan2(camY - fixY, camX - fixX);

      if (!isTracking || !isTrackingY)
      {
         camY += d * Math.sin(theta);
         fixY += d * Math.sin(theta);
      }

      if (!isTracking || !isTrackingX)
      {
         camX += d * Math.cos(theta);
         fixX += d * Math.cos(theta);
      }

      theta = Math.PI / 2 + Math.atan2(camY - fixY, camX - fixX);

      if (!isTracking || !isTrackingY)
      {
         camY -= dx * Math.sin(theta);
         fixY -= dx * Math.sin(theta);
      }

      if (!isTracking || !isTrackingX)
      {
         camX -= dx * Math.cos(theta);
         fixX -= dx * Math.cos(theta);
      }

   }

   private void initTransition()
   {
      camXSpeed = -(camX - storedCameraPositions.get(storedPositionIndex).x) / transitionTime;
      camYSpeed = -(camY - storedCameraPositions.get(storedPositionIndex).y) / transitionTime;
      camZSpeed = -(camZ - storedCameraPositions.get(storedPositionIndex).z) / transitionTime;

      fixXSpeed = -(fixX - storedFixPositions.get(storedPositionIndex).x) / transitionTime;
      fixYSpeed = -(fixY - storedFixPositions.get(storedPositionIndex).y) / transitionTime;
      fixZSpeed = -(fixZ - storedFixPositions.get(storedPositionIndex).z) / transitionTime;

      transitioning = true;
      lastTransitionTime = System.currentTimeMillis();
   }

   public void toggleCameraKeyMode()
   {
      setUseCameraKeyPoints(!useKeyCameraPoints());
   }

   public boolean getCameraKeyMode()
   {
      return toggleCameraKeyPoints;
   }

   public void setUseCameraKeyPoints(boolean use)
   {
      toggleCameraKeyPoints = use;
   }

   public boolean useKeyCameraPoints()
   {
      return toggleCameraKeyPoints;
   }

   public boolean setCameraKeyPoint(int time)
   {
      boolean added = false;
      for (int i = 0; i < cameraKeyPoints.size(); i++)
      {
         if (cameraKeyPoints.get(i) == time)
         {
            addKeyFrame(removeKeyFrameByTime(time), time);

            return false;
         }

         if (cameraKeyPoints.get(i) > time)
         {
            cameraKeyPoints.add(time);
            addKeyFrame(1, time);

            return true;
         }
      }

      if (!added)
      {
         cameraKeyPoints.add(time);
         addKeyFrame(time);
      }

      return true;
   }

   public void nextCameraKeyPoint(int time)
   {
      // int closestLesserTime = Integer.MIN_VALUE;
      // int index = -1;
      // for (int i = 0; i < cameraKeyPoints.size(); i++)
      // {
      // if (cameraKeyPoints.get(i) < time && cameraKeyPoints.get(i) > closestLesserTime)
      // {
      // index = i;
      // closestLesserTime = cameraKeyPoints.get(i);
      // }
      // }
      // if (index != -1)
      // {
      // camera.initTransition(index);
      // }
      cameraKeyPointIndex++;

      if (cameraKeyPointIndex >= cameraKeyPoints.size())
      {
         cameraKeyPointIndex = 0;
      }

      toggleCameraKeyPoints = false;
      gotoKey(cameraKeyPointIndex);
   }

   public void previousCameraKeyPoint(int time)
   {
      cameraKeyPointIndex--;

      if (cameraKeyPointIndex < 0)
      {
         cameraKeyPointIndex = cameraKeyPoints.size() - 1;
      }

      toggleCameraKeyPoints = false;
      gotoKey(cameraKeyPointIndex);
   }

   public void removeCameraKeyPoint(int time)
   {
      cameraKeyPoints.remove(cameraKeyPointIndex);
      removeKeyFrameByIndex(cameraKeyPointIndex);
   }

   public double getTrackXVar()
   {
      return cameraTrackAndDollyVariablesHolder.getTrackingX();
   }

   public double getTrackYVar()
   {
      return cameraTrackAndDollyVariablesHolder.getTrackingY();
   }

   public double getTrackZVar()
   {
      return cameraTrackAndDollyVariablesHolder.getTrackingZ();
   }

   public double getDollyXVar()
   {
      return cameraTrackAndDollyVariablesHolder.getDollyX();
   }

   public double getDollyYVar()
   {
      return cameraTrackAndDollyVariablesHolder.getDollyY();
   }

   public double getDollyZVar()
   {
      return cameraTrackAndDollyVariablesHolder.getDollyZ();
   }

   public void nextStoredPosition()
   {
      if (storedCameraPositions.size() > 0)
      {
         storedPositionIndex++;

         if (storedPositionIndex >= storedCameraPositions.size())
         {
            storedPositionIndex = 0;
         }

         initTransition();
      }
   }

   public void previousStoredPosition()
   {
      if (storedCameraPositions.size() > 0)
      {
         storedPositionIndex--;

         if (storedPositionIndex < 0)
         {
            storedPositionIndex = storedCameraPositions.size() - 1;
         }

         initTransition();
      }
   }

   public void storePosition()
   {
      storedCameraPositions.add(new Point3d(getCamX(), getCamY(), getCamZ()));
      storedFixPositions.add(new Point3d(getFixX(), getFixY(), getFixZ()));
   }

   public void reset()
   {
      // TODO Auto-generated method stub

   }

   private Matrix3d rotationMatrix = new Matrix3d();
   private Vector3d positionOffset = new Vector3d();

   private Vector3d zAxis = new Vector3d(), yAxis = new Vector3d(), xAxis = new Vector3d();

   public void computeTransform(Transform3D currXform)
   {
      update();
      CameraMountInterface cameraMount = getCameraMount();
      if (isMounted() && (cameraMount != null))
      {
         cameraMount.getTransformToCamera(currXform);

         return;
      }

      //    // Note: The following you should be able to just do with Transform3D.lookAt()...
      //    double delX = getCamX() - getFixX();
      //    if (Double.isNaN(delX))
      //       return;
      //    double delY = getCamY() - getFixY();
      //    if (Double.isNaN(delY))
      //       return;
      //    double delZ = getCamZ() - getFixZ();
      //    if (Double.isNaN(delZ))
      //       return;
      //
      //    // Z axis points away from camera look ray...
      //    zAxis.set(delX, delY, delZ);
      //
      //    if (zAxis.length() < 1.0e-10)
      //       zAxis.set(0.0, 0.0, -1.0); // Prevent "non-congruent transform above ViewPlatform exceptions...
      //    if (zAxis.length() > 1.0e100)
      //       return;
      //
      //    zAxis.normalize();
      //
      //    yAxis.set(0.0, 0.0, 1.0);
      //
      //    if (yAxis.equals(zAxis))
      //    {
      //       yAxis.set(0.0, 1.0, 0.0);
      //    }
      //
      //    xAxis.cross(yAxis, zAxis);
      //    xAxis.normalize();
      //
      //    yAxis.cross(zAxis, xAxis);
      //
      //    /*
      //     * double[] t3DMatrix = new double[] {xAxis.x, yAxis.x, zAxis.x,
      //     * classicCameraController.getCamX(), xAxis.y, yAxis.y, zAxis.y,
      //     * classicCameraController.getCamY(), xAxis.z, yAxis.z, zAxis.z,
      //     * classicCameraController.getCamZ(), 0.0, 0.0, 0.0, 1.0};
      //     */
      //
      //    t3DMatrix[0] = xAxis.x;
      //    t3DMatrix[1] = yAxis.x;
      //    t3DMatrix[2] = zAxis.x;
      //    t3DMatrix[3] = getCamX();
      //    t3DMatrix[4] = xAxis.y;
      //    t3DMatrix[5] = yAxis.y;
      //    t3DMatrix[6] = zAxis.y;
      //    t3DMatrix[7] = getCamY();
      //    t3DMatrix[8] = xAxis.z;
      //    t3DMatrix[9] = yAxis.z;
      //    t3DMatrix[10] = zAxis.z;
      //    t3DMatrix[11] = getCamZ();
      //    t3DMatrix[12] = 0.0;
      //    t3DMatrix[13] = 0.0;
      //    t3DMatrix[14] = 0.0;
      //    t3DMatrix[15] = 1.0;

      positionOffset.set(getCamX(), getCamY(), getCamZ());
      xAxis.set(getFixX(), getFixY(), getFixZ());

      xAxis.sub(positionOffset);
      xAxis.normalize();
      zAxis.set(0.0, 0.0, 1.0);
      yAxis.cross(zAxis, xAxis);
      zAxis.cross(xAxis, yAxis);

      rotationMatrix.setColumn(0, xAxis);
      rotationMatrix.setColumn(1, yAxis);
      rotationMatrix.setColumn(2, zAxis);

      currXform.set(rotationMatrix);
      currXform.setTranslation(positionOffset);

   }

   public void keyPressed(Key key)
   {
      if (graphics3dAdapter.getContextManager().getCurrentViewport() != viewportAdapter)
         return;

      switch (key)
      {
      case W:
         forward = true;

         break;

      case S:
         backward = true;

         break;

      case A:
         left = true;

         break;

      case D:
         right = true;

         break;

      case Q:
         up = true;

         break;

      case Z:
         down = true;

         break;
      }

   }

   public void keyReleased(Key key)
   {
      if (graphics3dAdapter.getContextManager().getCurrentViewport() != viewportAdapter)
         return;

      switch (key)
      {
      case W:
         forward = false;

         break;

      case S:
         backward = false;

         break;

      case A:
         left = false;

         break;

      case D:
         right = false;

         break;

      case Q:
         up = false;

         break;

      case Z:
         down = false;

         break;

      case RIGHT:
         nextStoredPosition();

         break;

      case LEFT:
         previousStoredPosition();

         break;

      case K:
         storePosition();

         break;
      }

   }

   public void selected(Graphics3DNode graphics3dNode, ModifierKeyInterface modifierKeyHolder, Point3d location, Point3d cameraLocation, Quat4d cameraRotation)
   {
      if (graphics3dAdapter.getContextManager().getCurrentViewport() != viewportAdapter)
         return;

      if (modifierKeyHolder.isKeyPressed(Key.SHIFT))
      {
         if (!isTracking() || !isTrackingX())
            setFixX(location.x);
         if (!isTracking() || !isTrackingY())
            setFixY(location.y);
         if (!isTracking() || !isTrackingZ())
            setFixZ(location.z);
      }
   }

   public void mouseDragged(MouseButton mouseButton, double dx, double dy)
   {
      if (graphics3dAdapter.getContextManager().getCurrentViewport() != viewportAdapter)
         return;

      switch (mouseButton)
      {
      case LEFT:
         doMouseDraggedLeft(dx, dy);

         break;

      case RIGHT:
         doMouseDraggedRight(dx, dy);

         break;

      case MIDDLE:
         doMouseDraggedMiddle(dx, dy);

         break;

      case LEFTRIGHT:
         pan(dx, dy);

         break;
      }
   }

   public double getClipNear()
   {
      if (isMounted)
      {
         return cameraMount.getClipDistanceNear();
      }
      else
      {
         return clipDistanceNear;
      }
   }

   public double getClipFar()
   {
      if (isMounted)
      {
         return cameraMount.getClipDistanceFar();
      }
      else
      {
         return clipDistanceFar;
      }
   }

   public double getHorizontalFieldOfViewInRadians()
   {
      if (isMounted)
      {
         return cameraMount.getFieldOfView();
      }
      else
      {
         return fieldOfView;
      }
   }

   public void setClipDistanceNear(double near)
   {
      clipDistanceNear = near;
   }

   public void setClipDistanceFar(double far)
   {
      clipDistanceFar = far;
   }

   public void copyPositionTrackingDollyConfiguration(TrackingDollyCameraController otherCamera)
   {
      setTracking(otherCamera.isTracking(), otherCamera.isTrackingX(), otherCamera.isTrackingY(), otherCamera.isTrackingZ());
      setDolly(otherCamera.isDolly(), otherCamera.isDollyX(), otherCamera.isDollyY(), otherCamera.isDollyZ());

      setCameraPosition(otherCamera.getCamX(), otherCamera.getCamY(), otherCamera.getCamZ());

      setFixPosition(otherCamera.getFixX(), otherCamera.getFixY(), otherCamera.getFixZ());

      setDollyOffsets(otherCamera.getDollyXOffset(), otherCamera.getDollyYOffset(), otherCamera.getDollyZOffset());
      setTrackingOffsets(otherCamera.getTrackingXOffset(), otherCamera.getTrackingYOffset(), otherCamera.getTrackingZOffset());

      if (otherCamera instanceof ClassicCameraController)
      {
         ClassicCameraController classicOtherCamera = (ClassicCameraController) otherCamera;

         keyFrameCamPos = classicOtherCamera.keyFrameCamPos;
         keyFrameFixPos = classicOtherCamera.keyFrameFixPos;
         keyFrameTimes = classicOtherCamera.keyFrameTimes;

         toggleCameraKeyPoints = classicOtherCamera.toggleCameraKeyPoints;
         cameraKeyPointIndex = classicOtherCamera.cameraKeyPointIndex;
         cameraKeyPoints = classicOtherCamera.cameraKeyPoints;

         System.out.println("Copying camera keys");
      }

   }
}
