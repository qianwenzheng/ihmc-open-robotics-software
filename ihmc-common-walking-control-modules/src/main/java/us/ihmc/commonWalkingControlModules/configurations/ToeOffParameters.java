package us.ihmc.commonWalkingControlModules.configurations;

public abstract class ToeOffParameters
{
   /**
    * Boolean to enable transitions to the toe off contact state, if the appropriate conditions are satisfied.
    * @return boolean (true = Allow Toe Off, false = Don't Allow Toe Off)
    */
   public abstract boolean doToeOffIfPossible();

   public abstract boolean doToeOffIfPossibleInSingleSupport();

   /**
    * Whether or not the location of the ECMP must be close enough to the support polygon before allowing toe off.
    *
    * @return whether or not to check the ECMP location.
    */
   public abstract boolean checkECMPLocationToTriggerToeOff();

   /**
    * Maximum distance of the ECMP to the toe off support polygon before allowing toe off.
    *
    * @return ECMP distance (m).
    */
   public double getECMPProximityForToeOff()
   {
      return 0.0;
   }

   /**
    * Whether or not the location of the CoP in the trailing foot must be close enough to the support polygon before allowing toe off.
    *
    * @return whether or not to check the CoP location.
    */
   public boolean checkCoPLocationToTriggerToeOff()
   {
      return false;
   }

   /**
    * Maximum distance of the CoP in the trailing foot to the toe off support polygon before allowing toe off.
    *
    * @return CoP distance (m).
    */
   public double getCoPProximityForToeOff()
   {
      return 0.03;
   }

   /**
    * Minimum stance length in double support to enable toe off.
    * @return threshold stance length in meters
    */
   public abstract double getMinStepLengthForToeOff();

   /**
    * If the leading foot is above this value in height, it is one of the last checks that says whether or not to
    * switch the contact state to toe off for the trailing foot.
    * @return threshold height in meters for stepping up to cause toe off
    */
   public double getMinStepHeightForToeOff()
   {
      return 0.10;
   }

   /**
    * Whether or not to use a line contact during the swing state. If false, will use a point contact instead.
    */
   public boolean useToeOffLineContactInSwing()
   {
      return true;
   }

   /**
    * Whether or not to use a line contact during the transfer state. If false, will use a point contact instead.
    */
   public boolean useToeOffLineContactInTransfer()
   {
      return false;
   }

   /**
    * Whether or not to update the line contact points when performing toe off. If false, the line is only calculated
    * when toe-off is first started using a line, and not updated.
    */
   public boolean updateLineContactDuringToeOff()
   {
      return false;
   }

   /**
    * Whether or not to update the point contact points when performing toe off. If false, the point is only calculated
    * when toe-off is first started using a point, and not updated.
    */
   public boolean updatePointContactDuringToeOff()
   {
      return false;
   }

   /**
    * To enable that feature, {@link ToeOffParameters#doToeOffIfPossible()} return true is required. John parameter
    */
   public abstract boolean doToeOffWhenHittingAnkleLimit();

   /**
    * Ankle limit that triggers {@link ToeOffParameters#doToeOffWhenHittingAnkleLimit()}.
    * The minimum limit is taken between the returned value and the joint limit.
    */
   public double getAnkleLowerLimitToTriggerToeOff()
   {
      return -1.0;
   }

   /**
    * To enable that feature, {@link ToeOffParameters#doToeOffIfPossible()} return true is required.
    */
   public boolean doToeOffWhenHittingKneeLimit()
   {
      return false;
   }

   /**
    * Knee limit that triggers {@link ToeOffParameters#doToeOffWhenHittingKneeLimit()}.
    * The maximum limit is taken between the returned value and the joint limit.
    */
   public double getKneeUpperLimitToTriggerToeOff()
   {
      return 1.2;
   }

   /**
    * Sets the maximum pitch of the foot during toe off to be fed into the whole-body controller
    * @return maximum pitch angle
    */
   public abstract double getMaximumToeOffAngle();

   /**
    * In transfer, this determines maximum distance from the ICP to the leading foot support polygon to allow toe-off.
    * This distance is determined by finding the stance length, and multiplying it by the returned variable.
    * If it is further than this, do not allow toe-off, as more control authority is needed from the trailing foot.
    * @return percent of stance length for proximity
    */
   public double getICPPercentOfStanceForDSToeOff()
   {
      return 0.0;
   }

   /**
    * In swing, this determines maximum distance from the ICP to the leading foot support polygon to allow toe-off.
    * This distance is determined by finding the stance length, and multiplying it by the returned variable.
    * If it is further than this, do not allow toe-off, as more control authority is needed from the trailing foot.
    * @return percent of stance length for proximity
    */
   public double getICPPercentOfStanceForSSToeOff()
   {
      return 0.0;
   }

   /**
    * Sets an interpolation ratio for determining the toe off contact point. A ray is cast forward from the center
    * of the foot through this point, and where the ray intersects with the foot polygon is where the toe off contact is set.
    * This interpolation allows biasing between the ideal ICP plan by choosing only the exit CMP and the feedback CMP location.
    * @return interpolation ratio (0.0 = all exit cmp, 1.0 = all desired CoP)
    */
   public double getToeOffContactInterpolation()
   {
      return 0.0;
   }
}
