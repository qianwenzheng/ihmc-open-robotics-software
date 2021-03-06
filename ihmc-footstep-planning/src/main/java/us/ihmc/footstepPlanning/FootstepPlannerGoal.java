package us.ihmc.footstepPlanning;

import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.robotics.robotSide.SideDependentList;

public class FootstepPlannerGoal
{
   private FramePose3D goalPoseBetweenFeet;
   private FramePoint3D goalPositionBetweenFeet;
   private SimpleFootstep singleFootstepGoal;
   private SideDependentList<SimpleFootstep> doubleFootstepGoal;
   private Point2D xyGoal;
   private double distanceFromXYGoal;

   private FootstepPlannerGoalType footstepPlannerGoalType;

   public FramePose3D getGoalPoseBetweenFeet()
   {
      return goalPoseBetweenFeet;
   }

   public void setGoalPoseBetweenFeet(FramePose3D goalPoseBetweenFeet)
   {
      this.goalPoseBetweenFeet = goalPoseBetweenFeet;
   }

   public FramePoint3D getGoalPositionBetweenFeet()
   {
      return goalPositionBetweenFeet;
   }

   public void setGoalPositionBetweenFeet(FramePoint3D goalPositionBetweenFeet)
   {
      this.goalPositionBetweenFeet = goalPositionBetweenFeet;
   }

   public SimpleFootstep getSingleFootstepGoal()
   {
      return singleFootstepGoal;
   }

   public void setSingleFootstepGoal(SimpleFootstep singleFootstepGoal)
   {
      this.singleFootstepGoal = singleFootstepGoal;
   }

   public SideDependentList<SimpleFootstep> getDoubleFootstepGoal()
   {
      return doubleFootstepGoal;
   }

   public void setDoubleFootstepGoal(SideDependentList<SimpleFootstep> doubleFootstepGoal)
   {
      this.doubleFootstepGoal = doubleFootstepGoal;
   }

   public void setXYGoal(Point2D xyGoal, double distanceFromXYGoal)
   {
      this.xyGoal = new Point2D(xyGoal);
      this.distanceFromXYGoal = distanceFromXYGoal;
   }

   public Point2D getXYGoal()
   {
      return xyGoal;
   }

   public double getDistanceFromXYGoal()
   {
      return distanceFromXYGoal;
   }

   public FootstepPlannerGoalType getFootstepPlannerGoalType()
   {
      return footstepPlannerGoalType;
   }

   public void setFootstepPlannerGoalType(FootstepPlannerGoalType footstepPlannerGoalType)
   {
      this.footstepPlannerGoalType = footstepPlannerGoalType;
   }

}
