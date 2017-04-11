package exampleai.brain;


import mrlib.core.KickLib;
import mrlib.core.MoveLib;
import mrlib.core.PositionLib;
import essentials.communication.Action;
import essentials.communication.action_server2008.Movement;
import essentials.communication.worlddata_server2008.BallPosition;
import essentials.communication.worlddata_server2008.FellowPlayer;
import essentials.communication.worlddata_server2008.PlayMode;
import essentials.communication.worlddata_server2008.RawWorldData;
import essentials.communication.worlddata_server2008.ReferencePoint;
import essentials.core.ArtificialIntelligence;
import essentials.core.BotInformation;
import essentials.core.BotInformation.GamevalueNames;
import essentials.core.BotInformation.Teams;
import java.util.List;

/**
 * Example striker AI
 * Simply runs to the ball and tries to kick into the middle of the goal.
 * @author Hannes Eilers
 *
 */
public class Striker extends Thread implements ArtificialIntelligence {

    BotInformation mSelf = null;
    RawWorldData mWorldState = null;
    Action mAction = null;
    
    boolean mNeedNewAction = true;    
    boolean mIsStarted = false;
    boolean mIsPaused = false;
    
    @Override
    public void initializeAI( BotInformation aOneSelf ) {        
        mSelf = aOneSelf; 
        mIsStarted = true;
        start();        
    }

    @Override
    public void resumeAI() {        
        mIsPaused = false;        
    }
    
    @Override
    public void suspendAI() {        
        mIsPaused = true;           
    }
    
    /**
     * Main function of AI
     */
    public void run(){
        
        RawWorldData vWorldState = null;
        Action vBotAction = null;
        
        while ( mIsStarted ){
            
            while( mIsPaused ){ try { this.wait( 10 ); } catch ( InterruptedException e ) { e.printStackTrace(); } }

            try {             
                if( mNeedNewAction && mWorldState != null  ){
                    synchronized ( this ) {
                        vWorldState = mWorldState;
                    }
                    
                    // Getting current play mode
                    PlayMode vPlayMode = mWorldState.getPlayMode();
                    
                    // Check for kick off
                    if( vPlayMode == PlayMode.KickOff
                    		|| (vPlayMode == PlayMode.KickOffYellow && mSelf.getTeam() == Teams.Yellow)
                    		|| (vPlayMode == PlayMode.KickOffBlue && mSelf.getTeam() == Teams.Blue) ){
                    	
                    	// --------------- KICK OFF ---------------
                    	if( vWorldState.getBallPosition() != null ){
                    		vBotAction = MoveLib.runTo( vWorldState.getBallPosition()  );
                    	}
                    	else{
                    		vBotAction = MoveLib.runTo( vWorldState.getFieldCenter() );
                    	}
                    	// --------------- KICK OFF END ---------------
                    	
                    }
                    // No kick off
                    else{

	                    // --------------- START AI -------------------
	                    
                    	// check if ball is available
	                    if( vWorldState.getBallPosition() != null ){
	                    	
	                    	
	                    	/**************************************************************************************
							*************************** Collect data for game decisions *****************************
							**************************************************************************************/
							
							//****************************** variable definition *************************************//
							// opposite player
							List<FellowPlayer> oppositeTeam = vWorldState.getListOfOpponents();
							
							// teammates
							List<FellowPlayer> teamMates = vWorldState.getListOfTeamMates(); 
			 
							// Points of interest
							ReferencePoint teamMatePos = teamMates.get(0);
							ReferencePoint opponent1Pos = oppositeTeam.get(0);
							ReferencePoint opponent2Pos = oppositeTeam.get(1);		
								
							BallPosition ballPos = vWorldState.getBallPosition();
							ReferencePoint oppGoalMid = PositionLib.getMiddleOfGoal( vWorldState, mSelf.getTeam() ); //goalMid is middle of opponent goal
							ReferencePoint goalLeftCorner;
							ReferencePoint goalRightCorner;
							ReferencePoint goalLeft;
							ReferencePoint goalRight;
							ReferencePoint penaltyAreaLeft;
							ReferencePoint penaltyAreaRight;
							ReferencePoint oppGoalKeeper;
							ReferencePoint oppStriker;
							ReferencePoint nearestOpponent;
	
							
							
							
							//******************************** calculate variable values *********************************//
							
							// calculate which opposite player is goalKeaper
							if(PositionLib.getDistanceBetweenTwoRefPoints(opponent1Pos, oppGoalMid) < PositionLib.getDistanceBetweenTwoRefPoints(opponent2Pos, oppGoalMid))
							{
								oppGoalKeeper = opponent1Pos;
								oppStriker = opponent2Pos;
							}
							else
							{
								oppGoalKeeper = opponent2Pos;
								oppStriker = opponent1Pos;
							}
							
							//calculate which opposite player is nearest to me
							if(oppGoalKeeper.getDistanceToPoint() < oppStriker.getDistanceToPoint())
							{
								nearestOpponent = oppGoalKeeper;
							}
							else
							{
								nearestOpponent = oppStriker;
							}
							
							// check which Team. Get reference points
							if(mSelf.getTeam() == BotInformation.Teams.Yellow)
							{
								goalLeftCorner = vWorldState.getBlueGoalCornerTop();
								goalRightCorner = vWorldState.getBlueGoalCornerBottom();
								penaltyAreaLeft = vWorldState.getBluePenaltyAreaFrontTop();
								penaltyAreaRight = vWorldState.getBluePenaltyAreaFrontBottom();
								
							}
							else
							{
								goalLeftCorner = vWorldState.getYellowGoalCornerTop();
								goalRightCorner = vWorldState.getYellowGoalCornerBottom();
								penaltyAreaLeft = vWorldState.getYellowPenaltyAreaFrontTop();
								penaltyAreaRight = vWorldState.getYellowPenaltyAreaFrontBottom();
								
							}
	                    	
	                    	// check if bot can kick (ball-posession)
	                    	if( ballPos.getDistanceToBall() < mSelf.getGamevalue( GamevalueNames.KickRange ) )
	                    	{                 
	                    		
	                    		//If enemey goal is in shooting range
	                    		if(oppGoalMid.getDistanceToPoint() < mSelf.getGamevalue(GamevalueNames.MaximumKickTravelDistance))
	                    		{
	                    			
	                    			//If enemy goalkeeper is farer than me to enemy goal
	                    			if(oppGoalKeeper.getDistanceToPoint() > oppGoalMid.getDistanceToPoint())
	                    			{
	                    				vBotAction = KickLib.kickTo( oppGoalMid ); 
	                    			}
	                    			
	                    			//Else kick in the corner with the largest angle from my position to enemy goalkeeper
	                    			else
	                    			{
	                    				if(PositionLib.getDistanceBetweenTwoRefPoints(oppGoalKeeper, goalLeftCorner) < PositionLib.getDistanceBetweenTwoRefPoints(oppGoalKeeper, goalRightCorner))
	                    				{
	                    					vBotAction = KickLib.kickTo( goalRightCorner ); 
	                    				}
	                    				
	                    				else
	                    				{
	                    					vBotAction = KickLib.kickTo( goalLeftCorner );
	                    				}
	                    			}
	                    		}
	                    		
	                    		//if our striker is not in shooting range -> try to find the best path to enemy goal
	                    		else
	                    		{
	                    			
	                    			//If any opponent is near the kickrange -> try to move around opponent
	                    			if(nearestOpponent.getDistanceToPoint() < (mSelf.getGamevalue( GamevalueNames.KickRange))*2)
	                    			{
	                    				//if my angle to enemy goal is smaller than nearsetOpponents angle to enemy goal -> run to left penalty zone ; else right
	                    				if(oppGoalMid.getAngleToPoint() < PositionLib.getAngleBetweenTwoReferencePoints(nearestOpponent, oppGoalMid))
	                    				{
	                    					vBotAction = MoveLib.runTo(penaltyAreaLeft);
	                    				}
	                    				
	                    				else
	                    				{
	                    					vBotAction = MoveLib.runTo(penaltyAreaRight);
	                    				}
	                    			}
	                    			
	                    			//If not run straight to enemy goal middle
	                    			else
	                    			{
	                    				vBotAction = MoveLib.runTo(oppGoalMid);
	                    			}
	                    		}
	                    		
	                    		
	                    		
	                    		
	                    	} 
	                    	
	                    	else {
	                    		// move to ball
	                    		vBotAction = MoveLib.runTo( ballPos );
	                    	}
	                    	
	                   
	                    	
	                    }
	                    
	                    // ---------------- END AI --------------------
                    
                    }
                    
                    // Set action
                    synchronized ( this ) {
                        mAction = vBotAction;
                        mNeedNewAction = false;
                    }                  
                }
                Thread.sleep( 1 );                
            } catch ( Exception e ) {
                e.printStackTrace();
            }            
            
        }
        
    }

    
    @Override
    public synchronized Action getAction() {
        synchronized ( this ) {
            if( mAction != null)
                return mAction;
        }
        return (Action) Movement.NO_MOVEMENT;        
    }

    @Override
    public void putWorldState(RawWorldData aWorldState) {
        synchronized ( this ) {
            mWorldState = aWorldState;
            if( mWorldState.getReferencePoints() != null || !mWorldState.getReferencePoints().isEmpty() ){            
            	mNeedNewAction = true;
            } else {
            	mNeedNewAction = false;
            }
        }        
    }

    @Override
    public void disposeAI() {        
        mIsStarted = false;
        mIsPaused = false;        
    }
    
    @Override
    public boolean isRunning() {
        return mIsStarted && !mIsPaused;        
    }

	@Override
	public boolean wantRestart() {
		return false;
	}

    @Override
    public void executeCommand( String arg0 ) {
    }

}
