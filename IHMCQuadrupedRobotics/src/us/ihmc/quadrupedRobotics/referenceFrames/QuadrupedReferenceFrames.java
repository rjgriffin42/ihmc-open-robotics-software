package us.ihmc.quadrupedRobotics.referenceFrames;

import java.util.EnumMap;

import javax.vecmath.Quat4d;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.SdfLoader.models.FullRobotModel;
import us.ihmc.SdfLoader.partNames.LegJointName;
import us.ihmc.SdfLoader.partNames.NeckJointName;
import us.ihmc.SdfLoader.partNames.RobotSpecificJointNames;
import us.ihmc.quadrupedRobotics.parameters.QuadrupedJointNameMap;
import us.ihmc.quadrupedRobotics.parameters.QuadrupedPhysicalProperties;
import us.ihmc.quadrupedRobotics.supportPolygon.QuadrupedSupportPolygon;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.referenceFrames.CenterOfMassReferenceFrame;
import us.ihmc.robotics.referenceFrames.MidFrameZUpFrame;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.referenceFrames.TranslationReferenceFrame;
import us.ihmc.robotics.referenceFrames.ZUpFrame;
import us.ihmc.robotics.robotSide.EndDependentList;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotEnd;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.tools.containers.ContainerTools;

public class QuadrupedReferenceFrames extends CommonQuadrupedReferenceFrames
{
   private final FullRobotModel fullRobotModel;

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final Quat4d IDENTITY_QUATERNION = new Quat4d();

   private final ReferenceFrame bodyFrame, rootJointFrame;
   private final ZUpFrame bodyZUpFrame;

   private final EnumMap<NeckJointName, ReferenceFrame> neckReferenceFrames = ContainerTools.createEnumMap(NeckJointName.class);
   private final QuadrantDependentList<EnumMap<LegJointName, ReferenceFrame>> framesBeforeLegJoint = QuadrantDependentList.createListOfEnumMaps(LegJointName.class);
   private final QuadrantDependentList<EnumMap<LegJointName, ReferenceFrame>> framesAfterLegJoint = QuadrantDependentList.createListOfEnumMaps(LegJointName.class);
   private final QuadrantDependentList<ReferenceFrame> soleFrames = new QuadrantDependentList<ReferenceFrame>();
   
   private final QuadrantDependentList<PoseReferenceFrame> tripleSupportFrames = new QuadrantDependentList<PoseReferenceFrame>();
   private final QuadrantDependentList<FramePose> tripleSupportCentroidPoses = new QuadrantDependentList<FramePose>();
   private final QuadrantDependentList<ZUpFrame> tripleSupportZupFrames = new QuadrantDependentList<ZUpFrame>();
   
   private final FramePose supportPolygonCentroidWithNominalRotation = new FramePose(ReferenceFrame.getWorldFrame());
   private final PoseReferenceFrame supportPolygonCentroidFrameWithNominalRotation;
   private final ZUpFrame supportPolygonCentroidZUpFrame;
   
   private final QuadrantDependentList<ReferenceFrame> legAttachementFrames = new QuadrantDependentList<ReferenceFrame>();
   private final QuadrantDependentList<FramePoint> legAttachementPoints= new QuadrantDependentList<FramePoint>();

   private final SideDependentList<ReferenceFrame> sideDependentMidTrotLineZUpFrames = new SideDependentList<ReferenceFrame>();
   private final SideDependentList<ReferenceFrame> sideDependentMidFeetZUpFrames = new SideDependentList<ReferenceFrame>();
   private final EndDependentList<ReferenceFrame> endDependentMidFeetZUpFrames = new EndDependentList<ReferenceFrame>();
   
   private final ReferenceFrame centerOfMassFrame;
   private final PoseReferenceFrame centerOfMassFrameWithRotation;
   private final ZUpFrame centerOfMassZUpFrame;
   private final FramePose centerOfMassPose;
   private final PoseReferenceFrame centerOfFourHipsFrame;
   private final FramePose centerOfFourHipsFramePose;
   private final FramePoint centerOfFourHipsFramePoint = new FramePoint();
   
   private final PoseReferenceFrame centerOfFourFeetFrameWithBodyRotation;
   private final FramePose centerOfFourFeetFramePose;
   private final FramePoint centerOfFourFeetFramePoint = new FramePoint();
   
   private QuadrupedSupportPolygon supportPolygonForCentroids = new QuadrupedSupportPolygon();

   public QuadrupedReferenceFrames(SDFFullRobotModel fullRobotModel, QuadrupedJointNameMap quadrupedJointNameMap, QuadrupedPhysicalProperties quadrupedPhysicalProperties)
   {
      this.fullRobotModel = fullRobotModel;

      rootJointFrame = fullRobotModel.getRootJoint().getFrameAfterJoint();
      bodyFrame = fullRobotModel.getRootJoint().getSuccessor().getBodyFixedFrame();
      centerOfMassPose = new FramePose(bodyFrame);

      bodyZUpFrame = new ZUpFrame(worldFrame, bodyFrame, "bodyZUpFrame");
      RobotSpecificJointNames robotJointNames = fullRobotModel.getRobotSpecificJointNames();
      
      for (NeckJointName neckJointName : robotJointNames.getNeckJointNames())
      {
         this.neckReferenceFrames.put(neckJointName, fullRobotModel.getNeckJoint(neckJointName).getFrameAfterJoint());
      }

      LegJointName[] legJointNames = quadrupedJointNameMap.getLegJointNames();
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {         
         for (LegJointName legJointName : legJointNames)
         {
            String jointName = quadrupedJointNameMap.getLegJointName(robotQuadrant, legJointName);
            OneDoFJoint oneDoFJoint = fullRobotModel.getOneDoFJointByName(jointName);
            ReferenceFrame frameBeforeJoint = oneDoFJoint.getFrameBeforeJoint();
            ReferenceFrame frameAfterJoint = oneDoFJoint.getFrameAfterJoint();
            
            framesBeforeLegJoint.get(robotQuadrant).put(legJointName, frameBeforeJoint);
            framesAfterLegJoint.get(robotQuadrant).put(legJointName, frameAfterJoint);
         }
         
         ReferenceFrame frameAfterKnee = framesAfterLegJoint.get(robotQuadrant).get(LegJointName.KNEE);

         TranslationReferenceFrame soleFrame = new TranslationReferenceFrame(robotQuadrant.toString() + "SoleFrame", frameAfterKnee);
         soleFrame.updateTranslation(quadrupedPhysicalProperties.getOffsetFromKneeToFoot(robotQuadrant));
         soleFrame.update();
         
         soleFrames.set(robotQuadrant, soleFrame);
         
         FramePoint legAttachmentPoint = new FramePoint();
         legAttachementPoints.set(robotQuadrant, legAttachmentPoint);
      }
      
      for (RobotSide robotSide : RobotSide.values)
      {
         RobotQuadrant hindSoleQuadrant = RobotQuadrant.getQuadrant(RobotEnd.HIND, robotSide);
         RobotQuadrant frontSoleQuadrant = RobotQuadrant.getQuadrant(RobotEnd.FRONT, robotSide);
         RobotQuadrant frontSoleQuadrantOppositeSide = RobotQuadrant.getQuadrant(RobotEnd.FRONT, robotSide.getOppositeSide());
         
         MidFrameZUpFrame midFeetZUpFrame = new MidFrameZUpFrame(robotSide.getCamelCaseNameForStartOfExpression() + "MidFeetZUpFrame", worldFrame, soleFrames.get(hindSoleQuadrant), soleFrames.get(frontSoleQuadrant));
         sideDependentMidFeetZUpFrames.put(robotSide, midFeetZUpFrame);
         
         MidFrameZUpFrame midTrotLineZUpFrame = new MidFrameZUpFrame("hind" + robotSide.getCamelCaseNameForMiddleOfExpression() + "Front" + robotSide.getOppositeSide().getCamelCaseNameForMiddleOfExpression() + "MidTrotLineZUpFrame", worldFrame, soleFrames.get(hindSoleQuadrant), soleFrames.get(frontSoleQuadrantOppositeSide));
         sideDependentMidTrotLineZUpFrames.put(robotSide, midTrotLineZUpFrame);
      }
      
      for (RobotEnd robotEnd : RobotEnd.values)
      {
         RobotQuadrant leftSoleQuadrant = RobotQuadrant.getQuadrant(robotEnd, RobotSide.LEFT);
         RobotQuadrant rightSoleQuadrant = RobotQuadrant.getQuadrant(robotEnd, RobotSide.RIGHT);
         
         MidFrameZUpFrame midFeetZUpFrame = new MidFrameZUpFrame(robotEnd.getCamelCaseNameForStartOfExpression() + "MidFeetZUpFrame", worldFrame, soleFrames.get(leftSoleQuadrant), soleFrames.get(rightSoleQuadrant));
         endDependentMidFeetZUpFrames.put(robotEnd, midFeetZUpFrame);
      }
             
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      { 
         ReferenceFrame frameBeforeHipRoll = framesBeforeLegJoint.get(robotQuadrant).get(LegJointName.HIP_ROLL);         
         ReferenceFrame frameBeforeHipPitch = framesBeforeLegJoint.get(robotQuadrant).get(LegJointName.HIP_PITCH);
         
         FramePoint xyOffsetFromRollToPitch = new FramePoint(frameBeforeHipPitch);
         xyOffsetFromRollToPitch.changeFrame(frameBeforeHipRoll);
         
         TranslationReferenceFrame legAttachmentFrame = new TranslationReferenceFrame(robotQuadrant.getCamelCaseNameForStartOfExpression() + "LegAttachementFrame", frameBeforeHipRoll);

         xyOffsetFromRollToPitch.setZ(0.0);
         
         legAttachmentFrame.updateTranslation(xyOffsetFromRollToPitch);
         legAttachementFrames.set(robotQuadrant, legAttachmentFrame);
         
         
         FramePose tripleSupportCentroidPose = new FramePose(worldFrame);
         PoseReferenceFrame tripleSupport = new PoseReferenceFrame(robotQuadrant.getCamelCaseNameForStartOfExpression() + "TripleSupportFrame", tripleSupportCentroidPose);
         ZUpFrame tripleSupportZUpFrame = new ZUpFrame(worldFrame, tripleSupport, robotQuadrant.getCamelCaseNameForStartOfExpression() + "TripleSupportZupFrame");

         tripleSupportCentroidPoses.set(robotQuadrant, tripleSupportCentroidPose);
         tripleSupportZupFrames.set(robotQuadrant, tripleSupportZUpFrame);
         tripleSupportFrames.set(robotQuadrant, tripleSupport);
      }
      
      centerOfMassFrame = new CenterOfMassReferenceFrame("centerOfMass", worldFrame, fullRobotModel.getElevator());
      
      centerOfMassPose.setToZero(centerOfMassFrame);
      centerOfMassPose.changeFrame(bodyFrame);
      
      centerOfMassFrameWithRotation = new PoseReferenceFrame("centerOfMassFrameWithRotation", bodyFrame);
      centerOfMassFrameWithRotation.setPoseAndUpdate(centerOfMassPose);
      centerOfMassZUpFrame = new ZUpFrame(worldFrame, centerOfMassFrameWithRotation, "centerOfMassZUpFrame");
      
      centerOfFourHipsFramePose = new FramePose(bodyFrame);
      centerOfFourHipsFrame = new PoseReferenceFrame("centerOfFourHipsFrame", bodyFrame);
      
      centerOfFourFeetFramePose = new FramePose(bodyFrame);
      centerOfFourFeetFrameWithBodyRotation = new PoseReferenceFrame("centerOfFourFeetFrame", bodyFrame);
      supportPolygonCentroidFrameWithNominalRotation = new PoseReferenceFrame("centerOfFourFeetWithSupportPolygonRotation", supportPolygonCentroidWithNominalRotation);
      supportPolygonCentroidZUpFrame = new ZUpFrame(worldFrame, supportPolygonCentroidFrameWithNominalRotation, "centerFootPolygonZUp");
      
      updateHipsCentroid();
      
      initializeCommonValues();
   }

   private void updateHipsCentroid()
   {
      centerOfFourHipsFramePose.setToZero(bodyFrame);
      centerOfFourHipsFramePoint.setToZero(bodyFrame);
      
      for(RobotQuadrant quadrant : RobotQuadrant.values)
      {
         FramePoint legAttachmentPoint = legAttachementPoints.get(quadrant);
         legAttachmentPoint.setToZero(legAttachementFrames.get(quadrant));
         legAttachmentPoint.changeFrame(bodyFrame);
         centerOfFourHipsFramePoint.add(legAttachmentPoint);
         
      }
      centerOfFourHipsFramePoint.scale(0.25);
      centerOfFourHipsFramePose.setPosition(centerOfFourHipsFramePoint);
      centerOfFourHipsFrame.setPoseAndUpdate(centerOfFourHipsFramePose);
   }
   
   public void updateFrames()
   {
      fullRobotModel.updateFrames();
      bodyZUpFrame.update();
      
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         soleFrames.get(robotQuadrant).update();
      }
      
      for (RobotSide robotSide : RobotSide.values)
      {
         sideDependentMidFeetZUpFrames.get(robotSide).update();
         sideDependentMidTrotLineZUpFrames.get(robotSide).update();
      }
      
      for (RobotEnd robotEnd : RobotEnd.values)
      {
         endDependentMidFeetZUpFrames.get(robotEnd).update();
      }

      centerOfMassFrame.update();
      
      centerOfMassPose.setToZero(centerOfMassFrame);
      centerOfMassPose.changeFrame(bodyFrame);
      centerOfMassPose.setOrientation(IDENTITY_QUATERNION);
      
      centerOfMassFrameWithRotation.setPoseAndUpdate(centerOfMassPose);
      centerOfMassZUpFrame.update();
      
      updateCentroids();
   }

   private void updateCentroids()
   {
      updateHipsCentroid();
      updateCenterOfFeetUsingBodyForRotationPart();
      updateTripleSupportCentroids();
      updateCenterOfFeetUsingNominalsForRotationPart();
   }
   
   private void updateCenterOfFeetUsingBodyForRotationPart()
   {
      updateSupportPolygon(null,supportPolygonForCentroids);
      
      centerOfFourFeetFramePoint.changeFrame(worldFrame);
      supportPolygonForCentroids.getCentroid(centerOfFourFeetFramePoint);
      
      centerOfFourFeetFramePoint.changeFrame(bodyFrame);
      centerOfFourFeetFramePose.setToZero(bodyFrame);
      
      centerOfFourFeetFramePose.setPosition(centerOfFourFeetFramePoint);
      centerOfFourFeetFrameWithBodyRotation.setPoseAndUpdate(centerOfFourFeetFramePose);
   }
   
   private void updateTripleSupportCentroids()
   {
      for(RobotQuadrant swingLeg : RobotQuadrant.values)
      {
         updateSupportPolygon(swingLeg, supportPolygonForCentroids);
         FramePose framePose = tripleSupportCentroidPoses.get(swingLeg);
         supportPolygonForCentroids.getCentroidFramePoseAveragingLowestZHeightsAcrossEnds(framePose);
         
         PoseReferenceFrame tripleSupportFrame = tripleSupportFrames.get(swingLeg);
         tripleSupportFrame.setPoseAndUpdate(framePose);
         
         ZUpFrame tripleSupportZUpFrame = tripleSupportZupFrames.get(swingLeg);
         tripleSupportZUpFrame.update();
      }
   }
   
   private void updateCenterOfFeetUsingNominalsForRotationPart()
   {
      updateSupportPolygon(null, supportPolygonForCentroids);
      supportPolygonForCentroids.getCentroidFramePoseAveragingLowestZHeightsAcrossEnds(supportPolygonCentroidWithNominalRotation);
      supportPolygonCentroidFrameWithNominalRotation.setPoseAndUpdate(supportPolygonCentroidWithNominalRotation);
      supportPolygonCentroidZUpFrame.update();
   }
   
   private final FramePoint soleFramePointTemp = new FramePoint();
   private void updateSupportPolygon(RobotQuadrant swingFoot, QuadrupedSupportPolygon supportPolygon)
   {
      supportPolygon.clear();
      for(RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         if(robotQuadrant.equals(swingFoot))
         {
            continue;
         }
         ReferenceFrame soleFrame = soleFrames.get(robotQuadrant);
         soleFramePointTemp.setToZero(soleFrame);
         soleFramePointTemp.changeFrame(worldFrame);
         supportPolygon.setFootstep(robotQuadrant, soleFramePointTemp);
      }
   }
   
   public static ReferenceFrame getWorldFrame()
   {
      return worldFrame;
   }

   @Override
   public ReferenceFrame getBodyFrame()
   {
      return bodyFrame;
   }
   
   @Override
   public ReferenceFrame getBodyZUpFrame()
   {
      return bodyZUpFrame;
   }

   @Override
   public ReferenceFrame getSideDependentMidFeetZUpFrame(RobotSide robotSide)
   {
      return sideDependentMidFeetZUpFrames.get(robotSide);
   }

   @Override
   public ReferenceFrame getMidTrotLineZUpFrame(RobotQuadrant quadrantAssocaitedWithTrotLine)
   {
      if(quadrantAssocaitedWithTrotLine.isQuadrantInHind())
      {
         return sideDependentMidTrotLineZUpFrames.get(quadrantAssocaitedWithTrotLine.getSide());
      }
      return sideDependentMidTrotLineZUpFrames.get(quadrantAssocaitedWithTrotLine.getOppositeSide());
   }
   
   @Override
   public ReferenceFrame getEndDependentMidFeetZUpFrame(RobotEnd robotEnd)
   {
      return endDependentMidFeetZUpFrames.get(robotEnd);
   }
   
   @Override
   public ReferenceFrame getRootJointFrame()
   {
      return rootJointFrame;
   }
   
   public ReferenceFrame getNeckFrame(NeckJointName neckJointName)
   {
      return neckReferenceFrames.get(neckJointName);
   }

   @Override
   public ReferenceFrame getFrameBeforeLegJoint(RobotQuadrant robotQuadrant, LegJointName legJointName)
   {
      return framesBeforeLegJoint.get(robotQuadrant).get(legJointName);
   }
   
   public ReferenceFrame getLegJointFrame(RobotQuadrant robotQuadrant, LegJointName legJointName)
   {
      return framesAfterLegJoint.get(robotQuadrant).get(legJointName);
   }
   
   @Override
   public ReferenceFrame getLegAttachmentFrame(RobotQuadrant robotQuadrant)
   {
      return legAttachementFrames.get(robotQuadrant);
   }
   
   @Override
   public ReferenceFrame getHipRollFrame(RobotQuadrant robotQuadrant)
   {
      return getLegJointFrame(robotQuadrant, LegJointName.HIP_ROLL);
   }

   @Override
   public ReferenceFrame getHipPitchFrame(RobotQuadrant robotQuadrant)
   {
      return getLegJointFrame(robotQuadrant, LegJointName.HIP_PITCH);
   }

   @Override
   public ReferenceFrame getKneeFrame(RobotQuadrant robotQuadrant)
   {
      return getLegJointFrame(robotQuadrant, LegJointName.KNEE);
   }
   
   @Override
   public ReferenceFrame getFootFrame(RobotQuadrant robotQuadrant)
   {
      return soleFrames.get(robotQuadrant);
   }
   
   @Override
   public ReferenceFrame getCenterOfMassFrame()
   {
      //return centerOfMassFrame;
      return centerOfMassFrameWithRotation;
   }

   @Override
   public ReferenceFrame getCenterOfMassZUpFrame()
   {
      return centerOfMassZUpFrame;
   }

   @Override
   public QuadrantDependentList<ReferenceFrame> getFootReferenceFrames()
   {
      return soleFrames;
   }

   @Override
   public ReferenceFrame getCenterOfFourHipsFrame()
   {
      return centerOfFourHipsFrame;
   }

   public ReferenceFrame getCenterOfFourFeetFrame()
   {
      return centerOfFourFeetFrameWithBodyRotation;
   }
   
   /**
    * returns the center of the support polygon excluding the specified leg
    * averaging the lowest front and the lowest hind Z values, 
    * and using the nominal yaw, pitch, and roll
    * @param feetQuadrants, feet 
    */
   @Override
   public ReferenceFrame getTripleSupportFrameAveragingLowestZHeightsAcrossEnds(RobotQuadrant footToExclude)
   {
      return tripleSupportFrames.get(footToExclude);
   }

   /**
    * returns the center of the polygon made up using the provided robot quadrants, 
    * averaging the lowest front and the lowest hind Z values, 
    * and using the nominal yaw
    */
   @Override
   public ReferenceFrame getZUpTripleSupportFrameAveragingLowestZHeightsAcrossEnds(RobotQuadrant footToExclude)
   {
      return tripleSupportZupFrames.get(footToExclude);
   }

   /**
    * returns the center of the polygon made up using the four feet, 
    * averaging the lowest front and the lowest hind Z values, 
    * and using the nominal yaw, pitch, and roll
    */
   @Override
   public ReferenceFrame getCenterOfFeetFrameAveragingLowestZHeightsAcrossEnds()
   {
      return supportPolygonCentroidFrameWithNominalRotation;
   }

   /**
    * returns the center of the four foot polygon, 
    * averaging the lowest front and the lowest hind Z values, 
    * and using the nominal yaw
    */
   @Override
   public ReferenceFrame getCenterOfFeetZUpFrameAveragingLowestZHeightsAcrossEnds()
   {
      return supportPolygonCentroidZUpFrame;
   }
}
