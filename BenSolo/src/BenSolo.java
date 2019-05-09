
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.parties.NegotiationInfo;
/**
 * This is your negotiation party.
 */
public class BenSolo extends AbstractNegotiationParty{
	
	private Random rng;
    private Map<Object,OpponentModel> opponentModels; //opponent mapping 
    private OfferingStrategy offeringStrategy;		  //reference to offering strategy class
        
    
    // Since the calls to chooseAction asks us if we want to accept a bid,
    // but doesn't supply the bid, we have to look at the bids received and
    // remember the most recent one.
    private Bid currentBidOffered;
    
    /**
     * The constructor called by genius.
     * 
     * @param info
     * 			The NegotiationInfo for this negotiation. 
     * 			These informations are: the utility space, the deadlines set
     * 			for this negotiation, the timeline, the randomSeed, the AgentID
     * 			and a storage that can hold data from previous sessions.
     */

	@Override
	public void init(NegotiationInfo info) {

		super.init(info);

		rng = new Random(info.getRandomSeed());
        opponentModels = new HashMap<>();
        offeringStrategy = new OfferingStrategy(utilitySpace, 0.5, 1.7, 0.05);
        
	}

	/**
	 * Each round this method gets called and ask you to accept or offer. The
	 * first party in the first round is a bit different, it can only propose an
	 * offer.
	 *
	 * @param validActions
	 *            Either a list containing both accept and offer or only offer.
	 * @return The chosen action.
	 */
	@Override
	public Action chooseAction(List<Class<? extends Action>> validActions) {
		System.out.println("Entering round " + timeline.getCurrentTime());
        
		//At the start of the negotiation we suggest our best utility.
		if (!validActions.contains(Accept.class)) {
            return new Offer(getPartyId(),offeringStrategy.getInitialBid());
        }
		
		//Generates a bid according to our offering strategy.
		Bid counterOffer = offeringStrategy.generateBid(
	            rng, timeline, opponentModels.values()
	        );
		
        if (isBidAcceptable(currentBidOffered, counterOffer)) {
            System.out.println("Accepted offer of utility " + utilitySpace.getUtility(currentBidOffered));
            return new Accept(getPartyId(),currentBidOffered);
        } else {
            System.out.printf(
                "Countered offer of utility %.3f with offer of utility %.3f\n",
                utilitySpace.getUtility(currentBidOffered),
                utilitySpace.getUtility(counterOffer)
            );
            currentBidOffered = counterOffer;
            return new Offer(getPartyId(),currentBidOffered);
        }
	}
	
	/**
	 * This functions is basically our very simple acceptance strategy which accepts 
	 * an offer if its utility is better that the utility of the counter offer we
	 * are about to propose.
	 *
	 * @param bidOffered
	 *            The bid that was proposed to us.
	 * @param counterOffer
	 * 			  Our counter offer
	 */
	
	public boolean isBidAcceptable(Bid bidOffered,Bid counterOffer) 
	{
	       try {
	           if (counterOffer != null && utilitySpace.getUtility(bidOffered) >= utilitySpace.getUtility(counterOffer))
	           {
	               return true;
	           }
	           
	       } catch (Exception e) {
	           e.printStackTrace();
	       }
	       return false;
	}
 
	 

	/**
	 * All offers proposed by the other parties will be received as a message.
	 * You can use this information to your advantage, for example to predict
	 * their utility.
	 *
	 * @param sender
	 *            The party that did the action. Can be null.
	 * @param action
	 *            The action that party did.
	 */
	@Override
	public void receiveMessage(AgentID sender, Action action)
	{
		super.receiveMessage(sender, action);
		
		if (!(action instanceof Offer) && !(action instanceof Accept)) {
            System.out.println("Invalid action " + action.toString());
            return;
        }
		
		if (action instanceof Offer) {
            Offer offer = (Offer) action;
            currentBidOffered = offer.getBid();
        } else if (action instanceof Accept) {
            try {
                System.out.printf("%s accepted offer of utility %.3f\n",
                    sender.toString(),
                    utilitySpace.getUtility(currentBidOffered)
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
		
		opponentModels.putIfAbsent(sender, new OpponentModel(utilitySpace.getDomain()));
        
        opponentModels.get(sender).addApprovedBid(currentBidOffered);
		
	}

	@Override
	public String getDescription() {
		return "BenSolo Agent";
	}

}
