package us.ihmc.commonWalkingControlModules.controlModules.pelvisOrientation;

import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.PelvisOrientationControlModule;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.YoVariableRegistry;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.util.controller.PDController;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoVariable;
import com.yobotics.simulationconstructionset.util.math.filter.FilteredVelocityYoVariable;

public class YawPitchRollPelvisOrientationControlModule implements PelvisOrientationControlModule
{
   private final ProcessedSensorsInterface processedSensors;
   private final CommonWalkingReferenceFrames commonWalkingReferenceFrames;
   private final YoVariableRegistry registry = new YoVariableRegistry("OrientationController");

   private ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final DoubleYoVariable pelvisOrientationYaw = new DoubleYoVariable("pelvisOrientationYaw", registry);
   private final DoubleYoVariable pelvisOrientationPitch = new DoubleYoVariable("pelvisOrientationPitch", registry);
   private final DoubleYoVariable pelvisOrientationRoll = new DoubleYoVariable("pelvisOrientationRoll", registry);
   
   private final DoubleYoVariable alphaPelvisAngleDerivative = new DoubleYoVariable("alphaPelvisAngleDerivative", registry);
   
   private final FilteredVelocityYoVariable yawd;
   private final FilteredVelocityYoVariable pitchd;
   private final FilteredVelocityYoVariable rolld;

   private final PDController yawController = new PDController("pelvisYaw", registry);
   private final PDController pitchController = new PDController("pelvisPitch", registry);
   private final PDController rollController = new PDController("pelvisRoll", registry);

   private final DoubleYoVariable tauYaw = new DoubleYoVariable("tauYaw", registry);
   private final DoubleYoVariable tauPitch = new DoubleYoVariable("tauPitch", registry);
   private final DoubleYoVariable tauRoll = new DoubleYoVariable("tauRoll", registry);

   private final DoubleYoVariable NxExtra = new DoubleYoVariable("NxExtra", registry);

   public YawPitchRollPelvisOrientationControlModule(ProcessedSensorsInterface processedSensors, CommonWalkingReferenceFrames commonWalkingReferenceFrames,YoVariableRegistry parentRegistry, double controlDT)
   {
      this.processedSensors = processedSensors;
      this.commonWalkingReferenceFrames = commonWalkingReferenceFrames;
      parentRegistry.addChild(registry);
      
      double breakFrequencyInHertz = 200.0;
      alphaPelvisAngleDerivative.set(AlphaFilteredYoVariable.computeAlphaGivenBreakFrequency(breakFrequencyInHertz, controlDT));

      yawd = new FilteredVelocityYoVariable("pelvisOrientationYawd", "", alphaPelvisAngleDerivative, pelvisOrientationYaw, controlDT, registry);
      pitchd = new FilteredVelocityYoVariable("pelvisOrientationPitchd", "", alphaPelvisAngleDerivative, pelvisOrientationPitch, controlDT, registry);
      rolld = new FilteredVelocityYoVariable("pelvisOrientationRolld", "", alphaPelvisAngleDerivative, pelvisOrientationRoll, controlDT, registry);
   }

   public FrameVector computePelvisTorque(RobotSide supportLeg, FrameOrientation desiredPelvisOrientation)
   {
      ReferenceFrame spineRollFrame = commonWalkingReferenceFrames.getPelvisFrame();

      double[] desiredYawPitchRoll = getDesiredYawPitchRollInWorldFrame(desiredPelvisOrientation);
      double desiredYaw = desiredYawPitchRoll[0];
      double desiredPitch = desiredYawPitchRoll[1];
      double desiredRoll = desiredYawPitchRoll[2];

      double[] yawPitchRoll = getActualYawPitchRollInWorldFrame();
      pelvisOrientationYaw.set(yawPitchRoll[0]);
      pelvisOrientationPitch.set(yawPitchRoll[1]);
      pelvisOrientationRoll.set(yawPitchRoll[2]);

      updateDerivatives();

      // compute torques
      computeTorques(desiredYaw, desiredPitch, desiredRoll);

      // Reduce tauRoll on the pelvis to let the roll fall. Can also do this by decreasing Fz, but then the knees buckle.
      if (supportLeg != null)
      {
         tauRoll.set(tauRoll.getDoubleValue() + supportLeg.negateIfRightSide(NxExtra.getDoubleValue()));
      }

      limitYawTorque();

      // return. Note! Don't put these in yaw, pitch roll order. They need to be Nx, Ny, Nz, meaning put them in roll, pitch, yaw order...
      FrameVector ret = new FrameVector(spineRollFrame, tauRoll.getDoubleValue(), tauPitch.getDoubleValue(), tauYaw.getDoubleValue());

      return ret;
   }

   private void limitYawTorque()
   {
      double maxYawTorque = 150.0;
      tauYaw.set(MathTools.clipToMinMax(tauYaw.getDoubleValue(), -maxYawTorque, maxYawTorque));
   }

   public void setNxExtra(double NxExtra)
   {
      this.NxExtra.set(NxExtra);
   }

   private void updateDerivatives()
   {
      yawd.updateForAngles();
      pitchd.updateForAngles();
      rolld.updateForAngles();
   }

   private void computeTorques(double desiredYaw, double desiredPitch, double desiredRoll)
   {
      // Note: you are not allowed to subtract yaws directly, so use computeForAngles:
      tauYaw.set(yawController.computeForAngles(pelvisOrientationYaw.getDoubleValue(), desiredYaw, yawd.getDoubleValue(), 0.0));
      tauPitch.set(pitchController.computeForAngles(pelvisOrientationPitch.getDoubleValue(), desiredPitch, pitchd.getDoubleValue(), 0.0));
      tauRoll.set(rollController.computeForAngles(pelvisOrientationRoll.getDoubleValue(), desiredRoll, rolld.getDoubleValue(), 0.0));
   }

   private double[] getActualYawPitchRollInWorldFrame()
   {
      FrameOrientation pelvisOrientation = processedSensors.getPelvisOrientationInFrame(worldFrame);
      double[] yawPitchRoll = pelvisOrientation.getYawPitchRoll();

      return yawPitchRoll;
   }

   private double[] getDesiredYawPitchRollInWorldFrame(FrameOrientation desiredPelvisOrientation)
   {
      desiredPelvisOrientation.changeFrame(worldFrame);
      double[] desiredYawPitchRoll = desiredPelvisOrientation.getYawPitchRoll();

      return desiredYawPitchRoll;
   }

   public void setGainsForR2()
   {
      yawController.setProportionalGain(1000.0); //5000.0); //50.0);    // 200.0);  800  // 1000.0);
      pitchController.setProportionalGain(800.0);    // 1000.0);
      rollController.setProportionalGain(1000.0);    // 2500.0);

      yawController.setDerivativeGain(50.0);    // 150.0);
      pitchController.setDerivativeGain(150.0);
      rollController.setDerivativeGain(200.0);

      this.NxExtra.set(50.0);    // 100.0);
   }
   
   public void setGainsForM2V2()
   {
      rollController.setProportionalGain(350.0);    // 400.0; //500.0;
      pitchController.setProportionalGain(300.0);    // 250.0; //350.0; //200.0;
      yawController.setProportionalGain(60.0);    // 15.0; //20.0; //0.0; //200.0;

      rollController.setDerivativeGain(30.0);    // 20.0;
      pitchController.setDerivativeGain(18.0);    // 15.0; //20.0;
      yawController.setDerivativeGain(5.0);    // 0.0; //20.0;
      
      this.NxExtra.set(0.0);
   }

}
