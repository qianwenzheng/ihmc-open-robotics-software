package us.ihmc.avatar;

import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.factory.AvatarSimulation;
import us.ihmc.avatar.factory.AvatarSimulationFactory;
import us.ihmc.avatar.initialSetup.DRCGuiInitialSetup;
import us.ihmc.avatar.initialSetup.DRCRobotInitialSetup;
import us.ihmc.avatar.initialSetup.DRCSCSInitialSetup;
import us.ihmc.commonWalkingControlModules.configurations.ICPWithTimeFreezingPlannerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.HeadingAndVelocityEvaluationScriptParameters;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.AbstractMomentumBasedControllerFactory;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ContactableBodiesFactory;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.NewMomentumBasedControllerFactory;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.WalkingProvider;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.newHighLevelStates.StandPrepSetpoints;
import us.ihmc.graphicsDescription.HeightMap;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.NewHighLevelControllerStates;
import us.ihmc.robotics.controllers.ControllerFailureListener;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.sensorProcessing.parameters.DRCRobotSensorInformation;
import us.ihmc.simulationconstructionset.HumanoidFloatingRootJointRobot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.wholeBodyController.RobotContactPointParameters;

public abstract class NewDRCFlatGroundWalkingTrack
{
   // looking for CREATE_YOVARIABLE_WALKING_PROVIDERS ?  use the second constructor and pass in WalkingProvider = YOVARIABLE_PROVIDER

   private final AvatarSimulation avatarSimulation;

   public NewDRCFlatGroundWalkingTrack(DRCRobotInitialSetup<HumanoidFloatingRootJointRobot> robotInitialSetup, DRCGuiInitialSetup guiInitialSetup, DRCSCSInitialSetup scsInitialSetup,
                                       boolean useVelocityAndHeadingScript, boolean cheatWithGroundHeightAtForFootstep, DRCRobotModel model)
   {
      this(robotInitialSetup, guiInitialSetup, scsInitialSetup, useVelocityAndHeadingScript, cheatWithGroundHeightAtForFootstep, model,
            WalkingProvider.VELOCITY_HEADING_COMPONENT, new HeadingAndVelocityEvaluationScriptParameters()); // should always be committed as VELOCITY_HEADING_COMPONENT
   }

   public NewDRCFlatGroundWalkingTrack(DRCRobotInitialSetup<HumanoidFloatingRootJointRobot> robotInitialSetup, DRCGuiInitialSetup guiInitialSetup, DRCSCSInitialSetup scsInitialSetup,
                                       boolean useVelocityAndHeadingScript, boolean cheatWithGroundHeightAtForFootstep, DRCRobotModel model, WalkingProvider walkingProvider, HeadingAndVelocityEvaluationScriptParameters walkingScriptParameters)
   {
      //    scsInitialSetup = new DRCSCSInitialSetup(TerrainType.FLAT);

      double dt = scsInitialSetup.getDT();
      int recordFrequency = (int) Math.round(model.getControllerDT() / dt);
      if (recordFrequency < 1)
         recordFrequency = 1;
      scsInitialSetup.setRecordFrequency(recordFrequency);

      StandPrepSetpoints standPrepSetpoints = model.getStandPrepSetpoints();
      WalkingControllerParameters walkingControllerParameters = model.getWalkingControllerParameters();
      RobotContactPointParameters contactPointParameters = model.getContactPointParameters();
      ICPWithTimeFreezingPlannerParameters capturePointPlannerParameters = model.getCapturePointPlannerParameters();
      ContactableBodiesFactory contactableBodiesFactory = contactPointParameters.getContactableBodiesFactory();
      DRCRobotSensorInformation sensorInformation = model.getSensorInformation();
      SideDependentList<String> feetForceSensorNames = sensorInformation.getFeetForceSensorNames();
      SideDependentList<String> feetContactSensorNames = sensorInformation.getFeetContactSensorNames();
      SideDependentList<String> wristForceSensorNames = sensorInformation.getWristForceSensorNames();

      AbstractMomentumBasedControllerFactory controllerFactory = getControllerFactory(contactableBodiesFactory, feetForceSensorNames, feetContactSensorNames,
                                                                                      wristForceSensorNames, walkingControllerParameters, capturePointPlannerParameters,
                                                                                      standPrepSetpoints, NewHighLevelControllerStates.WALKING_STATE,
                                                                                      NewHighLevelControllerStates.DO_NOTHING_STATE);
      controllerFactory.setHeadingAndVelocityEvaluationScriptParameters(walkingScriptParameters);



      HeightMap heightMapForFootstepZ = null;
      if (cheatWithGroundHeightAtForFootstep)
      {
         heightMapForFootstepZ = scsInitialSetup.getHeightMap();
      }

      controllerFactory.createComponentBasedFootstepDataMessageGenerator(useVelocityAndHeadingScript, heightMapForFootstepZ);

      AvatarSimulationFactory avatarSimulationFactory = new AvatarSimulationFactory();
      avatarSimulationFactory.setRobotModel(model);
      avatarSimulationFactory.setMomentumBasedControllerFactory(controllerFactory);
      avatarSimulationFactory.setMomentumBasedControllerFactory(controllerFactory);
      avatarSimulationFactory.setCommonAvatarEnvironment(null);
      avatarSimulationFactory.setRobotInitialSetup(robotInitialSetup);
      avatarSimulationFactory.setSCSInitialSetup(scsInitialSetup);
      avatarSimulationFactory.setGuiInitialSetup(guiInitialSetup);
      avatarSimulationFactory.setHumanoidGlobalDataProducer(null);
      avatarSimulation = avatarSimulationFactory.createAvatarSimulation();
      initialize();
      avatarSimulation.start();
   }

   /**
    * used to inject anything you need into scs before the sim starts
    */
   public void initialize()
   {

   }

   /**
    * Creates the momentum controller factory.
    */
   public AbstractMomentumBasedControllerFactory getControllerFactory(ContactableBodiesFactory contactableBodiesFactory, SideDependentList<String> footForceSensorNames,
                                                                      SideDependentList<String> footContactSensorNames, SideDependentList<String> wristSensorNames,
                                                                      WalkingControllerParameters walkingControllerParameters, ICPWithTimeFreezingPlannerParameters capturePointPlannerParameters,
                                                                      StandPrepSetpoints standPrepSetpoints, NewHighLevelControllerStates initialControllerState, NewHighLevelControllerStates fallbackControllerState)
   {
      return new NewMomentumBasedControllerFactory(contactableBodiesFactory, footForceSensorNames, footContactSensorNames, wristSensorNames, walkingControllerParameters,
                                                   capturePointPlannerParameters, standPrepSetpoints, initialControllerState, fallbackControllerState);
   }

   public void attachControllerFailureListener(ControllerFailureListener listener)
   {
      avatarSimulation.getMomentumBasedControllerFactory().attachControllerFailureListener(listener);
   }

   public SimulationConstructionSet getSimulationConstructionSet()
   {
      return avatarSimulation.getSimulationConstructionSet();
   }

   public AvatarSimulation getAvatarSimulation()
   {
      return avatarSimulation;
   }

   public void destroySimulation()
   {
      if (avatarSimulation != null)
      {
         avatarSimulation.dispose();
      }
   }
}
