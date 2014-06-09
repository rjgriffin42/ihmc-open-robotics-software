package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories;

import java.util.List;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point2d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ListOfPointsContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;

public class ContactableBodiesFactory
{
   private SideDependentList<String> namesOfJointsBeforeHands = null;
   private SideDependentList<List<Point2d>> handContactPoints = null;
   private SideDependentList<Transform3D> handContactPointTransforms = null;

   private SideDependentList<String> namesOfJointsBeforeThighs = null;
   private SideDependentList<List<Point2d>> thighContactPoints = null;
   private SideDependentList<Transform3D> thighContactPointTransforms = null;

   private List<Point2d> pelvisContactPoints = null;
   private Transform3D pelvisContactPointTransform = null;

   private List<Point2d> pelvisBackContactPoints = null;
   private Transform3D pelvisBackContactPointTransform = null;

   private List<Point2d> chestBackContactPoints = null;
   private Transform3D chestBackContactPointTransform = null;

   private SideDependentList<? extends List<Point2d>> footContactPoints = null;

   public void addPelvisContactParameters(List<Point2d> pelvisContactPoints, Transform3D pelvisContactPointTransform)
   {
      this.pelvisContactPointTransform = pelvisContactPointTransform;
      this.pelvisContactPoints = pelvisContactPoints;
   }

   public void addPelvisBackContactParameters(List<Point2d> pelvisBackContactPoints, Transform3D pelvisBackContactPointTransform)
   {
      this.pelvisBackContactPoints = pelvisBackContactPoints;
      this.pelvisBackContactPointTransform = pelvisBackContactPointTransform;
   }

   public void addChestBackContactParameters(List<Point2d> chestBackContactPoints, Transform3D chestBackContactPointTransform)
   {
      this.chestBackContactPoints = chestBackContactPoints;
      this.chestBackContactPointTransform = chestBackContactPointTransform;
   }
   
   public void addHandContactParameters(SideDependentList<String> namesOfJointsBeforeHands, SideDependentList<List<Point2d>> handContactPoints,
         SideDependentList<Transform3D> handContactPointTransforms)
   {
      this.namesOfJointsBeforeHands = namesOfJointsBeforeHands;
      this.handContactPoints = handContactPoints;
      this.handContactPointTransforms = handContactPointTransforms;
   }

   public void addThighContactParameters(SideDependentList<String> namesOfJointsBeforeThighs, SideDependentList<List<Point2d>> thighContactPoints,
         SideDependentList<Transform3D> thighContactPointTransforms)
   {
      this.namesOfJointsBeforeThighs = namesOfJointsBeforeThighs;
      this.thighContactPoints = thighContactPoints;
      this.thighContactPointTransforms = thighContactPointTransforms;
   }

   public void addFootContactParameters(SideDependentList<? extends List<Point2d>> footContactPoints)
   {
      this.footContactPoints = footContactPoints;
   }

   public ContactablePlaneBody createPelvisContactableBody(RigidBody pelvis)
   {
      if (pelvisContactPointTransform == null)
         return null;

      return createListOfPointsContactablePlaneBody("PelvisContact", pelvis, pelvisContactPointTransform, pelvisContactPoints);
   }

   public ContactablePlaneBody createPelvisBackContactableBody(RigidBody pelvis)
   {
      if (pelvisBackContactPointTransform == null)
         return null;

      return createListOfPointsContactablePlaneBody("PelvisBackContact", pelvis, pelvisBackContactPointTransform, pelvisBackContactPoints);
   }

   public ContactablePlaneBody createChestBackContactableBody(RigidBody chest)
   {
      if (chestBackContactPointTransform == null)
         return null;

      return createListOfPointsContactablePlaneBody("ChestBackContact", chest, chestBackContactPointTransform, chestBackContactPoints);
   }

   public SideDependentList<ContactablePlaneBody> createHandContactableBodies(RigidBody rootBody)
   {
      if (namesOfJointsBeforeHands == null)
         return null;

      InverseDynamicsJoint[] allJoints = ScrewTools.computeSupportAndSubtreeJoints(rootBody);

      SideDependentList<ContactablePlaneBody> handContactableBodies = new SideDependentList<>();

      for (RobotSide robotSide : RobotSide.values)
      {
         InverseDynamicsJoint[] jointBeforeHandArray = ScrewTools.findJointsWithNames(allJoints, namesOfJointsBeforeHands.get(robotSide));
         if (jointBeforeHandArray.length != 1)
            throw new RuntimeException("Incorrect number of joints before hand found: " + jointBeforeHandArray.length);

         RigidBody hand = jointBeforeHandArray[0].getSuccessor();
         String name = robotSide.getCamelCaseNameForStartOfExpression() + "HandContact";
         ListOfPointsContactablePlaneBody handContactableBody = createListOfPointsContactablePlaneBody(name, hand,
               handContactPointTransforms.get(robotSide), handContactPoints.get(robotSide));
         handContactableBodies.put(robotSide, handContactableBody);
      }
      return handContactableBodies;
   }

   public SideDependentList<ContactablePlaneBody> createThighContactableBodies(RigidBody rootBody)
   {
      if (namesOfJointsBeforeThighs == null)
         return null;

      InverseDynamicsJoint[] allJoints = ScrewTools.computeSupportAndSubtreeJoints(rootBody);

      SideDependentList<ContactablePlaneBody> thighContactableBodies = new SideDependentList<>();

      for (RobotSide robotSide : RobotSide.values)
      {
         InverseDynamicsJoint[] jointBeforeThighArray = ScrewTools.findJointsWithNames(allJoints, namesOfJointsBeforeThighs.get(robotSide));
         if (jointBeforeThighArray.length != 1)
            throw new RuntimeException("Incorrect number of joints before thigh found: " + jointBeforeThighArray.length);

         RigidBody thigh = jointBeforeThighArray[0].getSuccessor();
         String name = robotSide.getCamelCaseNameForStartOfExpression() + "ThighContact";
         ListOfPointsContactablePlaneBody thighContactableBody = createListOfPointsContactablePlaneBody(name, thigh,
               thighContactPointTransforms.get(robotSide), thighContactPoints.get(robotSide));
         thighContactableBodies.put(robotSide, thighContactableBody);
      }
      return thighContactableBodies;
   }

   public SideDependentList<ContactablePlaneBody> createFootContactableBodies(FullRobotModel fullRobotModel, CommonWalkingReferenceFrames referenceFrames)
   {
      if (footContactPoints == null)
         return null;

      SideDependentList<ContactablePlaneBody> footContactableBodies = new SideDependentList<>();

      for (RobotSide robotSide : RobotSide.values)
      {
         RigidBody foot = fullRobotModel.getFoot(robotSide);
         ListOfPointsContactablePlaneBody footContactableBody = new ListOfPointsContactablePlaneBody(foot, referenceFrames.getSoleFrame(robotSide),
               footContactPoints.get(robotSide));
         footContactableBodies.put(robotSide, footContactableBody);
      }
      return footContactableBodies;
   }

   private final static ListOfPointsContactablePlaneBody createListOfPointsContactablePlaneBody(String name, RigidBody body,
         Transform3D contactPointsTransform, List<Point2d> contactPoints)
   {
      ReferenceFrame parentFrame = body.getParentJoint().getFrameAfterJoint();
      ReferenceFrame contactPointsFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent(name, parentFrame, contactPointsTransform);
      ListOfPointsContactablePlaneBody ret = new ListOfPointsContactablePlaneBody(body, contactPointsFrame, contactPoints);
      return ret;
   }
}
