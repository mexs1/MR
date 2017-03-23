package exampleai.brain;


import mrlib.core.KickLib;

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
	                    	
	                    	// get ball position
	                    	BallPosition ballPos = vWorldState.getBallPosition();
	                    	
	                    	// check if bot can kick
	                    	if( ballPos.getDistanceToBall() < mSelf.getGamevalue( GamevalueNames.KickRange ) ){                 
	                    		// kick
	                    		ReferencePoint goalMid = PositionLib.getMiddleOfGoal( vWorldState, mSelf.getTeam() );
	                    		vBotAction = KickLib.kickTo( goalMid ); 
	                    		
	                    	} else {
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
