import java.util.HashMap;
import java.util.Map;

import negotiator.Bid;
import negotiator.Domain;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.ValueDiscrete;

/**
 * Opponent model based on a frequency analysis heuristic
 *
 */
public class OpponentModel {
	
    private Map<IssueDiscrete, Map<ValueDiscrete, Integer>> issueValueCount;
    private Map<IssueDiscrete, Double> weights;
    
    private final double lrate = 0.1;
    private Bid lastBid = null;

    // Number of bids entered into the model.
    // This value is for convenience since the sum of the counts for each
    // issue is equivalent to this
    private int datapoints = 1;

    public OpponentModel(Domain domain) 
    {
        issueValueCount = new HashMap<>();
        weights = new HashMap<>();
        
        for (Issue issue : domain.getIssues()) 
        {
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            issueValueCount.put(issueDiscrete, new HashMap<>());
            
            for (ValueDiscrete value : issueDiscrete.getValues()) 
            {
                issueValueCount.get(issueDiscrete).put(value, 1);
            }
            
            weights.put(issueDiscrete, 1.0 / domain.getIssues().size());
        }
    }

    /**
     * Call this when a bid is suggested, or accepted. 
     * @param bid
     *            the bid that was approved by the opponent
     */
    public void addApprovedBid(Bid bid) {
        try {
            for (Issue issue : bid.getIssues()) {
                ValueDiscrete value = (ValueDiscrete) bid.getValue(issue.getNumber());
                issueValueCount.get(issue).compute(value, (k, v) -> v + 1);
            }
            if (lastBid != null) 
            {
                for (Issue issue : bid.getIssues()) 
                {
                    if (bid.getValue(issue.getNumber()).equals(lastBid
                    		.getValue(issue.getNumber()))) 
                    {
                        weights.put((IssueDiscrete) issue, weights.get(issue) + lrate);
                    }
                }
                // normalize
                double sum = weights.values().stream().reduce(0.0, Double::sum);
                weights.replaceAll((k, v) -> v / sum);
            }
            lastBid = bid;
            datapoints++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Evaluates the predicted utility of a given bid, using the information
     * provided for the opponent until this point of the negotiation.
     * 
     * @param bid
     *            the bid to be evaluated
     * @return 
     * 		the predicted utility of the bid
     */
    public double evaluateBid(Bid bid) {
        double utility = 0.0;

        try {
            for (Issue issueRaw : bid.getIssues()) 
            {
                ValueDiscrete value = (ValueDiscrete) bid.getValue(issueRaw
                        .getNumber());
                IssueDiscrete issue = (IssueDiscrete) issueRaw;

                double issueWeight = weights.get(issue);
                double issueValue = ((double) issueValueCount.get(issue)
                		.get(value))/ datapoints;

                utility += issueWeight * issueValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return utility;
    }

}
