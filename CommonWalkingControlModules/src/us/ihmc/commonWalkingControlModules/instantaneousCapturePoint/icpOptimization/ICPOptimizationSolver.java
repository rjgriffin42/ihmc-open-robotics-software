package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import us.ihmc.convexOptimization.quadraticProgram.SimpleEfficientActiveSetQPSolver;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.math.frames.YoMatrix;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.tools.io.printing.PrintTools;

import java.util.ArrayList;

public class ICPOptimizationSolver
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private static final boolean DEBUG = false;
   private final boolean localDebug;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final double betaSmoothing = 0.0001;

   private final YoMatrix yoWeightG;
   private final YoMatrix yoWeightg;
   private final YoMatrix footstepH;
   private final YoMatrix footsteph;
   private final YoMatrix footstepReferenceLocation;

   private final YoMatrix yoDynamics_Aeq;
   private final YoMatrix yoDynamics_beq;
   private final YoMatrix yoSolver_Aeq;
   private final YoMatrix yoSolver_beq;

   private final YoMatrix yoStanceCMPDynamics_Aeq;
   private final YoMatrix yoStanceCMPDynamics_beq;
   private final YoMatrix yoStanceCMPSum_Aeq;
   private final YoMatrix yoStanceCMPSum_beq;

   protected final DenseMatrix64F solverInput_H;
   protected final DenseMatrix64F solverInput_h;
   protected final DenseMatrix64F solverInputResidualCost;

   protected final DenseMatrix64F footstepCost_H;
   protected final DenseMatrix64F footstepCost_h;
   protected final DenseMatrix64F footstepResidualCost;

   protected final DenseMatrix64F footstepRegularizationCost_H;
   protected final DenseMatrix64F footstepRegularizationCost_h;
   protected final DenseMatrix64F footstepRegularizationResidualCost;

   protected final DenseMatrix64F feedbackCost_H;
   protected final DenseMatrix64F feedbackCost_h;
   protected final DenseMatrix64F feedbackResidualCost;

   protected final DenseMatrix64F dynamicRelaxationCost_H;
   protected final DenseMatrix64F dynamicRelaxationCost_h;
   protected final DenseMatrix64F dynamicRelaxationResidualCost;

   protected final DenseMatrix64F feedbackRegularizationCost_H;
   protected final DenseMatrix64F feedbackRegularizationCost_h;
   protected final DenseMatrix64F feedbackRegularizationResidualCost;

   protected final DenseMatrix64F solverInput_Aeq;
   protected final DenseMatrix64F solverInput_AeqTrans;
   protected final DenseMatrix64F solverInput_beq;

   protected final DenseMatrix64F solverInput_Aineq;
   protected final DenseMatrix64F solverInput_AineqTrans;
   protected final DenseMatrix64F solverInput_bineq;

   protected final DenseMatrix64F dynamics_Aeq;
   protected final DenseMatrix64F dynamics_beq;

   protected final DenseMatrix64F stanceCMPCost_G;
   protected final DenseMatrix64F stanceCMP_Aeq;
   protected final DenseMatrix64F stanceCMP_beq;
   protected final DenseMatrix64F stanceCMPDynamics_Aeq;
   protected final DenseMatrix64F stanceCMPDynamics_beq;
   protected final DenseMatrix64F stanceCMPSum_Aeq;
   protected final DenseMatrix64F stanceCMPSum_beq;
   protected final DenseMatrix64F stanceCMP_Aineq;
   protected final DenseMatrix64F stanceCMP_bineq;

   protected final ArrayList<DenseMatrix64F> vertexLocations = new ArrayList<>();

   protected final ArrayList<DenseMatrix64F> footstepRecursionMutlipliers = new ArrayList<>();
   protected final ArrayList<DenseMatrix64F> referenceFootstepLocations = new ArrayList<>();
   protected final ArrayList<DenseMatrix64F> previousFootstepLocations = new ArrayList<>();
   private final DenseMatrix64F footstepObjectiveVector;

   protected final DenseMatrix64F finalICPRecursion = new DenseMatrix64F(2, 1);
   protected final DenseMatrix64F cmpOffsetRecursionEffect = new DenseMatrix64F(2, 1);
   protected final DenseMatrix64F stanceCMPProjection = new DenseMatrix64F(2, 1);
   protected final DenseMatrix64F currentICP = new DenseMatrix64F(2, 1);
   protected final DenseMatrix64F referenceICP = new DenseMatrix64F(2, 1);
   protected final DenseMatrix64F perfectCMP = new DenseMatrix64F(2, 1);

   protected double currentStateProjection;

   protected final ArrayList<DenseMatrix64F> footstepWeights = new ArrayList<>();
   protected final DenseMatrix64F footstepRegularizationWeight = new DenseMatrix64F(2, 2);
   protected final DenseMatrix64F feedbackWeight = new DenseMatrix64F(2, 2);
   protected final DenseMatrix64F dynamicRelaxationWeight = new DenseMatrix64F(2, 2);
   protected final DenseMatrix64F feedbackRegularizationWeight = new DenseMatrix64F(2, 2);
   protected final DenseMatrix64F feedbackGain = new DenseMatrix64F(2, 2);

   private final SimpleEfficientActiveSetQPSolver activeSetSolver;

   protected final DenseMatrix64F solution;
   protected final DenseMatrix64F freeVariableSolution;
   protected final DenseMatrix64F lagrangeMultiplierSolution;
   protected final DenseMatrix64F footstepLocationSolution;
   protected final DenseMatrix64F feedbackDeltaSolution;
   protected final DenseMatrix64F dynamicRelaxationSolution;
   protected final DenseMatrix64F feedbackLocation;
   protected final DenseMatrix64F previousFeedbackDeltaSolution;

   private final DenseMatrix64F tmpCost;
   private final DenseMatrix64F tmpFootstepCost;
   private final DenseMatrix64F tmpFeedbackCost;
   private final DenseMatrix64F costToGo;
   private final DenseMatrix64F footstepCostToGo;
   private final DenseMatrix64F footstepRegularizationCostToGo;
   private final DenseMatrix64F feedbackCostToGo;
   private final DenseMatrix64F feedbackRegularizationCostToGo;
   private final DenseMatrix64F dynamicRelaxationCostToGo;

   protected final int maximumNumberOfFootstepsToConsider;
   private final int maximumNumberOfVertices;

   protected int numberOfFootstepsToConsider;
   protected int numberOfVertices;
   protected int numberOfFreeVariables = 0;
   protected int numberOfFootstepVariables = 0;
   protected int numberOfVertexVariables = 0;
   protected int numberOfLagrangeMultipliers = 2;

   private int feedbackCMPIndex;
   private int dynamicRelaxtionIndex;
   private int cmpConstraintIndex;
   private int lagrangeMultiplierIndex;

   private boolean useFeedback = false;
   private boolean useStepAdjustment = true;
   private boolean useTwoCMPs = false;

   private boolean hasFootstepRegularizationTerm = false;
   private boolean hasFeedbackRegularizationTerm = false;

   private final double minimumFootstepWeight;
   private final double minimumFeedbackWeight;

   private final double feedbackWeightHardeningMultiplier;

   public ICPOptimizationSolver(ICPOptimizationParameters icpOptimizationParameters, int maximumNumberOfVertices)
   {
      this(icpOptimizationParameters, maximumNumberOfVertices, null);
   }

   public ICPOptimizationSolver(ICPOptimizationParameters icpOptimizationParameters, int maximumNumberOfVertices, YoVariableRegistry parentRegistry)
   {
      maximumNumberOfFootstepsToConsider = icpOptimizationParameters.getMaximumNumberOfFootstepsToConsider();
      this.maximumNumberOfVertices = maximumNumberOfVertices;

      minimumFootstepWeight = icpOptimizationParameters.getMinimumFootstepWeight();
      minimumFeedbackWeight = icpOptimizationParameters.getMinimumFeedbackWeight();

      feedbackWeightHardeningMultiplier = icpOptimizationParameters.getFeedbackWeightHardeningMultiplier();

      int maximumNumberOfFreeVariables = 2 * maximumNumberOfFootstepsToConsider + 2 * maximumNumberOfVertices + 4;
      int maximumNumberOfLagrangeMultipliers = 8;

      solverInput_H = new DenseMatrix64F(maximumNumberOfFreeVariables, maximumNumberOfFreeVariables);
      solverInput_h = new DenseMatrix64F(maximumNumberOfFreeVariables, 1);
      solverInputResidualCost = new DenseMatrix64F(1, 1);

      footstepCost_H = new DenseMatrix64F(2 * maximumNumberOfFootstepsToConsider, 2 * maximumNumberOfFootstepsToConsider);
      footstepCost_h = new DenseMatrix64F(2 * maximumNumberOfFootstepsToConsider, 1);
      footstepResidualCost = new DenseMatrix64F(1, 1);

      footstepRegularizationCost_H = new DenseMatrix64F(2 * maximumNumberOfFootstepsToConsider, 2 * maximumNumberOfFootstepsToConsider);
      footstepRegularizationCost_h = new DenseMatrix64F(2 * maximumNumberOfFootstepsToConsider, 1);
      footstepRegularizationResidualCost = new DenseMatrix64F(1, 1);

      feedbackCost_H = new DenseMatrix64F(2, 2);
      feedbackCost_h = new DenseMatrix64F(2, 1);
      feedbackResidualCost = new DenseMatrix64F(1, 1);

      dynamicRelaxationCost_H = new DenseMatrix64F(2, 2);
      dynamicRelaxationCost_h = new DenseMatrix64F(2, 1);
      dynamicRelaxationResidualCost = new DenseMatrix64F(1, 1);

      feedbackRegularizationCost_H = new DenseMatrix64F(2, 2);
      feedbackRegularizationCost_h = new DenseMatrix64F(2, 1);
      feedbackRegularizationResidualCost = new DenseMatrix64F(1, 1);

      solverInput_Aeq = new DenseMatrix64F(maximumNumberOfFreeVariables, maximumNumberOfLagrangeMultipliers);
      solverInput_AeqTrans = new DenseMatrix64F(maximumNumberOfLagrangeMultipliers, maximumNumberOfFreeVariables);
      solverInput_beq = new DenseMatrix64F(maximumNumberOfLagrangeMultipliers, 1);

      dynamics_Aeq = new DenseMatrix64F(maximumNumberOfFreeVariables, 2);
      dynamics_beq = new DenseMatrix64F(2, 1);

      stanceCMPCost_G = new DenseMatrix64F(2 * maximumNumberOfVertices, 2 * maximumNumberOfVertices);
      stanceCMP_Aeq = new DenseMatrix64F(2 * maximumNumberOfVertices, 4);
      stanceCMP_beq = new DenseMatrix64F(4, 1);
      stanceCMPDynamics_Aeq = new DenseMatrix64F(4 + 2 * maximumNumberOfVertices, 2);
      stanceCMPDynamics_beq = new DenseMatrix64F(2, 1);
      stanceCMPSum_Aeq = new DenseMatrix64F(2 * maximumNumberOfVertices, 2);
      stanceCMPSum_beq = new DenseMatrix64F(2, 1);


      stanceCMP_Aineq = new DenseMatrix64F(2 * maximumNumberOfVertices, maximumNumberOfVertices);
      stanceCMP_bineq = new DenseMatrix64F(maximumNumberOfVertices, 1);

      solverInput_Aineq = new DenseMatrix64F(2 * maximumNumberOfVertices, maximumNumberOfVertices);
      solverInput_AineqTrans = new DenseMatrix64F(maximumNumberOfVertices, 2 * maximumNumberOfVertices);
      solverInput_bineq = new DenseMatrix64F(2 * maximumNumberOfVertices, 1);

      for (int i = 0; i < maximumNumberOfFootstepsToConsider; i++)
      {
         referenceFootstepLocations.add(new DenseMatrix64F(2, 1));
         previousFootstepLocations.add(new DenseMatrix64F(2, 1));

         footstepRecursionMutlipliers.add(new DenseMatrix64F(2, 2));
         footstepWeights.add(new DenseMatrix64F(2, 2));
      }
      footstepObjectiveVector =  new DenseMatrix64F(2 * maximumNumberOfFreeVariables, 1);

      for (int i = 0; i < maximumNumberOfVertices; i++)
      {
         vertexLocations.add(new DenseMatrix64F(2, 1));
      }

      solution = new DenseMatrix64F(maximumNumberOfFreeVariables + maximumNumberOfLagrangeMultipliers, 1);
      lagrangeMultiplierSolution = new DenseMatrix64F(maximumNumberOfLagrangeMultipliers, 1);
      freeVariableSolution = new DenseMatrix64F(maximumNumberOfFreeVariables, 1);
      footstepLocationSolution = new DenseMatrix64F(2 * maximumNumberOfFootstepsToConsider, 1);
      feedbackDeltaSolution = new DenseMatrix64F(2, 1);
      dynamicRelaxationSolution = new DenseMatrix64F(2, 1);
      feedbackLocation = new DenseMatrix64F(2, 1);
      previousFeedbackDeltaSolution = new DenseMatrix64F(2, 1);

      tmpCost = new DenseMatrix64F(maximumNumberOfFreeVariables + maximumNumberOfLagrangeMultipliers, 1);
      tmpFootstepCost = new DenseMatrix64F(2 * maximumNumberOfFootstepsToConsider, 1);
      tmpFeedbackCost = new DenseMatrix64F(2, 1);
      costToGo = new DenseMatrix64F(1, 1);
      footstepCostToGo = new DenseMatrix64F(1, 1);
      footstepRegularizationCostToGo = new DenseMatrix64F(1, 1);
      feedbackCostToGo = new DenseMatrix64F(1, 1);
      feedbackRegularizationCostToGo = new DenseMatrix64F(1, 1);
      dynamicRelaxationCostToGo = new DenseMatrix64F(1, 1);

      activeSetSolver = new SimpleEfficientActiveSetQPSolver();

      if (parentRegistry != null && DEBUG)
      {
         localDebug = true;

         yoWeightG = new YoMatrix("solverQuadraticCost", maximumNumberOfFreeVariables, maximumNumberOfFreeVariables, registry);
         yoWeightg = new YoMatrix("solverLinearCost", maximumNumberOfFreeVariables, 1, registry);

         footstepH = new YoMatrix("footstepQuadraticCost", 2 * maximumNumberOfFootstepsToConsider, 2 * maximumNumberOfFootstepsToConsider, registry);
         footsteph = new YoMatrix("footstepLinearCost", 2 * maximumNumberOfFootstepsToConsider, 1, registry);

         yoDynamics_Aeq = new YoMatrix("dynamics_Aeq", maximumNumberOfFreeVariables, 2, registry);
         yoDynamics_beq = new YoMatrix("dynamics_beq", 2, 1, registry);

         yoSolver_Aeq = new YoMatrix("solver_Aeq", maximumNumberOfFreeVariables, maximumNumberOfLagrangeMultipliers, registry);
         yoSolver_beq = new YoMatrix("solver_beq", maximumNumberOfLagrangeMultipliers, 1, registry);

         yoStanceCMPDynamics_Aeq = new YoMatrix("stanceCMPDynamics_Aeq", 4 + 2 * maximumNumberOfVertices, 2, registry);
         yoStanceCMPDynamics_beq = new YoMatrix("stanceCMPDynamics_beq", 2, 1, registry);
         yoStanceCMPSum_Aeq = new YoMatrix("stanceCMPSum_Aeq", 2 * maximumNumberOfVertices, 2, registry);
         yoStanceCMPSum_beq = new YoMatrix("stanceCMPSum_beq", 2, 1, registry);

         footstepReferenceLocation = new YoMatrix("footstepReferenceLocation", 2 * maximumNumberOfFootstepsToConsider, 1, registry);

         parentRegistry.addChild(registry);
      }
      else
      {
         localDebug = false;

         yoWeightG = null;
         yoWeightg = null;

         footstepH = null;
         footsteph = null;

         yoDynamics_Aeq = null;
         yoDynamics_beq = null;

         yoSolver_Aeq = null;
         yoSolver_beq = null;

         yoStanceCMPDynamics_Aeq = null;
         yoStanceCMPDynamics_beq = null;
         yoStanceCMPSum_Aeq = null;
         yoStanceCMPSum_beq = null;

         footstepReferenceLocation = null;
      }
   }

   public void setNumberOfVertices(int numberOfVertices)
   {
      this.numberOfVertices = numberOfVertices;
      numberOfVertexVariables = 2 * numberOfVertices;

      stanceCMPCost_G.zero();
      stanceCMP_Aeq.zero();
      stanceCMP_beq.zero();
      stanceCMPDynamics_Aeq.zero();
      stanceCMPDynamics_beq.zero();
      stanceCMPSum_Aeq.zero();
      stanceCMPSum_beq.zero();

      stanceCMP_Aineq.zero();
      stanceCMP_bineq.zero();

      stanceCMPCost_G.reshape(numberOfVertexVariables, numberOfVertexVariables);
      stanceCMP_Aeq.reshape(2 + numberOfVertexVariables, 4);
      stanceCMPDynamics_Aeq.reshape(2 + numberOfVertexVariables, 2);
      stanceCMPSum_Aeq.reshape(numberOfVertexVariables, 2);

      stanceCMP_Aineq.reshape(numberOfVertexVariables, numberOfVertexVariables);
      stanceCMP_bineq.reshape(numberOfVertexVariables, 1);

      for (int i = 0; i < maximumNumberOfVertices; i++)
      {
         vertexLocations.get(i).zero();
      }
   }

   public void submitProblemConditions(int numberOfFootstepsToConsider, boolean useStepAdjustment, boolean useFeedback, boolean useTwoCMPs)
   {
      if (!useFeedback && (!useStepAdjustment || numberOfFootstepsToConsider < 1))
      {
         throw new RuntimeException("No possible feedback mechanism available.");
      }

      this.useFeedback = useFeedback;
      this.useStepAdjustment = useStepAdjustment;
      this.useTwoCMPs = useTwoCMPs;

      if (useFeedback && !useStepAdjustment)
         this.numberOfFootstepsToConsider = 0;
      else
         this.numberOfFootstepsToConsider = numberOfFootstepsToConsider;

      numberOfFootstepVariables = 2 * this.numberOfFootstepsToConsider;

      if (useFeedback)
      {
         numberOfLagrangeMultipliers = 2;

         feedbackCMPIndex = 2 * numberOfFootstepsToConsider;
         dynamicRelaxtionIndex = feedbackCMPIndex + 2;

         if (numberOfVertices > 0)
         {
            numberOfLagrangeMultipliers += 4;
            cmpConstraintIndex = dynamicRelaxtionIndex + 2;
         }

         numberOfFreeVariables = numberOfFootstepVariables + 4;

         lagrangeMultiplierIndex = cmpConstraintIndex + numberOfVertexVariables;
      }
      else
      {
         numberOfFreeVariables = numberOfFootstepVariables;
         numberOfLagrangeMultipliers = 0;
         this.numberOfVertices = 0;
         numberOfVertexVariables = 0;

         feedbackCMPIndex = 0;
         lagrangeMultiplierIndex = numberOfFootstepVariables;
      }

      reset();
      reshape();
   }

   private void reset()
   {
      solverInput_H.zero();
      solverInput_h.zero();
      solverInputResidualCost.zero();

      footstepCost_H.zero();
      footstepCost_h.zero();
      footstepResidualCost.zero();

      footstepRegularizationCost_H.zero();
      footstepRegularizationCost_h.zero();
      footstepRegularizationResidualCost.zero();

      feedbackCost_H.zero();
      feedbackCost_h.zero();
      feedbackResidualCost.zero();

      dynamicRelaxationCost_H.zero();
      dynamicRelaxationCost_h.zero();
      dynamicRelaxationResidualCost.zero();

      feedbackRegularizationCost_H.zero();
      feedbackRegularizationCost_h.zero();
      feedbackRegularizationResidualCost.zero();

      solverInput_Aeq.zero();
      solverInput_AeqTrans.zero();
      solverInput_beq.zero();

      solverInput_Aineq.zero();
      solverInput_AineqTrans.zero();
      solverInput_bineq.zero();

      dynamics_Aeq.zero();
      dynamics_beq.zero();

      for (int i = 0; i < maximumNumberOfFootstepsToConsider; i++)
      {
         referenceFootstepLocations.get(i).zero();
         footstepRecursionMutlipliers.get(i).zero();
         footstepWeights.get(i).zero();
      }

      finalICPRecursion.zero();
      cmpOffsetRecursionEffect.zero();
      currentICP.zero();
      referenceICP.zero();
      perfectCMP.zero();

      footstepRegularizationWeight.zero();
      feedbackWeight.zero();
      feedbackGain.zero();
      dynamicRelaxationWeight.zero();

      solution.zero();
      lagrangeMultiplierSolution.zero();
      freeVariableSolution.zero();
      footstepLocationSolution.zero();
      feedbackDeltaSolution.zero();
      dynamicRelaxationSolution.zero();
      feedbackLocation.zero();

      hasFootstepRegularizationTerm = false;
   }

   private void reshape()
   {
      solverInput_H.reshape(numberOfFreeVariables + numberOfVertexVariables, numberOfFreeVariables + numberOfVertexVariables);
      solverInput_h.reshape(numberOfFreeVariables + numberOfVertexVariables, 1);

      footstepCost_H.reshape(numberOfFootstepVariables, numberOfFootstepVariables);
      footstepCost_h.reshape(numberOfFootstepVariables, 1);

      footstepRegularizationCost_H.reshape(numberOfFootstepVariables, numberOfFootstepVariables);
      footstepRegularizationCost_h.reshape(numberOfFootstepVariables, 1);

      solverInput_Aeq.reshape(numberOfFreeVariables + numberOfVertexVariables, numberOfLagrangeMultipliers);
      solverInput_AeqTrans.reshape(numberOfLagrangeMultipliers, numberOfFreeVariables + numberOfVertexVariables);
      solverInput_beq.reshape(numberOfLagrangeMultipliers, 1);

      solverInput_Aineq.reshape(numberOfFreeVariables + numberOfVertexVariables, numberOfVertexVariables);
      solverInput_AineqTrans.reshape(numberOfVertexVariables, numberOfFreeVariables + numberOfVertexVariables);
      solverInput_bineq.reshape(numberOfVertexVariables, 1);

      dynamics_Aeq.reshape(numberOfFreeVariables + numberOfVertexVariables, 2);

      solution.reshape(numberOfFreeVariables + numberOfVertexVariables + numberOfLagrangeMultipliers, 1);
      freeVariableSolution.reshape(numberOfFreeVariables + numberOfVertexVariables, 1);
      lagrangeMultiplierSolution.reshape(numberOfLagrangeMultipliers, 1);
      footstepLocationSolution.reshape(numberOfFootstepVariables, 1);
      footstepObjectiveVector.reshape(numberOfFootstepVariables, 1);
   }

   private final DenseMatrix64F identity = CommonOps.identity(2, 2);
   public void setFootstepAdjustmentConditions(int footstepIndex, double recursionMultiplier, double weight, FramePoint2d referenceFootstepLocation)
   {
      this.setFootstepAdjustmentConditions(footstepIndex, recursionMultiplier, weight, weight, referenceFootstepLocation);
   }

   public void setFootstepAdjustmentConditions(int footstepIndex, double recursionMultiplier, double xWeight, double yWeight, FramePoint2d referenceFootstepLocation)
   {
      setFootstepRecursionMutliplier(footstepIndex, recursionMultiplier);
      setFootstepWeight(footstepIndex, xWeight, yWeight);
      setReferenceFootstepLocation(footstepIndex, referenceFootstepLocation);
   }

   private void setFootstepRecursionMutliplier(int footstepIndex, double recursionMultiplier)
   {
      CommonOps.setIdentity(identity);
      MatrixTools.setMatrixBlock(footstepRecursionMutlipliers.get(footstepIndex), 0, 0, identity, 0, 0, 2, 2, recursionMultiplier);
   }

   private void setFootstepWeight(int footstepIndex, double xWeight, double yWeight)
   {
      xWeight = Math.max(minimumFootstepWeight, xWeight);
      yWeight = Math.max(minimumFootstepWeight, yWeight);

      identity.zero();
      identity.set(0, 0, xWeight);
      identity.set(1, 1, yWeight);
      MatrixTools.setMatrixBlock(footstepWeights.get(footstepIndex), 0, 0, identity, 0, 0, 2, 2, 1.0);
   }

   private void setReferenceFootstepLocation(int footstepIndex, FramePoint2d referenceFootstepLocation)
   {
      referenceFootstepLocation.changeFrame(worldFrame);
      referenceFootstepLocations.get(footstepIndex).set(0, 0, referenceFootstepLocation.getX());
      referenceFootstepLocations.get(footstepIndex).set(1, 0, referenceFootstepLocation.getY());
   }

   public void setFeedbackConditions(double feedbackWeight, double feedbackGain, double dynamicRelaxationWeight, double omega)
   {
      this.setFeedbackConditions(feedbackWeight, feedbackWeight, feedbackGain, feedbackGain, dynamicRelaxationWeight,omega);
   }

   public void setFeedbackConditions(double feedbackXWeight, double feedbackYWeight, double feedbackXGain, double feedbackYGain, double dynamicRelaxationWeight,
         double omega)
   {
      feedbackXGain = feedbackXGain / omega;
      feedbackYGain = feedbackYGain / omega;

      feedbackXWeight = Math.max(feedbackXWeight, minimumFeedbackWeight);
      feedbackYWeight = Math.max(feedbackYWeight, minimumFeedbackWeight);

      this.feedbackWeight.zero();
      this.feedbackWeight.set(0, 0, feedbackXWeight);
      this.feedbackWeight.set(1, 1, feedbackYWeight);

      this.feedbackGain.zero();
      this.feedbackGain.set(0, 0, feedbackXGain);
      this.feedbackGain.set(1, 1, feedbackYGain);

      CommonOps.setIdentity(this.dynamicRelaxationWeight);
      CommonOps.scale(dynamicRelaxationWeight, this.dynamicRelaxationWeight);
   }

   public void setFeedbackRegularizationWeight(double regularizationWeight)
   {
      CommonOps.setIdentity(feedbackRegularizationWeight);
      CommonOps.scale(regularizationWeight, feedbackRegularizationWeight);

      hasFeedbackRegularizationTerm = true;
   }

   public void setUseFeedbackWeightHardening()
   {
      double xWeight = feedbackWeight.get(0, 0);
      double yWeight = feedbackWeight.get(1, 1);

      xWeight *= (1.0 + feedbackWeightHardeningMultiplier * Math.abs(previousFeedbackDeltaSolution.get(0, 0)));
      yWeight *= (1.0 + feedbackWeightHardeningMultiplier * Math.abs(previousFeedbackDeltaSolution.get(1, 0)));

      feedbackWeight.set(0, 0, xWeight);
      feedbackWeight.set(1, 1, yWeight);
   }

   public void setFootstepRegularizationWeight(double regularizationWeight)
   {
      CommonOps.setIdentity(footstepRegularizationWeight);
      CommonOps.scale(regularizationWeight, footstepRegularizationWeight);

      hasFootstepRegularizationTerm = true;
   }

   public void resetFootstepRegularization(int footstepIndex, FramePoint2d previousFootstepLocation)
   {
      previousFootstepLocation.changeFrame(worldFrame);
      previousFootstepLocations.get(footstepIndex).set(0, 0, previousFootstepLocation.getX());
      previousFootstepLocations.get(footstepIndex).set(1, 0, previousFootstepLocation.getY());
   }

   public void resetFeedbackRegularization()
   {
      previousFeedbackDeltaSolution.zero();
   }

   private final FramePoint tmpPoint = new FramePoint();
   public void setSupportPolygonVertex(int vertexIndex, FramePoint2d vertexLocation, ReferenceFrame frame, double xBuffer, double yBuffer)
   {
      tmpPoint.setToZero(frame);
      tmpPoint.setXY(vertexLocation);

      if (tmpPoint.getX() > 0.0)
         tmpPoint.setX(tmpPoint.getX() + xBuffer);
      else
         tmpPoint.setX(tmpPoint.getX() - xBuffer);

      if (tmpPoint.getY() > 0.0)
         tmpPoint.setY(tmpPoint.getY() + yBuffer);
      else
         tmpPoint.setY(tmpPoint.getY() - yBuffer);

      tmpPoint.changeFrame(worldFrame);

      vertexLocations.get(vertexIndex).set(0, 0, tmpPoint.getX());
      vertexLocations.get(vertexIndex).set(1, 0, tmpPoint.getY());
   }

   public void compute(FramePoint2d finalICPRecursion, FramePoint2d cmpOffsetRecursionEffect, FramePoint2d currentICP, FramePoint2d perfectCMP,
                       FramePoint2d stanceCMPProjection, double currentStateProjection)
   {
      finalICPRecursion.changeFrame(worldFrame);
      currentICP.changeFrame(worldFrame);
      perfectCMP.changeFrame(worldFrame);
      stanceCMPProjection.changeFrame(worldFrame);

      if (cmpOffsetRecursionEffect != null)
         cmpOffsetRecursionEffect.changeFrame(worldFrame);

      this.finalICPRecursion.set(0, 0, finalICPRecursion.getX());
      this.finalICPRecursion.set(1, 0, finalICPRecursion.getY());

      this.currentICP.set(0, 0, currentICP.getX());
      this.currentICP.set(1, 0, currentICP.getY());

      this.perfectCMP.set(0, 0, perfectCMP.getX());
      this.perfectCMP.set(1, 0, perfectCMP.getY());

      this.stanceCMPProjection.set(0, 0, stanceCMPProjection.getX());
      this.stanceCMPProjection.set(1, 0, stanceCMPProjection.getY());

      this.currentStateProjection = currentStateProjection;

      if (useTwoCMPs)
      {
         this.cmpOffsetRecursionEffect.set(0, 0, cmpOffsetRecursionEffect.getX());
         this.cmpOffsetRecursionEffect.set(1, 0, cmpOffsetRecursionEffect.getY());
      }

      if (useFeedback)
      {
         addFeedbackTask();
         addDynamicRelaxationTask();

         if (hasFeedbackRegularizationTerm)
            addFeedbackRegularizationTask();

         if (numberOfVertices > 0)
            addCMPLocationConstraint();
      }

      if (useStepAdjustment)
      {
         addStepAdjustmentTask();

         if (hasFootstepRegularizationTerm)
            addFootstepRegularizationTask();
      }

      addDynamicConstraint();

      assembleTotalProblem();

      solve(solution);

      extractLagrangeMultiplierSolution(lagrangeMultiplierSolution);
      extractFreeVariableSolution(freeVariableSolution);
      if (useStepAdjustment)
      {
         extractFootstepSolutions(footstepLocationSolution);
         setPreviousFootstepSolution(footstepLocationSolution);
      }
      if (useFeedback)
      {
         extractFeedbackDeltaSolution(feedbackDeltaSolution);
         extractDynamicRelaxationSolution(dynamicRelaxationSolution);
         setPreviousFeedbackDeltaSolution(feedbackDeltaSolution);
      }

      computeFeedbackLocation();

      computeCostToGo();
   }

   protected void addFeedbackTask()
   {
      MatrixTools.setMatrixBlock(feedbackCost_H, 0, 0, feedbackWeight, 0, 0, 2, 2, 1.0);
      feedbackCost_h.zero();

      MatrixTools.addMatrixBlock(solverInput_H, feedbackCMPIndex, feedbackCMPIndex, feedbackCost_H, 0, 0, 2, 2, 1.0);
      MatrixTools.addMatrixBlock(solverInput_h, feedbackCMPIndex, 0, feedbackCost_h, 0, 0, 2, 1, 1.0);
   }

   protected void addDynamicRelaxationTask()
   {
      MatrixTools.setMatrixBlock(dynamicRelaxationCost_H, 0, 0, dynamicRelaxationWeight, 0, 0, 2, 2, 1.0);
      dynamicRelaxationCost_h.zero();

      MatrixTools.addMatrixBlock(solverInput_H, dynamicRelaxtionIndex, dynamicRelaxtionIndex, dynamicRelaxationCost_H, 0, 0, 2, 2, 1.0);
      MatrixTools.addMatrixBlock(solverInput_h, dynamicRelaxtionIndex, 0, dynamicRelaxationCost_h, 0, 0, 2, 1, 1.0);
   }

   private final DenseMatrix64F tmpFootstepObjective = new DenseMatrix64F(2, 1);
   protected void addStepAdjustmentTask()
   {
      footstepObjectiveVector.zero();
      for (int i = 0; i < numberOfFootstepsToConsider; i++)
      {
         MatrixTools.setMatrixBlock(footstepCost_H, 2 * i, 2 * i, footstepWeights.get(i), 0, 0, 2, 2, 1.0);

         tmpFootstepObjective.zero();
         tmpFootstepObjective.set(referenceFootstepLocations.get(i));
         CommonOps.mult(footstepWeights.get(i), tmpFootstepObjective, tmpFootstepObjective);
         CommonOps.multTransA(referenceFootstepLocations.get(i), tmpFootstepObjective, footstepRegularizationResidualCost);

         MatrixTools.setMatrixBlock(footstepCost_h, 2 * i, 0, tmpFootstepObjective, 0, 0, 2, 1, 1.0);
         CommonOps.addEquals(solverInputResidualCost, footstepRegularizationResidualCost);

         MatrixTools.setMatrixBlock(footstepObjectiveVector, 2 * i, 0, referenceFootstepLocations.get(i), 0, 0, 2, 1, 1.0);
      }

      MatrixTools.addMatrixBlock(solverInput_H, 0, 0, footstepCost_H, 0, 0, numberOfFootstepVariables, numberOfFootstepVariables, 1.0);
      MatrixTools.addMatrixBlock(solverInput_h, 0, 0, footstepCost_h, 0, 0, numberOfFootstepVariables, 1, 1.0);

      if (localDebug)
      {
         footstepReferenceLocation.set(footstepObjectiveVector);
         footstepH.set(footstepCost_H);
         footsteph.set(footstepCost_h);
      }
   }

   private final DenseMatrix64F tmpObjective = new DenseMatrix64F(2, 1);
   protected void addFeedbackRegularizationTask()
   {
      MatrixTools.setMatrixBlock(feedbackRegularizationCost_H, 0, 0, feedbackRegularizationWeight, 0, 0, 2, 2, 1.0);

      tmpObjective.zero();
      tmpObjective.set(previousFeedbackDeltaSolution);
      CommonOps.mult(feedbackRegularizationWeight, tmpObjective, tmpObjective);
      CommonOps.multTransA(previousFeedbackDeltaSolution, tmpObjective, feedbackRegularizationResidualCost);

      MatrixTools.setMatrixBlock(feedbackRegularizationCost_h, 0, 0, tmpObjective, 0, 0, 2, 1, 1.0);
      CommonOps.addEquals(solverInputResidualCost, feedbackRegularizationResidualCost);

      MatrixTools.addMatrixBlock(solverInput_H, numberOfFootstepVariables, numberOfFootstepVariables, feedbackRegularizationCost_H, 0, 0, feedbackCMPIndex, feedbackCMPIndex, 1.0);
      MatrixTools.addMatrixBlock(solverInput_h, numberOfFootstepVariables, 1, feedbackRegularizationCost_h, 0, 0, feedbackCMPIndex, 1, 1.0);
   }

   protected void addFootstepRegularizationTask()
   {
      for (int i = 0; i < numberOfFootstepsToConsider; i++)
      {
         MatrixTools.setMatrixBlock(footstepRegularizationCost_H, 2 * i, 2 * i, footstepRegularizationWeight, 0, 0, 2, 2, 1.0);

         tmpObjective.zero();
         tmpObjective.set(previousFootstepLocations.get(i));
         CommonOps.mult(footstepRegularizationWeight, tmpObjective, tmpObjective);
         CommonOps.multTransA(previousFootstepLocations.get(i), tmpObjective, footstepRegularizationResidualCost);

         MatrixTools.setMatrixBlock(footstepRegularizationCost_h, 2 * i, 0, tmpObjective, 0, 0, 2, 1, 1.0);
         CommonOps.addEquals(solverInputResidualCost, footstepRegularizationResidualCost);
      }

      MatrixTools.addMatrixBlock(solverInput_H, 0, 0, footstepRegularizationCost_H, 0, 0, numberOfFootstepVariables, numberOfFootstepVariables, 1.0);
      MatrixTools.addMatrixBlock(solverInput_h, 0, 0, footstepRegularizationCost_h, 0, 0, numberOfFootstepVariables, 1, 1.0);
   }

   private void addCMPLocationConstraint()
   {
      computeCMPLocationConstraint();

      CommonOps.setIdentity(stanceCMPCost_G);
      CommonOps.scale(betaSmoothing, stanceCMPCost_G);

      MatrixTools.addMatrixBlock(solverInput_H, numberOfFreeVariables, numberOfFreeVariables, stanceCMPCost_G, 0, 0, numberOfVertexVariables, numberOfVertexVariables, 1.0);

      MatrixTools.addMatrixBlock(solverInput_Aeq, feedbackCMPIndex, 2, stanceCMP_Aeq, 0, 0, 2 + numberOfVertexVariables, 4, 1.0);
      MatrixTools.addMatrixBlock(solverInput_beq, 2, 0, stanceCMP_beq, 0, 0, 4, 1, 1.0);

      MatrixTools.setMatrixBlock(solverInput_Aineq, cmpConstraintIndex, 0, stanceCMP_Aineq, 0, 0, numberOfVertexVariables, numberOfVertexVariables, 1.0);
      MatrixTools.setMatrixBlock(solverInput_bineq, 0, 0, stanceCMP_bineq, 0, 0, numberOfVertexVariables, 1, 1.0);
   }

   private void computeCMPLocationConstraint()
   {
      // set up location constraints
      stanceCMPDynamics_Aeq.set(0, 0, -1.0);
      stanceCMPDynamics_Aeq.set(1, 1, -1.0);

      int offset = 2;
      for (int i = 0; i < numberOfVertices; i++)
      {
         stanceCMPDynamics_Aeq.set(offset + i, 0, vertexLocations.get(i).get(0, 0));
         stanceCMPDynamics_Aeq.set(offset + numberOfVertices + i, 1, vertexLocations.get(i).get(1, 0));

         stanceCMPSum_Aeq.set(i, 0, 1.0);
         stanceCMPSum_Aeq.set(numberOfVertices + i, 1, 1.0);

         stanceCMP_Aineq.set(i, i, -1.0);
         stanceCMP_Aineq.set(numberOfVertices + i, numberOfVertices + i, -1.0);
      }

      stanceCMPDynamics_beq.set(perfectCMP);

      stanceCMPSum_beq.set(0, 0, 1.0);
      stanceCMPSum_beq.set(1, 0, 1.0);

      MatrixTools.setMatrixBlock(stanceCMP_Aeq, 0, 0, stanceCMPDynamics_Aeq, 0, 0, (cmpConstraintIndex - dynamicRelaxtionIndex) + numberOfVertexVariables, 2, 1.0);
      MatrixTools.setMatrixBlock(stanceCMP_Aeq, 2, 2, stanceCMPSum_Aeq, 0, 0, numberOfVertexVariables, 2, 1.0);

      MatrixTools.setMatrixBlock(stanceCMP_beq, 0, 0, stanceCMPDynamics_beq, 0, 0, 2, 1, 1.0);
      MatrixTools.setMatrixBlock(stanceCMP_beq, 2, 0, stanceCMPSum_beq, 0, 0, 2, 1, 1.0);
   }

   private void addDynamicConstraint()
   {
      computeDynamicConstraint();

      MatrixTools.addMatrixBlock(solverInput_Aeq, 0, 0, dynamics_Aeq, 0, 0, numberOfFreeVariables, 2, 1.0);
      MatrixTools.addMatrixBlock(solverInput_beq, 0, 0, dynamics_beq, 0, 0, 2, 1, 1.0);
   }

   private void computeDynamicConstraint()
   {
      if (useFeedback)
         addFeedbackToDynamicConstraint(currentStateProjection);
      if (useStepAdjustment)
         addFootstepRecursionsToDynamicConstraint();

      CommonOps.scale(currentStateProjection, currentICP);

      CommonOps.subtractEquals(currentICP, finalICPRecursion);
      CommonOps.subtractEquals(currentICP, stanceCMPProjection);

      if (useTwoCMPs)
         CommonOps.subtractEquals(currentICP, cmpOffsetRecursionEffect);

      MatrixTools.setMatrixBlock(dynamics_beq, 0, 0, currentICP, 0, 0, 2, 1, 1.0);
   }

   private void addFeedbackToDynamicConstraint(double currentStateProjection)
   {
      CommonOps.setIdentity(identity);

      CommonOps.invert(feedbackGain);
      CommonOps.scale(currentStateProjection, feedbackGain);

      MatrixTools.addMatrixBlock(dynamics_Aeq, feedbackCMPIndex, 0, feedbackGain, 0, 0, 2, 2, 1.0);
      MatrixTools.addMatrixBlock(dynamics_Aeq, dynamicRelaxtionIndex, 0, identity, 0, 0, 2, 2, 1.0);
   }

   private void addFootstepRecursionsToDynamicConstraint()
   {
      for (int i = 0; i < numberOfFootstepsToConsider; i++)
      {
         MatrixTools.addMatrixBlock(dynamics_Aeq, 2 * i, 0, footstepRecursionMutlipliers.get(i), 0, 0, 2, 2, 1.0);
      }
   }

   private void assembleTotalProblem()
   {
      CommonOps.transpose(solverInput_Aeq, solverInput_AeqTrans);
      CommonOps.transpose(solverInput_Aineq, solverInput_AineqTrans);
   }

   private void solve(DenseMatrix64F solutionToPack)
   {
      CommonOps.scale(-1.0, solverInput_h);

      activeSetSolver.clear();

      if (localDebug)
      {
         yoWeightG.set(solverInput_H);
         yoWeightg.set(solverInput_h);
         yoDynamics_Aeq.set(dynamics_Aeq);
         yoDynamics_beq.set(dynamics_beq);
         yoSolver_Aeq.set(solverInput_Aeq);
         yoSolver_beq.set(solverInput_beq);
         yoStanceCMPDynamics_Aeq.set(stanceCMPDynamics_Aeq);
         yoStanceCMPDynamics_beq.set(stanceCMPDynamics_beq);
         yoStanceCMPSum_Aeq.set(stanceCMPSum_Aeq);
         yoStanceCMPSum_beq.set(stanceCMPSum_beq);
      }

      activeSetSolver.setQuadraticCostFunction(solverInput_H, solverInput_h, 0.0);
      activeSetSolver.setLinearEqualityConstraints(solverInput_AeqTrans, solverInput_beq);
      activeSetSolver.setLinearInequalityConstraints(solverInput_AineqTrans, solverInput_bineq);

      activeSetSolver.solve(solutionToPack);

      if (MatrixTools.containsNaN(solutionToPack))
      {
         PrintTools.debug("number of steps = " + numberOfFootstepsToConsider);
         PrintTools.debug("solverInput_H = " + solverInput_H);
         PrintTools.debug("solverInput_h = " + solverInput_h);
         throw new RuntimeException("had a NaN");
      }
   }

   private void extractFootstepSolutions(DenseMatrix64F footstepLocationSolutionToPack)
   {
      MatrixTools.setMatrixBlock(footstepLocationSolutionToPack, 0, 0, solution, 0, 0, numberOfFootstepVariables, 1, 1.0);
   }

   private void extractFeedbackDeltaSolution(DenseMatrix64F feedbackSolutionToPack)
   {
      MatrixTools.setMatrixBlock(feedbackSolutionToPack, 0, 0, solution, feedbackCMPIndex, 0, 2, 1, 1.0);
   }

   private void extractDynamicRelaxationSolution(DenseMatrix64F dynamicRelaxationSolutionToPack)
   {
      MatrixTools.setMatrixBlock(dynamicRelaxationSolutionToPack, 0, 0, solution, dynamicRelaxtionIndex, 0, 2, 1, 1.0);
   }

   private void extractLagrangeMultiplierSolution(DenseMatrix64F lagrangeMultiplierSolutionToPack)
   {
      MatrixTools.setMatrixBlock(lagrangeMultiplierSolutionToPack, 0, 0, solution, lagrangeMultiplierIndex, 0, numberOfLagrangeMultipliers, 1, 1.0);
   }

   private void extractFreeVariableSolution(DenseMatrix64F freeVariableSolution)
   {
      MatrixTools.setMatrixBlock(freeVariableSolution, 0, 0, solution, 0, 0, numberOfFreeVariables, 1, 1.0);
   }

   private void setPreviousFootstepSolution(DenseMatrix64F footstepLocationSolution)
   {
      for (int i = 0; i < numberOfFootstepsToConsider; i++)
         MatrixTools.setMatrixBlock(previousFootstepLocations.get(i), 0, 0, footstepLocationSolution, 2 * i, 0, 2, 1, 1.0);
   }

   private void setPreviousFeedbackDeltaSolution(DenseMatrix64F feedbackDeltaSolution)
   {
      MatrixTools.setMatrixBlock(previousFeedbackDeltaSolution, 0, 0, feedbackDeltaSolution, 0, 0, 2, 1, 1.0);
   }

   private void computeFeedbackLocation()
   {
      feedbackLocation.set(perfectCMP);
      CommonOps.addEquals(feedbackLocation, feedbackDeltaSolution);
   }

   private final DenseMatrix64F tmpCostScalar = new DenseMatrix64F(1, 1);
   private void computeCostToGo()
   {
      costToGo.zero();
      footstepCostToGo.zero();
      footstepRegularizationCostToGo.zero();
      feedbackCostToGo.zero();
      feedbackRegularizationCostToGo.zero();
      dynamicRelaxationCostToGo.zero();

      tmpCost.zero();
      tmpFootstepCost.zero();
      tmpFeedbackCost.zero();

      tmpCost.reshape(numberOfFreeVariables + numberOfVertexVariables, 1);
      tmpFootstepCost.reshape(numberOfFootstepVariables, 1);
      tmpFeedbackCost.reshape(2, 1);

      // quadratic cost;
      CommonOps.mult(solverInput_H, freeVariableSolution, tmpCost);
      CommonOps.multTransA(freeVariableSolution, tmpCost, costToGo);

      CommonOps.mult(footstepCost_H, footstepLocationSolution, tmpFootstepCost);
      CommonOps.multTransA(footstepLocationSolution, tmpFootstepCost, footstepCostToGo);

      CommonOps.mult(footstepRegularizationCost_H, footstepLocationSolution, tmpFootstepCost);
      CommonOps.multTransA(footstepLocationSolution, tmpFootstepCost, footstepRegularizationCostToGo);

      CommonOps.mult(feedbackCost_H, feedbackDeltaSolution, tmpFeedbackCost);
      CommonOps.multTransA(feedbackDeltaSolution, tmpFeedbackCost, feedbackCostToGo);

      CommonOps.mult(dynamicRelaxationCost_H, dynamicRelaxationSolution, tmpFeedbackCost);
      CommonOps.multTransA(dynamicRelaxationSolution, tmpFeedbackCost, dynamicRelaxationCostToGo);

      CommonOps.mult(feedbackRegularizationCost_H, feedbackDeltaSolution, tmpFeedbackCost);
      CommonOps.multTransA(feedbackDeltaSolution, tmpFeedbackCost, feedbackRegularizationCostToGo);

      // linear cost
      CommonOps.multTransA(solverInput_h, freeVariableSolution, tmpCostScalar);
      CommonOps.addEquals(costToGo, tmpCostScalar);

      CommonOps.multTransA(footstepCost_h, footstepLocationSolution, tmpCostScalar);
      CommonOps.addEquals(footstepCostToGo, tmpCostScalar);

      CommonOps.multTransA(footstepRegularizationCost_h, footstepLocationSolution, tmpCostScalar);
      CommonOps.addEquals(footstepRegularizationCostToGo, tmpCostScalar);

      CommonOps.multTransA(feedbackCost_h, feedbackDeltaSolution, tmpCostScalar);
      CommonOps.addEquals(feedbackCostToGo, tmpCostScalar);

      CommonOps.multTransA(feedbackRegularizationCost_h, feedbackDeltaSolution, tmpCostScalar);
      CommonOps.addEquals(feedbackRegularizationCostToGo, tmpCostScalar);

      // residual cost
      CommonOps.addEquals(costToGo, solverInputResidualCost);
      CommonOps.addEquals(footstepCostToGo, footstepResidualCost);
      CommonOps.addEquals(footstepRegularizationCostToGo, footstepRegularizationResidualCost);
      CommonOps.addEquals(feedbackCostToGo, feedbackResidualCost);
      CommonOps.addEquals(feedbackRegularizationCostToGo, feedbackRegularizationResidualCost);
      CommonOps.addEquals(dynamicRelaxationCostToGo, dynamicRelaxationResidualCost);
   }

   public void getFootstepSolutionLocation(int footstepIndex, FramePoint2d footstepLocationToPack)
   {
      footstepLocationToPack.setToZero(worldFrame);
      footstepLocationToPack.setX(footstepLocationSolution.get(2 * footstepIndex, 0));
      footstepLocationToPack.setY(footstepLocationSolution.get(2 * footstepIndex + 1, 0));
   }

   public void getCMPFeedbackDifference(FrameVector2d cmpFeedbackDifferenceToPack)
   {
      cmpFeedbackDifferenceToPack.setToZero(worldFrame);
      cmpFeedbackDifferenceToPack.setX(feedbackDeltaSolution.get(0, 0));
      cmpFeedbackDifferenceToPack.setY(feedbackDeltaSolution.get(1, 0));
   }

   public void getCMPFeedback(FramePoint2d cmpFeedbackToPack)
   {
      cmpFeedbackToPack.setToZero(worldFrame);
      cmpFeedbackToPack.setX(feedbackLocation.get(0, 0));
      cmpFeedbackToPack.setY(feedbackLocation.get(1, 0));
   }

   public double getCostToGo()
   {
      return costToGo.get(0, 0);
   }

   public double getFootstepCostToGo()
   {
      return footstepCostToGo.get(0, 0);
   }

   public double getFootstepRegularizationCostToGo()
   {
      return footstepRegularizationCostToGo.get(0, 0);
   }

   public double getFeedbackCostToGo()
   {
      return feedbackCostToGo.get(0, 0);
   }

   public double getFeedbackRegularizationCostToGo()
   {
      return feedbackRegularizationCostToGo.get(0, 0);
   }

   public double getDynamicRelaxationCostToGo()
   {
      return dynamicRelaxationCostToGo.get(0, 0);
   }
}
