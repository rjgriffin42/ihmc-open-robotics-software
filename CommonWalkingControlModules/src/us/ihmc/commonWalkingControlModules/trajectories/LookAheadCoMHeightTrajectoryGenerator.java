package us.ihmc.commonWalkingControlModules.trajectories;

import java.util.List;

import javax.vecmath.Point2d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.controlModules.WalkOnTheEdgesManager;
import us.ihmc.commonWalkingControlModules.desiredFootStep.TransferToAndNextFootstepsData;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredComHeightProvider;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.Line2d;
import us.ihmc.utilities.math.geometry.LineSegment2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.stringStretcher2d.StringStretcher2d;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;
import us.ihmc.yoUtilities.graphics.BagOfBalls;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.yoUtilities.humanoidRobot.footstep.Footstep;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.trajectories.CubicPolynomialTrajectoryGenerator;
import us.ihmc.yoUtilities.math.trajectories.providers.YoVariableDoubleProvider;


public class LookAheadCoMHeightTrajectoryGenerator implements CoMHeightTrajectoryGenerator
{
   private boolean VISUALIZE = true;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final FourPointSpline1D spline = new FourPointSpline1D(registry);

   private final DesiredComHeightProvider desiredComHeightProvider;

   private final BooleanYoVariable hasBeenInitializedWithNextStep = new BooleanYoVariable("hasBeenInitializedWithNextStep", registry);

   private final DoubleYoVariable offsetHeightAboveGround = new DoubleYoVariable("offsetHeightAboveGround", registry);

   private final DoubleYoVariable offsetHeightAboveGroundPrevValue = new DoubleYoVariable("offsetHeightAboveGroundPrevValue", registry);
   private final DoubleYoVariable offsetHeightAboveGroundChangedTime = new DoubleYoVariable("offsetHeightAboveGroundChangedTime", registry);
   private final YoVariableDoubleProvider offsetHeightAboveGroundInitialPositionProvider = new YoVariableDoubleProvider(offsetHeightAboveGroundPrevValue);
   private final YoVariableDoubleProvider offsetHeightAboveGroundFinalPositionProvider = new YoVariableDoubleProvider(offsetHeightAboveGround);
   private final YoVariableDoubleProvider offsetHeightAboveGroundTrajectoryOutput = new YoVariableDoubleProvider("offsetHeightAboveGroundTrajectoryOutput",
         registry);
   private final YoVariableDoubleProvider offsetHeightAboveGroundTrajectoryTimeProvider = new YoVariableDoubleProvider(
         "offsetHeightAboveGroundTrajectoryTimeProvider", registry);
   private final CubicPolynomialTrajectoryGenerator offsetHeightAboveGroundTrajectory = new CubicPolynomialTrajectoryGenerator(
         "offsetHeightAboveGroundTrajectory", offsetHeightAboveGroundInitialPositionProvider, offsetHeightAboveGroundFinalPositionProvider,
         offsetHeightAboveGroundTrajectoryTimeProvider, registry);

   private final DoubleYoVariable minimumHeightAboveGround = new DoubleYoVariable("minimumHeightAboveGround", registry);
   private final DoubleYoVariable nominalHeightAboveGround = new DoubleYoVariable("nominalHeightAboveGround", registry);
   private final DoubleYoVariable maximumHeightAboveGround = new DoubleYoVariable("maximumHeightAboveGround", registry);

   private final DoubleYoVariable maximumHeightDeltaBetweenWaypoints = new DoubleYoVariable("maximumHeightDeltaBetweenWaypoints", registry);
   private final DoubleYoVariable doubleSupportPercentageIn = new DoubleYoVariable("doubleSupportPercentageIn", registry);

   private final DoubleYoVariable previousZFinal = new DoubleYoVariable("previousZFinal", registry);
   private final DoubleYoVariable desiredCoMHeight = new DoubleYoVariable("desiredCoMHeight", registry);
   private final YoFramePoint desiredCoMPosition = new YoFramePoint("desiredCoMPosition", ReferenceFrame.getWorldFrame(), registry);

   private LineSegment2d projectionSegment;

   private final YoFramePoint contactFrameZeroPosition = new YoFramePoint("contactFrameZeroPosition", worldFrame, registry);
   private final YoFramePoint contactFrameOnePosition = new YoFramePoint("contactFrameOnePosition", worldFrame, registry);

   private final YoGraphicPosition pointS0Viz, pointSFViz, pointD0Viz, pointDFViz, pointSNextViz;
   private final YoGraphicPosition pointS0MinViz, pointSFMinViz, pointD0MinViz, pointDFMinViz, pointSNextMinViz;
   private final YoGraphicPosition pointS0MaxViz, pointSFMaxViz, pointD0MaxViz, pointDFMaxViz, pointSNextMaxViz;

   private final BooleanYoVariable correctForCoMHeightDrift = new BooleanYoVariable("correctForCoMHeightDrift", registry);

   private final BagOfBalls bagOfBalls;

   private WalkOnTheEdgesManager walkOnTheEdgesManager;
   private double extraCoMMaxHeightWithToes = 0.0;

   private final DoubleYoVariable yoTime;

   public LookAheadCoMHeightTrajectoryGenerator(DesiredComHeightProvider desiredComHeightProvider, double minimumHeightAboveGround,
         double nominalHeightAboveGround, double maximumHeightAboveGround, double doubleSupportPercentageIn, final DoubleYoVariable yoTime,
         YoGraphicsListRegistry yoGraphicsListRegistry, YoVariableRegistry parentRegistry)
   {
      this.desiredComHeightProvider = desiredComHeightProvider;

      this.yoTime = yoTime;
      offsetHeightAboveGroundChangedTime.set(yoTime.getDoubleValue());
      offsetHeightAboveGroundTrajectoryTimeProvider.set(0.5);
      offsetHeightAboveGround.set(0.0);
      offsetHeightAboveGroundPrevValue.set(0.0);
      offsetHeightAboveGround.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            offsetHeightAboveGroundChangedTime.set(yoTime.getDoubleValue());
            offsetHeightAboveGroundTrajectory.initialize();
         }
      });

      setMinimumHeightAboveGround(minimumHeightAboveGround);
      setNominalHeightAboveGround(nominalHeightAboveGround);
      setMaximumHeightAboveGround(maximumHeightAboveGround);
      previousZFinal.set(nominalHeightAboveGround);

      hasBeenInitializedWithNextStep.set(false);

      this.doubleSupportPercentageIn.set(doubleSupportPercentageIn);

      this.maximumHeightDeltaBetweenWaypoints.set(0.2); //0.04);

      parentRegistry.addChild(registry);

      if (yoGraphicsListRegistry == null)
      {
         VISUALIZE = false;
      }
      if (VISUALIZE)
      {
         double pointSize = 0.03;

         YoGraphicPosition position0 = new YoGraphicPosition("contactFrame0", contactFrameZeroPosition, pointSize, YoAppearance.Purple());
         YoGraphicPosition position1 = new YoGraphicPosition("contactFrame1", contactFrameOnePosition, pointSize, YoAppearance.Gold());

         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", position0);
         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", position1);

         pointS0Viz = new YoGraphicPosition("pointS0", "", registry, pointSize, YoAppearance.CadetBlue());
         pointSFViz = new YoGraphicPosition("pointSF", "", registry, pointSize, YoAppearance.Chartreuse());
         pointD0Viz = new YoGraphicPosition("pointD0", "", registry, pointSize, YoAppearance.BlueViolet());
         pointDFViz = new YoGraphicPosition("pointDF", "", registry, pointSize, YoAppearance.Azure());
         pointSNextViz = new YoGraphicPosition("pointSNext", "", registry, pointSize, YoAppearance.Pink());

         pointS0MinViz = new YoGraphicPosition("pointS0Min", "", registry, 0.8 * pointSize, YoAppearance.CadetBlue());
         pointSFMinViz = new YoGraphicPosition("pointSFMin", "", registry, 0.8 * pointSize, YoAppearance.Chartreuse());
         pointD0MinViz = new YoGraphicPosition("pointD0Min", "", registry, 0.8 * pointSize, YoAppearance.BlueViolet());
         pointDFMinViz = new YoGraphicPosition("pointDFMin", "", registry, 0.8 * pointSize, YoAppearance.Azure());
         pointSNextMinViz = new YoGraphicPosition("pointSNextMin", "", registry, 0.8 * pointSize, YoAppearance.Pink());

         pointS0MaxViz = new YoGraphicPosition("pointS0Max", "", registry, 0.9 * pointSize, YoAppearance.CadetBlue());
         pointSFMaxViz = new YoGraphicPosition("pointSFMax", "", registry, 0.9 * pointSize, YoAppearance.Chartreuse());
         pointD0MaxViz = new YoGraphicPosition("pointD0Max", "", registry, 0.9 * pointSize, YoAppearance.BlueViolet());
         pointDFMaxViz = new YoGraphicPosition("pointDFMax", "", registry, 0.9 * pointSize, YoAppearance.Azure());
         pointSNextMaxViz = new YoGraphicPosition("pointSNextMax", "", registry, 0.9 * pointSize, YoAppearance.Pink());

         bagOfBalls = new BagOfBalls(registry, yoGraphicsListRegistry);

         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointS0Viz);
         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointSFViz);
         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointD0Viz);
         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointDFViz);
         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointSNextViz);

         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointS0MinViz);
         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointSFMinViz);
         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointD0MinViz);
         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointDFMinViz);
         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointSNextMinViz);

         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointS0MaxViz);
         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointSFMaxViz);
         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointD0MaxViz);
         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointDFMaxViz);
         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", pointSNextMaxViz);

         YoGraphicPosition desiredCoMPositionViz = new YoGraphicPosition("desiredCoMPosition", desiredCoMPosition, 1.1 * pointSize,
               YoAppearance.Gold());
         yoGraphicsListRegistry.registerYoGraphic("CoMHeightTrajectoryGenerator", desiredCoMPositionViz);

      }
      else
      {
         pointS0Viz = null;
         pointSFViz = null;
         pointD0Viz = null;
         pointDFViz = null;
         pointSNextViz = null;

         pointS0MinViz = null;
         pointSFMinViz = null;
         pointD0MinViz = null;
         pointDFMinViz = null;
         pointSNextMinViz = null;

         pointS0MaxViz = null;
         pointSFMaxViz = null;
         pointD0MaxViz = null;
         pointDFMaxViz = null;
         pointSNextMaxViz = null;

         bagOfBalls = null;
      }
   }

   @Override
   public void attachWalkOnToesManager(WalkOnTheEdgesManager walkOnTheEdgesManager)
   {
      this.walkOnTheEdgesManager = walkOnTheEdgesManager;
      extraCoMMaxHeightWithToes = walkOnTheEdgesManager.getExtraCoMMaxHeightWithToes();
   }

   public void setMinimumHeightAboveGround(double minimumHeightAboveGround)
   {
      this.minimumHeightAboveGround.set(minimumHeightAboveGround);
   }

   public void setNominalHeightAboveGround(double nominalHeightAboveGround)
   {
      this.nominalHeightAboveGround.set(nominalHeightAboveGround);
   }

   public void setMaximumHeightAboveGround(double maximumHeightAboveGround)
   {
      this.maximumHeightAboveGround.set(maximumHeightAboveGround);
   }

   public void setCoMHeightDriftCompensation(boolean activate)
   {
      correctForCoMHeightDrift.set(activate);
   }

   @Override
   public void initialize(TransferToAndNextFootstepsData transferToAndNextFootstepsData, RobotSide supportLeg, Footstep nextFootstep,
         List<PlaneContactState> contactStates)
   {
      initialize(transferToAndNextFootstepsData);
   }

   private final Point2d tempPoint2dA = new Point2d();
   private final Point2d tempPoint2dB = new Point2d();

   private final Point2d s0Min = new Point2d();
   private final Point2d d0Min = new Point2d();
   private final Point2d dFMin = new Point2d();
   private final Point2d sFMin = new Point2d();
   private final Point2d sNextMin = new Point2d();

   private final Point2d s0Nom = new Point2d();
   private final Point2d d0Nom = new Point2d();
   private final Point2d dFNom = new Point2d();
   private final Point2d sFNom = new Point2d();
   private final Point2d sNextNom = new Point2d();

   private final Point2d s0Max = new Point2d();
   private final Point2d d0Max = new Point2d();
   private final Point2d dFMax = new Point2d();
   private final Point2d sFMax = new Point2d();
   private final Point2d sNextMax = new Point2d();

   private final Point2d s0 = new Point2d();
   private final Point2d d0 = new Point2d();
   private final Point2d dF = new Point2d();
   private final Point2d sF = new Point2d();
   private final Point2d sNext = new Point2d();

   private void initialize(TransferToAndNextFootstepsData transferToAndNextFootstepsData)
   {
      if (desiredComHeightProvider != null && desiredComHeightProvider.isNewComHeightInformationAvailable())
      {
         offsetHeightAboveGround.set(desiredComHeightProvider.getComHeightOffset());
      }

      Footstep transferFromFootstep = transferToAndNextFootstepsData.getTransferFromFootstep();
      Footstep transferToFootstep = transferToAndNextFootstepsData.getTransferToFootstep();
      Footstep nextFootstep = transferToAndNextFootstepsData.getNextFootstep();

      if (nextFootstep != null)
         hasBeenInitializedWithNextStep.set(true);
      else
         hasBeenInitializedWithNextStep.set(false);

      FramePoint transferFromContactFramePosition = new FramePoint(transferFromFootstep.getPoseReferenceFrame());
      FramePoint transferToContactFramePosition = new FramePoint(transferToFootstep.getPoseReferenceFrame());

      Footstep transferFromDesiredFootstep = transferToAndNextFootstepsData.getTransferFromDesiredFootstep();
      FrameVector fromContactFrameDrift = null;

      if (correctForCoMHeightDrift.getBooleanValue() && transferFromDesiredFootstep != null)
      {
         FramePoint transferFromDesiredContactFramePosition = new FramePoint(transferFromDesiredFootstep.getPoseReferenceFrame());
         transferFromDesiredContactFramePosition.changeFrame(transferFromContactFramePosition.getReferenceFrame());
         fromContactFrameDrift = new FrameVector(transferFromContactFramePosition.getReferenceFrame());
         fromContactFrameDrift.sub(transferFromContactFramePosition, transferFromDesiredContactFramePosition);
         fromContactFrameDrift.changeFrame(transferToContactFramePosition.getReferenceFrame());
         transferToContactFramePosition.setZ(transferToContactFramePosition.getZ() + fromContactFrameDrift.getZ());
      }

      transferFromContactFramePosition.changeFrame(worldFrame);
      transferToContactFramePosition.changeFrame(worldFrame);

      FramePoint nextContactFramePosition = null;
      if (nextFootstep != null)
      {
         nextContactFramePosition = new FramePoint(nextFootstep.getPoseReferenceFrame());
         if (fromContactFrameDrift != null)
         {
            fromContactFrameDrift.changeFrame(nextContactFramePosition.getReferenceFrame());
            nextContactFramePosition.setZ(nextContactFramePosition.getZ() + fromContactFrameDrift.getZ());
         }
         nextContactFramePosition.changeFrame(worldFrame);
      }

      contactFrameZeroPosition.set(transferFromContactFramePosition);
      contactFrameOnePosition.set(transferToContactFramePosition);

      getPoint2d(tempPoint2dA, transferFromContactFramePosition);
      getPoint2d(tempPoint2dB, transferToContactFramePosition);

      projectionSegment = new LineSegment2d(tempPoint2dA, tempPoint2dB);
      setPointXValues(nextContactFramePosition);

      double footHeight0 = transferFromContactFramePosition.getZ();
      double footHeight1 = transferToContactFramePosition.getZ();

      double nextFootHeight = Double.NaN;
      if (nextContactFramePosition != null)
      {
         nextFootHeight = nextContactFramePosition.getZ();
      }

      s0Min.setY(footHeight0 + minimumHeightAboveGround.getDoubleValue());
      s0Nom.setY(footHeight0 + nominalHeightAboveGround.getDoubleValue());
      s0Max.setY(footHeight0 + maximumHeightAboveGround.getDoubleValue());

      sFMin.setY(footHeight1 + minimumHeightAboveGround.getDoubleValue());
      sFNom.setY(footHeight1 + nominalHeightAboveGround.getDoubleValue());
      sFMax.setY(footHeight1 + maximumHeightAboveGround.getDoubleValue());

      d0Min.setY(findMinimumDoubleSupportHeight(s0.getX(), sF.getX(), d0.getX(), footHeight0, footHeight1));
      d0Nom.setY(findNominalDoubleSupportHeight(s0.getX(), sF.getX(), d0.getX(), footHeight0, footHeight1));
      if (walkOnTheEdgesManager != null && walkOnTheEdgesManager.willLandOnToes())
      {
         d0Max.setY(findMaximumDoubleSupportHeight(s0.getX(), sF.getX(), d0.getX(), footHeight0, footHeight1 + extraCoMMaxHeightWithToes));
      }
      else
      {
         d0Max.setY(findMaximumDoubleSupportHeight(s0.getX(), sF.getX(), d0.getX(), footHeight0, footHeight1));
      }

      dFMin.setY(findMinimumDoubleSupportHeight(s0.getX(), sF.getX(), dF.getX(), footHeight0, footHeight1));
      dFNom.setY(findNominalDoubleSupportHeight(s0.getX(), sF.getX(), dF.getX(), footHeight0, footHeight1));

      if (walkOnTheEdgesManager != null && walkOnTheEdgesManager.willDoToeOff(transferToAndNextFootstepsData))
      {
         dFMax.setY(findMaximumDoubleSupportHeight(s0.getX(), sF.getX(), dF.getX(), footHeight0 + extraCoMMaxHeightWithToes, footHeight1));
      }
      else
      {
         dFMax.setY(findMaximumDoubleSupportHeight(s0.getX(), sF.getX(), dF.getX(), footHeight0, footHeight1));
      }

      sNextMin.setY(nextFootHeight + minimumHeightAboveGround.getDoubleValue());
      sNextNom.setY(nextFootHeight + nominalHeightAboveGround.getDoubleValue());
      sNextMax.setY(nextFootHeight + maximumHeightAboveGround.getDoubleValue());

      computeHeightsToUseByStretchingString();
      previousZFinal.set(sF.getY());

      Point2d[] points = new Point2d[] { s0, d0, dF, sF };
      double[] endpointSlopes = new double[] { 0.0, 0.0 };

      double[] waypointSlopes = new double[2];
      waypointSlopes[0] = (points[2].y - points[0].y) / (points[2].x - points[0].x);
      waypointSlopes[1] = (points[3].y - points[1].y) / (points[3].x - points[1].x);

      spline.setPoints(points, endpointSlopes, waypointSlopes);

      if (VISUALIZE)
      {
         FramePoint framePointS0 = new FramePoint(transferFromContactFramePosition);
         framePointS0.setZ(s0.getY() + offsetHeightAboveGround.getDoubleValue());
         pointS0Viz.setPosition(framePointS0);

         framePointS0.setZ(s0Min.getY() + offsetHeightAboveGround.getDoubleValue());
         pointS0MinViz.setPosition(framePointS0);

         framePointS0.setZ(s0Max.getY() + offsetHeightAboveGround.getDoubleValue());
         pointS0MaxViz.setPosition(framePointS0);

         FramePoint framePointD0 = new FramePoint(transferFromContactFramePosition.getReferenceFrame());
         framePointD0.interpolate(transferFromContactFramePosition, transferToContactFramePosition, d0.getX() / sF.getX());
         framePointD0.setZ(d0.getY() + offsetHeightAboveGround.getDoubleValue());
         pointD0Viz.setPosition(framePointD0);

         framePointD0.setZ(d0Min.getY() + offsetHeightAboveGround.getDoubleValue());
         pointD0MinViz.setPosition(framePointD0);

         framePointD0.setZ(d0Max.getY() + offsetHeightAboveGround.getDoubleValue());
         pointD0MaxViz.setPosition(framePointD0);

         FramePoint framePointDF = new FramePoint(transferFromContactFramePosition.getReferenceFrame());
         framePointDF.interpolate(transferFromContactFramePosition, transferToContactFramePosition, dF.getX() / sF.getX());
         framePointDF.setZ(dF.getY() + offsetHeightAboveGround.getDoubleValue());
         pointDFViz.setPosition(framePointDF);

         framePointDF.setZ(dFMin.getY() + offsetHeightAboveGround.getDoubleValue());
         pointDFMinViz.setPosition(framePointDF);

         framePointDF.setZ(dFMax.getY() + offsetHeightAboveGround.getDoubleValue());
         pointDFMaxViz.setPosition(framePointDF);

         FramePoint framePointSF = new FramePoint(transferToContactFramePosition);
         framePointSF.setZ(sF.getY() + offsetHeightAboveGround.getDoubleValue());
         pointSFViz.setPosition(framePointSF);

         framePointSF.setZ(sFMin.getY() + offsetHeightAboveGround.getDoubleValue());
         pointSFMinViz.setPosition(framePointSF);

         framePointSF.setZ(sFMax.getY() + offsetHeightAboveGround.getDoubleValue());
         pointSFMaxViz.setPosition(framePointSF);

         if (nextContactFramePosition != null)
         {
            FramePoint framePointSNext = new FramePoint(nextContactFramePosition);
            framePointSNext.setZ(sNext.getY() + offsetHeightAboveGround.getDoubleValue());
            pointSNextViz.setPosition(framePointSNext);

            framePointSNext.setZ(sNextMin.getY() + offsetHeightAboveGround.getDoubleValue());
            pointSNextMinViz.setPosition(framePointSNext);

            framePointSNext.setZ(sNextMax.getY() + offsetHeightAboveGround.getDoubleValue());
            pointSNextMaxViz.setPosition(framePointSNext);
         }
         else
         {
            pointSNextViz.setPositionToNaN();
            pointSNextMinViz.setPositionToNaN();
            pointSNextMaxViz.setPositionToNaN();
         }

         bagOfBalls.reset();
         int numberOfPoints = 30;
         CoMHeightPartialDerivativesData coMHeightPartialDerivativesData = new CoMHeightPartialDerivativesData();
         for (int i = 0; i < numberOfPoints; i++)
         {
            FramePoint framePoint = new FramePoint(transferFromContactFramePosition.getReferenceFrame());
            framePoint.interpolate(transferFromContactFramePosition, transferToContactFramePosition, ((double) i) / ((double) numberOfPoints));
            Point2d queryPoint = new Point2d(framePoint.getX(), framePoint.getY());
            this.solve(coMHeightPartialDerivativesData, queryPoint);
            FramePoint framePointToPack = new FramePoint();
            coMHeightPartialDerivativesData.getCoMHeight(framePointToPack);
            framePointToPack.setX(framePoint.getX());
            framePointToPack.setY(framePoint.getY());

            bagOfBalls.setBallLoop(framePointToPack);
         }
      }
   }

   private void computeHeightsToUseByStretchingString()
   {
      // s0 is at previous
      double z0 = MathTools.clipToMinMax(previousZFinal.getDoubleValue(), s0Min.getY(), s0Max.getY());
      s0.setY(z0);

      StringStretcher2d stringStretcher2d = new StringStretcher2d();
      stringStretcher2d.setStartPoint(s0);

      if (!Double.isNaN(sNext.getX()))
      {
         if (sNext.getX() < sF.getX() + 0.01)
         {
            sNext.setX(sF.getX() + 0.01);
         }

         double zNext;
         if (sNextMin.getY() > sFNom.getY())
         {
            zNext = sNextMin.getY();
         }
         else if (sNextMax.getY() < sFNom.getY())
         {
            zNext = sNextMax.getY();
         }
         else
         {
            zNext = sFNom.getY();
         }
         sNext.setY(zNext);

         stringStretcher2d.setEndPoint(sNext);

         stringStretcher2d.addMinMaxPoints(d0Min, d0Max);
         stringStretcher2d.addMinMaxPoints(dFMin, dFMax);
         stringStretcher2d.addMinMaxPoints(sFMin, sFMax);
      }
      else
      {
         stringStretcher2d.setEndPoint(sFNom);

         stringStretcher2d.addMinMaxPoints(d0Min, d0Max);
         stringStretcher2d.addMinMaxPoints(dFMin, dFMax);
      }

      List<Point2d> stretchedString = stringStretcher2d.stretchString();

      d0.set(stretchedString.get(1));
      dF.set(stretchedString.get(2));
      sF.set(stretchedString.get(3));
   }

   private void setPointXValues(FramePoint nextContactFramePosition)
   {
      double length = projectionSegment.length();

      double xS0 = 0.0;
      double xD0 = doubleSupportPercentageIn.getDoubleValue() * length;
      double xDF = (1.0 - doubleSupportPercentageIn.getDoubleValue()) * length;
      double xSF = length;

      double xSNext = Double.NaN;
      if (nextContactFramePosition != null)
      {
         Line2d line2d = new Line2d(projectionSegment.getFirstEndPointCopy(), projectionSegment.getSecondEndPointCopy());
         Point2d nextPoint2d = new Point2d(nextContactFramePosition.getX(), nextContactFramePosition.getY());
         line2d.orthogonalProjectionCopy(nextPoint2d);
         xSNext = projectionSegment.percentageAlongLineSegment(nextPoint2d) * projectionSegment.length();
      }

      s0.setX(xS0);
      d0.setX(xD0);
      dF.setX(xDF);
      sF.setX(xSF);
      sNext.setX(xSNext);

      s0Min.setX(xS0);
      d0Min.setX(xD0);
      dFMin.setX(xDF);
      sFMin.setX(xSF);
      sNextMin.setX(xSNext);

      s0Nom.setX(xS0);
      d0Nom.setX(xD0);
      dFNom.setX(xDF);
      sFNom.setX(xSF);
      sNextNom.setX(xSNext);

      s0Max.setX(xS0);
      d0Max.setX(xD0);
      dFMax.setX(xDF);
      sFMax.setX(xSF);
      sNextMax.setX(xSNext);
   }

   public double findMinimumDoubleSupportHeight(double s0, double sF, double s_d0, double foot0Height, double foot1Height)
   {
      return findDoubleSupportHeight(minimumHeightAboveGround.getDoubleValue(), s0, sF, s_d0, foot0Height, foot1Height);
   }

   public double findNominalDoubleSupportHeight(double s0, double sF, double s_d0, double foot0Height, double foot1Height)
   {
      return findDoubleSupportHeight(nominalHeightAboveGround.getDoubleValue(), s0, sF, s_d0, foot0Height, foot1Height);
   }

   public double findMaximumDoubleSupportHeight(double s0, double sF, double s_d0, double foot0Height, double foot1Height)
   {
      return findDoubleSupportHeight(maximumHeightAboveGround.getDoubleValue(), s0, sF, s_d0, foot0Height, foot1Height);
   }

   private double findDoubleSupportHeight(double distanceFromFoot, double s0, double sF, double s_d0, double foot0Height, double foot1Height)
   {
      double z_d0_A = foot0Height + Math.sqrt(MathTools.square(distanceFromFoot) - MathTools.square(s_d0 - s0));
      double z_d0_B = foot1Height + Math.sqrt(MathTools.square(distanceFromFoot) - MathTools.square((sF - s_d0)));
      double z_d0 = Math.min(z_d0_A, z_d0_B);
      return z_d0;
   }

   private void getPoint2d(Point2d point2dToPack, FramePoint point)
   {
      point2dToPack.set(point.getX(), point.getY());
   }

   private final FramePoint tempFramePoint = new FramePoint();
   private final Point2d queryPoint = new Point2d();
   private final Point2d solutionPoint = new Point2d();

   @Override
   public void solve(CoMHeightPartialDerivativesData coMHeightPartialDerivativesDataToPack, ContactStatesAndUpcomingFootstepData centerOfMassHeightInputData)
   {
      getCenterOfMass2d(queryPoint, centerOfMassHeightInputData.getCenterOfMassFrame());
      solutionPoint.set(queryPoint);
      solve(coMHeightPartialDerivativesDataToPack, solutionPoint);

      coMHeightPartialDerivativesDataToPack.getCoMHeight(tempFramePoint);
      desiredCoMPosition.set(queryPoint.getX(), queryPoint.getY(), tempFramePoint.getZ());
   }

   private void solve(CoMHeightPartialDerivativesData coMHeightPartialDerivativesDataToPack, Point2d queryPoint)
   {
      if (desiredComHeightProvider != null && desiredComHeightProvider.isNewComHeightInformationAvailable())
      {
         offsetHeightAboveGround.set(desiredComHeightProvider.getComHeightOffset());
      }
      offsetHeightAboveGroundTrajectory.compute(yoTime.getDoubleValue() - offsetHeightAboveGroundChangedTime.getDoubleValue());
      offsetHeightAboveGroundTrajectoryOutput.set(offsetHeightAboveGroundTrajectory.getValue());

      offsetHeightAboveGroundPrevValue.set(offsetHeightAboveGroundTrajectory.getValue());

      projectionSegment.orthogonalProjection(queryPoint);
      double splineQuery = projectionSegment.percentageAlongLineSegment(queryPoint) * projectionSegment.length();

      double[] splineOutput = spline.getZSlopeAndSecondDerivative(splineQuery);
      double z = splineOutput[0] + offsetHeightAboveGroundTrajectoryOutput.getValue();
      double dzds = splineOutput[1];
      double ddzdds = splineOutput[2];

      double[] partialDerivativesWithRespectToS = getPartialDerivativesWithRespectToS(projectionSegment);
      double dsdx = partialDerivativesWithRespectToS[0];
      double dsdy = partialDerivativesWithRespectToS[1];
      double ddsddx = 0;
      double ddsddy = 0;
      double ddsdxdy = 0;

      double dzdx = dsdx * dzds;
      double dzdy = dsdy * dzds;
      double ddzddx = dzds * ddsddx + ddzdds * dsdx * dsdx;
      double ddzddy = dzds * ddsddy + ddzdds * dsdy * dsdy;
      double ddzdxdy = ddzdds * dsdx * dsdy + dzds * ddsdxdy;

      coMHeightPartialDerivativesDataToPack.setCoMHeight(worldFrame, z);
      coMHeightPartialDerivativesDataToPack.setPartialDzDx(dzdx);
      coMHeightPartialDerivativesDataToPack.setPartialDzDy(dzdy);
      coMHeightPartialDerivativesDataToPack.setPartialD2zDxDy(ddzdxdy);
      coMHeightPartialDerivativesDataToPack.setPartialD2zDx2(ddzddx);
      coMHeightPartialDerivativesDataToPack.setPartialD2zDy2(ddzddy);

      desiredCoMHeight.set(z);
   }

   private double[] getPartialDerivativesWithRespectToS(LineSegment2d segment)
   {
      double dsdx = (segment.getSecondEndPointCopy().getX() - segment.getFirstEndPointCopy().getX()) / segment.length();
      double dsdy = (segment.getSecondEndPointCopy().getY() - segment.getFirstEndPointCopy().getY()) / segment.length();

      return new double[] { dsdx, dsdy };
   }

   private final FramePoint coM = new FramePoint();

   private void getCenterOfMass2d(Point2d point2dToPack, ReferenceFrame centerOfMassFrame)
   {
      coM.setToZero(centerOfMassFrame);
      coM.changeFrame(worldFrame);

      getPoint2d(point2dToPack, coM);
   }

   @Override
   public boolean hasBeenInitializedWithNextStep()
   {
      return hasBeenInitializedWithNextStep.getBooleanValue();
   }
   
   public void setOffsetHeightAboveGround(double value)
   {
      offsetHeightAboveGround.set(value);
   }
   
   public double getOffsetHeightAboveGround()
   {
      return offsetHeightAboveGround.getDoubleValue();
   }
}