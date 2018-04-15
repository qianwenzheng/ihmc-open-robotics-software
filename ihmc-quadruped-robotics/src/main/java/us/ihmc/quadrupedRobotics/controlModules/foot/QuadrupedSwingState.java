package us.ihmc.quadrupedRobotics.controlModules.foot;

import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.PointFeedbackControlCommand;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControllerToolbox;
import us.ihmc.quadrupedRobotics.planning.YoQuadrupedTimedStep;
import us.ihmc.quadrupedRobotics.planning.trajectory.OneWaypointSwingGenerator;
import us.ihmc.robotics.dataStructures.parameters.FrameParameterVector3D;
import us.ihmc.robotics.math.filters.GlitchFilteredYoBoolean;
import us.ihmc.robotics.math.trajectories.MultipleWaypointsBlendedPositionTrajectoryGenerator;
import us.ihmc.robotics.math.trajectories.waypoints.FrameEuclideanTrajectoryPoint;
import us.ihmc.robotics.math.trajectories.waypoints.MultipleWaypointsPositionTrajectoryGenerator;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.screwTheory.MovingReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.trajectories.TrajectoryType;
import us.ihmc.robotics.trajectories.providers.CurrentRigidBodyStateProvider;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoFramePoint3D;
import us.ihmc.yoVariables.variable.YoFrameVector3D;

public class QuadrupedSwingState extends QuadrupedFootState
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final OneWaypointSwingGenerator swingTrajectoryWaypointCalculator;
   private final MultipleWaypointsBlendedPositionTrajectoryGenerator blendedSwingTrajectory;

   private final FrameEuclideanTrajectoryPoint tempPositionTrajectoryPoint = new FrameEuclideanTrajectoryPoint();

   private final CurrentRigidBodyStateProvider currentStateProvider;

   private final FramePoint3D initialPosition = new FramePoint3D();
   private final FrameVector3D initialLinearVelocity = new FrameVector3D();

   private final FramePoint3D finalPosition = new FramePoint3D();
   private final FrameVector3D finalLinearVelocity = new FrameVector3D();

   private final GlitchFilteredYoBoolean touchdownTrigger;

   private final FramePoint3D desiredPosition = new FramePoint3D();
   private final FrameVector3D desiredVelocity = new FrameVector3D();

   private final YoFramePoint3D desiredSolePosition;
   private final YoFrameVector3D desiredSoleLinearVelocity;

   private final QuadrupedFootControlModuleParameters parameters;

   private final YoBoolean stepCommandIsValid;
   private final YoDouble timestamp;
   private final YoQuadrupedTimedStep currentStepCommand;

   private final PointFeedbackControlCommand feedbackControlCommand = new PointFeedbackControlCommand();

   private final MovingReferenceFrame soleFrame;

   private final QuadrupedControllerToolbox controllerToolbox;
   private final RobotQuadrant robotQuadrant;

   private boolean swingIsDone;

   private final FrameVector3DReadOnly touchdownVelocity;

   public QuadrupedSwingState(RobotQuadrant robotQuadrant, QuadrupedControllerToolbox controllerToolbox, YoBoolean stepCommandIsValid,
                              YoQuadrupedTimedStep currentStepCommand, YoGraphicsListRegistry graphicsListRegistry, YoVariableRegistry registry)
   {
      this.robotQuadrant = robotQuadrant;
      this.controllerToolbox = controllerToolbox;

      this.stepCommandIsValid = stepCommandIsValid;
      this.timestamp = controllerToolbox.getRuntimeEnvironment().getRobotTimestamp();
      this.currentStepCommand = currentStepCommand;

      String namePrefix = robotQuadrant.getPascalCaseName();

      Vector3D defaultTouchdownVelocity = new Vector3D(0.0, 0.0, 0.0);
      touchdownVelocity = new FrameParameterVector3D(namePrefix + "TouchdownVelocity", ReferenceFrame.getWorldFrame(), defaultTouchdownVelocity, registry);

      this.parameters = controllerToolbox.getFootControlModuleParameters();

      soleFrame = controllerToolbox.getReferenceFrames().getSoleFrame(robotQuadrant);

      swingTrajectoryWaypointCalculator = new OneWaypointSwingGenerator(this.robotQuadrant.getPascalCaseName(), 0.5, 0.04, 0.3, registry, graphicsListRegistry);

      MultipleWaypointsPositionTrajectoryGenerator baseTrajectory = new MultipleWaypointsPositionTrajectoryGenerator(this.robotQuadrant.getPascalCaseName(),
                                                                                                                     true, worldFrame, registry);
      blendedSwingTrajectory = new MultipleWaypointsBlendedPositionTrajectoryGenerator(this.robotQuadrant.getPascalCaseName(), baseTrajectory, worldFrame,
                                                                                       registry);

      currentStateProvider = new CurrentRigidBodyStateProvider(soleFrame);

      this.touchdownTrigger = new GlitchFilteredYoBoolean(this.robotQuadrant.getCamelCaseName() + "TouchdownTriggered", registry,
                                                          QuadrupedFootControlModuleParameters.getDefaultTouchdownTriggerWindow());

      RigidBody foot = controllerToolbox.getFullRobotModel().getFoot(robotQuadrant);
      FramePoint3D currentPosition = new FramePoint3D(soleFrame);
      currentPosition.changeFrame(foot.getBodyFixedFrame());

      feedbackControlCommand.set(controllerToolbox.getFullRobotModel().getBody(), foot);
      feedbackControlCommand.setBodyFixedPointToControl(currentPosition);

      desiredSolePosition = new YoFramePoint3D(namePrefix + "DesiredSolePositionInWorld", worldFrame, registry);
      desiredSoleLinearVelocity = new YoFrameVector3D(namePrefix + "DesiredSoleLinearVelocityInWorld", worldFrame, registry);
   }

   @Override
   public void onEntry()
   {
      controllerToolbox.getFootContactState(robotQuadrant).clear();

      // initialize swing trajectory
      currentStateProvider.getPosition(initialPosition);
      currentStateProvider.getLinearVelocity(initialLinearVelocity);
      initialPosition.changeFrame(worldFrame);
      initialLinearVelocity.changeFrame(worldFrame);

      currentStepCommand.getGoalPosition(finalPosition);
      finalPosition.changeFrame(worldFrame);
      finalPosition.addZ(parameters.getStepGoalOffsetZParameter());

      fillAndInitializeTrajectories();

      touchdownTrigger.set(false);
      swingIsDone = false;
   }

   @Override
   public void doAction(double timeInState)
   {
      double currentTime = timestamp.getDoubleValue();
      double touchDownTime = currentStepCommand.getTimeInterval().getEndTime();

      blendForStepAdjustment(timeInState);

      blendedSwingTrajectory.compute(timeInState);
      blendedSwingTrajectory.getPosition(desiredPosition);
      blendedSwingTrajectory.getVelocity(desiredVelocity);

      desiredSolePosition.setMatchingFrame(desiredPosition);
      desiredSoleLinearVelocity.setMatchingFrame(desiredVelocity);

      // Detect early touch-down.
      FrameVector3D soleForceEstimate = controllerToolbox.getTaskSpaceEstimates().getSoleVirtualForce(robotQuadrant);
      soleForceEstimate.changeFrame(worldFrame);
      double pressureEstimate = -soleForceEstimate.getZ();
      double normalizedTimeInSwing = timeInState / currentStepCommand.getTimeInterval().getDuration();
      if (normalizedTimeInSwing > 0.5)
      {
         touchdownTrigger.update(pressureEstimate > parameters.getTouchdownPressureLimitParameter());
      }

      feedbackControlCommand.set(desiredPosition, desiredVelocity);
      feedbackControlCommand.setGains(parameters.getSolePositionGains());

      // Trigger support phase.
      if (currentTime >= touchDownTime)
      {
         if (stepTransitionCallback != null)
         {
            stepTransitionCallback.onTouchDown(robotQuadrant);
         }
         swingIsDone = true;
      }
   }

   private void fillAndInitializeTrajectories()
   {
      blendedSwingTrajectory.clearTrajectory(worldFrame);
      blendedSwingTrajectory.appendPositionWaypoint(0.0, initialPosition, initialLinearVelocity);

      finalLinearVelocity.setIncludingFrame(touchdownVelocity);

      swingTrajectoryWaypointCalculator.setInitialConditions(initialPosition, initialLinearVelocity);
      swingTrajectoryWaypointCalculator.setFinalConditions(finalPosition, finalLinearVelocity);
      swingTrajectoryWaypointCalculator.setStepTime(currentStepCommand.getTimeInterval().getDuration());
      swingTrajectoryWaypointCalculator.setTrajectoryType(TrajectoryType.DEFAULT);
      swingTrajectoryWaypointCalculator.setSwingHeight(currentStepCommand.getGroundClearance());
      swingTrajectoryWaypointCalculator.initialize();

      for (int i = 0; i < swingTrajectoryWaypointCalculator.getNumberOfWaypoints(); i++)
      {
         swingTrajectoryWaypointCalculator.getWaypointData(i, tempPositionTrajectoryPoint);
         blendedSwingTrajectory.appendPositionWaypoint(tempPositionTrajectoryPoint);
      }

      blendedSwingTrajectory.appendPositionWaypoint(currentStepCommand.getTimeInterval().getDuration(), finalPosition, finalLinearVelocity);

      blendedSwingTrajectory.initializeTrajectory();
      blendedSwingTrajectory.initialize();
   }

   private void blendForStepAdjustment(double timeInState)
   {
      double currentTime = timestamp.getDoubleValue();
      double duration = currentStepCommand.getTimeInterval().getDuration();
      double touchDownTime = currentStepCommand.getTimeInterval().getEndTime();

      // Compute current goal position.
      currentStepCommand.getGoalPosition(finalPosition);
      finalPosition.changeFrame(worldFrame);
      finalPosition.addZ(parameters.getStepGoalOffsetZParameter());

      // Compute swing trajectory.
      if (touchDownTime - currentTime > parameters.getMinimumStepAdjustmentTimeParameter())
      {
         blendedSwingTrajectory.clear();
         blendedSwingTrajectory.blendFinalConstraint(finalPosition, duration, duration - timeInState);
         blendedSwingTrajectory.initialize();
      }
   }

   @Override
   public QuadrupedFootControlModule.FootEvent fireEvent(double timeInState)
   {
      if (swingIsDone)
         return QuadrupedFootControlModule.FootEvent.TIMEOUT;
      if (touchdownTrigger.getBooleanValue())
         return QuadrupedFootControlModule.FootEvent.LOADED;

      return null;
   }

   @Override
   public void onExit()
   {
      stepCommandIsValid.set(false);

      swingIsDone = false;

      desiredSolePosition.setToNaN();
      desiredSoleLinearVelocity.setToNaN();
      swingTrajectoryWaypointCalculator.hideVisualization();
   }

   @Override
   public PointFeedbackControlCommand getFeedbackControlCommand()
   {
      return feedbackControlCommand;
   }

   @Override
   public FeedbackControlCommand<?> createFeedbackControlTemplate()
   {
      return feedbackControlCommand;
   }
}
