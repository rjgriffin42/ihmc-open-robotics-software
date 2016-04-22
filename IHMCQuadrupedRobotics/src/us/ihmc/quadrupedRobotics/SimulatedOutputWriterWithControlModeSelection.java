package us.ihmc.quadrupedRobotics;

import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.OutputWriter;
import us.ihmc.SdfLoader.SDFFullQuadrupedRobotModel;
import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.SdfLoader.SDFPerfectSimulatedOutputWriter;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.SdfLoader.models.FullRobotModel;
import us.ihmc.SdfLoader.partNames.JointRole;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.aware.model.QuadrupedActuatorParameters;
import us.ihmc.aware.model.QuadrupedRobotParameters;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.controllers.PDController;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.simulationconstructionset.GroundContactPoint;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;
import us.ihmc.simulationconstructionset.robotController.RobotController;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition.GraphicType;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicVector;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

public class SimulatedOutputWriterWithControlModeSelection implements OutputWriter
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);
   private final SDFPerfectSimulatedOutputWriter outputWriter;
   
   private final ArrayList<PDPositionControllerForOneDoFJoint> positionControllers = new ArrayList<>();

   private final SDFRobot sdfRobot;
   private final SDFFullRobotModel sdfFullRobotModel;
   
   private RobotController robotController;

   private final Point3d comPoint = new Point3d();
   private final YoFramePoint actualCenterOfMassPosition = new YoFramePoint("actualCenterOfMass", ReferenceFrame.getWorldFrame(), registry);
   private final QuadrantDependentList<YoGraphicVector> groundForceVectorGraphics = new QuadrantDependentList<>();
   private final QuadrantDependentList<YoFrameVector> groundForceVectors = new QuadrantDependentList<>();
   private final QuadrantDependentList<Vector3d> tempGroundForceVectors = new QuadrantDependentList<>();
   private final QuadrantDependentList<Point3d> tempGroundForceOriginPoints = new QuadrantDependentList<>();
   private final QuadrantDependentList<YoFramePoint> groundForceOriginPoints = new QuadrantDependentList<>();
   private final YoGraphicPosition actualCenterOfMassViz = new YoGraphicPosition("actualCenterOfMass", actualCenterOfMassPosition, 0.04, YoAppearance.DeepPink(), GraphicType.BALL_WITH_CROSS);
   
   private final YoFramePoint cop = new YoFramePoint("cop", ReferenceFrame.getWorldFrame(), registry);
   private final YoGraphicPosition copViz = new YoGraphicPosition("copViz", cop, 0.01, YoAppearance.Red());
   
   public SimulatedOutputWriterWithControlModeSelection(SDFFullQuadrupedRobotModel sdfFullRobotModel, SDFRobot robot, QuadrupedRobotParameters robotParameters, YoVariableRegistry parentRegistry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this.sdfRobot = robot;
      this.sdfFullRobotModel = sdfFullRobotModel;
      this.outputWriter = new SDFPerfectSimulatedOutputWriter(robot, sdfFullRobotModel);
      
      ArrayList<OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints  = new ArrayList<>();
      robot.getAllOneDegreeOfFreedomJoints(oneDegreeOfFreedomJoints);
      
      createPDControllers(sdfFullRobotModel, robotParameters, oneDegreeOfFreedomJoints);
      
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         tempGroundForceVectors.set(robotQuadrant, new Vector3d());
         tempGroundForceOriginPoints.set(robotQuadrant, new Point3d());
         groundForceOriginPoints.set(robotQuadrant, new YoFramePoint(robotQuadrant.getCamelCaseNameForStartOfExpression() + "GroundForceOriginPoint", ReferenceFrame.getWorldFrame(), parentRegistry));
         groundForceVectors.set(robotQuadrant, new YoFrameVector(robotQuadrant.getCamelCaseNameForStartOfExpression() + "GroundForceVector", ReferenceFrame.getWorldFrame(), parentRegistry));
         groundForceVectorGraphics.set(robotQuadrant, new YoGraphicVector(robotQuadrant.getCamelCaseNameForStartOfExpression() + "GroundForceVectorViz", groundForceOriginPoints.get(robotQuadrant),
                                                                          groundForceVectors.get(robotQuadrant), 0.0005, YoAppearance.Crimson(), true, 0.075));
         yoGraphicsListRegistry.registerYoGraphic(robotQuadrant.getCamelCaseNameForStartOfExpression() + "GroundForceVectorGraphic", groundForceVectorGraphics.get(robotQuadrant));
      }
      
      yoGraphicsListRegistry.registerYoGraphic("actualCenterOfMassViz", actualCenterOfMassViz);
      yoGraphicsListRegistry.registerArtifact("centerOfPressure", copViz.createArtifact());
      
      actualCenterOfMassViz.hideGraphicObject();
      parentRegistry.addChild(registry);
   }
   
   public void setHighLevelController(RobotController robotController)
   {
      if(this.robotController != null)
      {
         throw new IllegalArgumentException("Robot controller already registered! Currently, you can only register one");
      }
      this.robotController = robotController;
      registry.addChild(robotController.getYoVariableRegistry());
   }

   @Override
   public void initialize()
   {
      outputWriter.setFullRobotModel(sdfFullRobotModel);
   }
   
   private final Point3d copPoint = new Point3d();
   private final Vector3d copForce = new Vector3d();
   private final Vector3d copMoment = new Vector3d();
   
   @Override
   public void write()
   {
      for(int i = 0; i < positionControllers.size(); i++)
      {
         if(positionControllers.get(i).doPositionControl())
         {
            positionControllers.get(i).update();
         }
      }
      
      outputWriter.write();
      
      sdfRobot.computeCenterOfMass(comPoint);
      sdfRobot.computeCenterOfPressure(copPoint, copForce, copMoment);
      cop.set(copPoint);
      actualCenterOfMassPosition.set(comPoint);
      actualCenterOfMassViz.update();
      
      ArrayList<GroundContactPoint> allGroundContactPoints = sdfRobot.getAllGroundContactPoints();
      
      for (GroundContactPoint groundContactPoint : allGroundContactPoints)
      {
         String groundContactPointName = groundContactPoint.getName();
         
         //TODO: SDF loader should figure out this first and put in an enumMap or something.
         RobotQuadrant robotQuadrant = RobotQuadrant.guessQuadrantFromName(groundContactPointName);
         
         if (robotQuadrant == null) 
         {
            System.err.println("robotQuadrant == null in SimulatedOutputWriterWithControlModeSelection.write()");
            continue;
         }
         
         groundContactPoint.getForce(tempGroundForceVectors.get(robotQuadrant));
         groundForceVectors.get(robotQuadrant).set(tempGroundForceVectors.get(robotQuadrant));
         groundContactPoint.getPosition(tempGroundForceOriginPoints.get(robotQuadrant));
//         groundContactPoint.getTouchdownLocation(tempGroundForceOriginPoints.get(robotQuadrant));
         groundForceOriginPoints.get(robotQuadrant).set(tempGroundForceOriginPoints.get(robotQuadrant));
      }
   }
   
   private void createPDControllers(SDFFullQuadrupedRobotModel sdfFullRobotModel, QuadrupedRobotParameters robotParameters, ArrayList<OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints)
   {
      QuadrupedActuatorParameters actuatorParameters = robotParameters.getActuatorParameters();
      for(OneDegreeOfFreedomJoint simulatedJoint : oneDegreeOfFreedomJoints)
      {
         String jointName = simulatedJoint.getName();
         OneDoFJoint oneDoFJoint = sdfFullRobotModel.getOneDoFJointByName(jointName);
         double kp = actuatorParameters.getLegKp();
         double kd = actuatorParameters.getLegKd();
         double maxTorque = actuatorParameters.getLegSoftTorqueLimit();

         if(sdfFullRobotModel.getNameForOneDoFJoint(oneDoFJoint).getRole() == JointRole.NECK)
         {
            kp = actuatorParameters.getNeckKp();
            kd = actuatorParameters.getNeckKd();
            maxTorque = actuatorParameters.getNeckSoftTorqueLimit();
         }
         
         positionControllers.add(new PDPositionControllerForOneDoFJoint(oneDoFJoint, kp, kd, maxTorque));
      }
   }
   
   public class PDPositionControllerForOneDoFJoint
   {
      private final PDController pdController;
      private final YoVariableRegistry pidRegistry;
      private final OneDoFJoint oneDofJoint;
      private final DoubleYoVariable q_d, q_d_notCapped, tau_d, tau_d_notCapped, maxTorque;
      private final BooleanYoVariable hitJointLimit, hitTorqueLimit;
      
      public PDPositionControllerForOneDoFJoint(OneDoFJoint oneDofJoint, double kp, double kd, double torqueLimit)
      {
         String name = "pdController_" + oneDofJoint.getName();
         pidRegistry = new YoVariableRegistry(name);
         q_d = new DoubleYoVariable(name + "_q_d", pidRegistry);
         q_d_notCapped = new DoubleYoVariable(name + "_q_d_notCapped", pidRegistry);
         tau_d = new DoubleYoVariable(name + "_tau_d", pidRegistry);
         tau_d_notCapped = new DoubleYoVariable(name + "_tau_d_notCapped", pidRegistry);
         maxTorque = new DoubleYoVariable(name + "_tau_max", pidRegistry);
         maxTorque.set(torqueLimit);
         hitJointLimit = new BooleanYoVariable(name + "_hitJointLimit", pidRegistry);
         hitTorqueLimit = new BooleanYoVariable(name + "_hitTorqueLimit", pidRegistry);
         
         pdController = new PDController(oneDofJoint.getName(), pidRegistry);
         pdController.setProportionalGain(kp);
         pdController.setDerivativeGain(kd);
         registry.addChild(pidRegistry);
         
         this.oneDofJoint = oneDofJoint;
      }
      
      public void update()
      {
         double currentPosition = oneDofJoint.getQ();
         double desiredPosition = oneDofJoint.getqDesired();
         
         q_d_notCapped.set(desiredPosition);
         double desiredPositionClipped = MathTools.clipToMinMax(desiredPosition, oneDofJoint.getJointLimitLower(), oneDofJoint.getJointLimitUpper());
         boolean insidePosiotionLimits = MathTools.isInsideBoundsInclusive(desiredPosition, oneDofJoint.getJointLimitLower(), oneDofJoint.getJointLimitUpper());
         hitJointLimit.set(!insidePosiotionLimits);
         q_d.set(desiredPositionClipped);
         
         double currentRate = oneDofJoint.getQd();
         double desiredRate = oneDofJoint.getQdDesired();
         double desiredTau = pdController.compute(currentPosition, desiredPositionClipped, currentRate, desiredRate);
         
         tau_d_notCapped.set(desiredTau);
         boolean insideTauLimits = MathTools.isInsideBoundsInclusive(desiredTau, -maxTorque.getDoubleValue(), maxTorque.getDoubleValue());
         hitTorqueLimit.set(!insideTauLimits);
         desiredTau = MathTools.clipToMinMax(desiredTau, maxTorque.getDoubleValue());
         
         tau_d.set(desiredTau);
         oneDofJoint.setTau(desiredTau);
      }
      
      public boolean doPositionControl()
      {
         return oneDofJoint.isUnderPositionControl();
      }
   }

   @Override
   public void setFullRobotModel(FullRobotModel fullRobotModel)
   {
      outputWriter.setFullRobotModel(sdfFullRobotModel);
   }
}
