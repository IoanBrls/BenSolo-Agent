import java.util.Collection;
import java.util.List;
import java.util.Random;

import misc.Range;
import negotiator.Bid;
import negotiator.timeline.TimeLineInfo;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.SortedOutcomeSpace;
import negotiator.utility.AbstractUtilitySpace;

public class OfferingStrategy {
    private SortedOutcomeSpace outcomeSpace;
    
    private double reserveUtility;
    private double suggestRange;
    private double concessionShape;
    
    public OfferingStrategy(AbstractUtilitySpace utilitySpace, double reserveUtility,
    						double concessionShape,double suggestRange)
    {
        if (concessionShape <= 0.0 || concessionShape >= 2.0) 
        {
            throw new IllegalArgumentException("Concession Shape must be between 0.0 and 2.0");
        }
        
        outcomeSpace = new SortedOutcomeSpace(utilitySpace);
        this.reserveUtility = reserveUtility;
        this.concessionShape = concessionShape;
        this.suggestRange = suggestRange;
    }
    
    /**
     * Our Bidding Strategy.
     * 
     * First we calculate our utilitygoal, and then we get a range of bids that satisfy
     * this goal.
     * Afterwards, we implement a technique similar to simulated annealing in which we either
     * pick the bid that best maximizes the worst opponent utility and of course is to our 
     * interest as well or we pick a random bit.
     * The chance of picking the optimal bid increases as the time goes by.
     
     * @param rng 
     * 		The random generator we initialized in our Agent's main class.
     * @param time 
     * 		The current timeline of our negotiation, used to help us optimize our 
     * 		bidding strategy.
     * @param opponentModels 
     * 		The models our agent has mapped for his opponents, used so that the agent
     * 		 can maximize their worst utility.
     */
    public Bid generateBid(Random rng, TimeLineInfo time, Collection<OpponentModel> opponentModels) {
        
    	double targetUtility = utilityGoal(time, this.concessionShape, this.reserveUtility);
        Range range = new Range(targetUtility - suggestRange/2.0, targetUtility + suggestRange/2.0);
        
        //Check if there is an utility higher than 1.0
        if (range.getUpperbound() > 1.0) 
        {
            range.setLowerbound(range.getLowerbound() - (range.getUpperbound() - 1.0));
            range.setUpperbound(1.0);
        }
        
        List<BidDetails> possibleBids = outcomeSpace.getBidsinRange(range);
        System.out.println("Number of bids in range " + possibleBids.size());
        
        double temperature = rng.nextDouble();
        if (temperature > utilityGoal(time, 0.25, 0.0)) 
        {
            // Select best option
            System.out.println("Suggested best bid");
            
            Bid bestBid = null;
            double bestMinUtility = 0.0;
            for (BidDetails bid : possibleBids)
            {
                double worstOpponentUtility = opponentModels.stream()
                		.mapToDouble(model -> model.evaluateBid(bid.getBid()))
                        .min().getAsDouble();
                if (worstOpponentUtility > bestMinUtility) 
                {
                    bestBid = bid.getBid();
                    bestMinUtility = worstOpponentUtility;
                }
            }
            
            return bestBid;
            
        } else {
            // Select random bid in range
            System.out.println("Suggested random bid");
            int i = rng.nextInt(possibleBids.size());
            return possibleBids.get(i).getBid();
        }
    }
    
    //returns the best utility bid.
    public Bid getInitialBid() {
        return outcomeSpace.getMaxBidPossible().getBid();
    }
    
    
    /**
     * The function that gives our Agent the utility he should aim for.
     * 
     * @param time
     * 		The timeline that the utility goal is based on.
     * @param concessionShape
     * 		This is the variable representing our concession shape which declares whether
     * 		the agent will follow a conceder type strategy or a boulware  type strategy.
     * 		If the value of the concession shape is between 0 and 1 the agent follows a 
     * 		conceder type strategy.
     * 		If the value is between 1 and 2 the agent follows a boulware type strategy. 	 
     * @param reservedUtility
     * 		The minimum acceptable utility.
     * @return  
     * 		The utililty goal which is a double value between 1.0 and the value of the reserveUtility.
     */
    private double utilityGoal(TimeLineInfo time, double concessionShape, double reserveUtility) {
        double utility;
        
        if (concessionShape < 1.0) {
            utility = 1.0 - Math.pow(time.getTime(), concessionShape);
        } else {
            utility = 1.0 - Math.pow(time.getTime(), 1.0 / (2.0 - concessionShape));
        }
        
        return reserveUtility + (1.0 - reserveUtility) * utility;
    }
}
