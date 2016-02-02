package solver;

import java.util.ArrayList;

/** The Optimizer class first determines which algorithm should be used to select the most optimal list
 * of requirements, and then it runs that specified algorithm to select such requirements.
 * 
 * Two algorithms are possible: a greedy algorithm, which works in O(n) time and space where n = number
 * of requirements, and a dynamic algorithm which works in O(n*k) time and space, where k is the fixed
 * cost.  The dynamic algorithm will tend to generate a more optimal solution (i.e. a list of 
 * requirements that maximizes the amount of profit, while staying within fixed cost), but tends to 
 * use more memory overhead to do so.  The hasEnoughSpace() method will estimate the amount of memory 
 * needed, and always choose the dynamic algorithm if there is enough space; otherwise the greedy 
 * algorithm is used.  This ensures that even if the "better" dynamic algorithm cannot be used due
 * to space constraints, the user is still able to get a valid list of requirements that attempts
 * to maximize profits.
 * 
 * @author Michael Camara
 *
 */
public class Optimizer {

	// The original list of requirements
	private ArrayList<Requirement> requirements;
	
	// The fixedCost threshold the chosen requirements cannot cumulatively exceed
	private int fixedCost;
	
	// A string representation of the algorithm chosen ("greedy" or "dynamic")
	private String chosenAlgorithm;
	
	// Indicate if user has chosen to force using greedy algorithm instead of the dynamic
	private boolean forceGreedy;

	/** Initialize Optimizer using the original list of requirements and specified fixed cost
	 * 
	 * @param requirements The original list of requirements
	 * @param fixedCost The fixed cost threshold
	 */
	public Optimizer(ArrayList<Requirement> requirements, int fixedCost, boolean forceGreedy) {
		this.requirements = requirements;
		this.fixedCost = fixedCost;
		this.forceGreedy = forceGreedy;
	}

	/** This method decides which algorithm should be used with the original list of requirements.
	 * It attempts to choose the dynamic algorithm first (which should yield the most optimal
	 * result), but it will switch to the greedy algorithm if not enough space is detected
	 * (through the hasEnoughSpace() method, which estimates the amount of JVM free space
	 * before memory allocation).
	 * 
	 * @return The chosen combination of requirements generated by one of the algorithms
	 */
	public ArrayList<Requirement> optimize() {
		
		// Announce that selection process has begun
		System.out.println("Selecting profit maximizing requirements given fixed cost of " + fixedCost + "...\n");

		// Contain list of profit-maximizing requirements
		ArrayList<Requirement> optimalReqs = new ArrayList<Requirement>();
		
		// If user has specified during startup to force the greedy algorithm, use that one
		if(forceGreedy == true) {
			optimalReqs = useGreedy();
			chosenAlgorithm = "Greedy";
		}
		
		// Otherwise always begin with the dynamic algorithm
		else {
			try {
				optimalReqs = useDynamic();
				chosenAlgorithm = "Dynamic";
			}
			
			// If out of memory error encountered, automatically switch to greedy algorithm
			catch(OutOfMemoryError e) {
				optimalReqs = useGreedy();
				chosenAlgorithm = "Greedy";
			}
		}

		return optimalReqs;
	}

	/** This method estimates the amount of space needed to perform the dynamic selection algorithm.
	 * Due to the arbitrary nature of garbage collection in Java and the inaccuracy of direct
	 * runtime memory queries, this is meant only as a broad estimate.  However, it should be accurate
	 * enough to determine the amount of memory needed to create the large data structure for the
	 * dynamic algorithm, and then determine a ratio of expected memory usage.
	 * 
	 * @return false if the ratio of free memory usage after the dynamic algorithm is expected to be
	 * too small; true if the ratio of free memory usage after the dynamic algorithm appears sufficient
	 */
	public boolean hasEnoughSpace() {
	
		// Allow current instance of JVM to be accessed
		Runtime runtime = Runtime.getRuntime();
	
		// Calculate the expected number of bytes to be used by maxValues matrix
		double expectedUsedMem = 4.0 * (fixedCost + 1) * (requirements.size() + 1);
	
		// Determine the (approximate) max and currently allocated memory of JVM
		double allocatedMem = runtime.totalMemory();
		double maxMem = runtime.maxMemory();
	
		// Calculate the ratio of expected available memory 
		double expectedMemRatio = (maxMem - allocatedMem - expectedUsedMem) / maxMem;

		// Debugging statement: Display ratio of expected available memory
//		System.out.println("Available memory ratio: " + expectedMemRatio);

		// Indicate insufficient space if available memory ratio is too low
		if(expectedMemRatio < 0.02) {
			//			System.out.println("Insufficient memory: Greedy algorithm chosen.\n");			
			return false;
		}
		else {
			//			System.out.println("Sufficient memory: Dynamic algorithm chosen.\n");
			return true;
		}
	}

	/** This method using a dynamic algorithm to determine which requirements should be selected
	 * that maximize profit while staying within the fixed cost threshold.  Expected to run in
	 * O(n*k) time and space, where n = number of requirements and k = fixed cost.
	 * 
	 * @return The combination of requirements determined to yield to maximum possible profit while
	 * staying within fixed cost
	 * @author Original algorithm taken from Hans Kellerer, Ulrich Pferschy, and David Pisinger in
	 * their book, Knapsack Problems (2004).  Interpreted into Java by Michael Camara.
	 */
	public ArrayList<Requirement> useDynamic() {
	
		// Quickly check if JVM has enough space to perform this algorithm;
		// will throw OutOfMemory error if insufficient *before* memory allocation begins next
		if(hasEnoughSpace() == false)
			throw new OutOfMemoryError();
	
		// Contain list of chosen requirements using Dynamic algorithm		
		ArrayList<Requirement> chosenReqs = new ArrayList<Requirement>();
	
		// Contain the max profits for each requirement and each possible cost constraint
		// NOTE: all values initialized to zero
		int [][] maxValues = new int[requirements.size()+1][fixedCost + 1];
	
		// Add null first requirement to list (to prevent off-by-1 error in algorithm)
		requirements.add(0,null);
	
		// Use algorithm for dynamic 0-1 knapsack problem to determine maximum possible profit
		// given constraint of fixedCost
		for(int i = 1; i < requirements.size(); i++) {
	
			Requirement req = requirements.get(i);
			int cost = req.getCost();
	
			// OR!! for(int j = 0; j < cost - 1 && j < fixedCost; j++) {
			for(int j = 0; j < cost - 1; j++) {
				maxValues[i][j] = maxValues[i-1][j];
			}
			for(int j = cost; j <= fixedCost; j++) {
				int prevMax = maxValues[i-1][j];
				int otherMax = maxValues[i-1][j-cost] + req.getPerceivedProfit();
				maxValues[i][j] = Math.max(prevMax, otherMax);
			}
		}
	
		// Select the requirements that yield the maximum profit as calculated above
		for(int i = requirements.size() - 1, k = fixedCost; i > 0; i--) {
			if(maxValues[i][k] != maxValues[i-1][k]) {
				Requirement chosenReq = requirements.get(i);
				chosenReqs.add(chosenReq);
				k = k - chosenReq.getCost();
			}
		}
	
		return chosenReqs;
	}

	/** This method uses a greedy algorithm to determine which requirements should be selected that
	 * maximize profit while staying within fixed cost.  The requirements are first sorted in descending
	 * order based on their ratio of profit to cost.  Requirements are then chosen by iterating through the
	 * list, starting at the first index, and adding successive requirements until the fixed cost is reached
	 * (but not exceeded).  Expected to run in O(n) time and space, where n = number of requirements.
	 *  
	 * @return The combination of requirements determined to yield to maximum possible profit while
	 * staying within fixed cost.
	 */
	public ArrayList<Requirement> useGreedy() {

		// Contain list of chosen requirements using Greedy algorithm
		ArrayList<Requirement> chosenReqs = new ArrayList<Requirement>();

		// Sort starting requirements in descending order based on profit/cost ratio
		requirements.sort(new RequirementComparator());

		// Add requirements to chosenReqs list from highest profit ratio to lowest;
		// keep adding until total cost of requirements would exceed fixedCost
		for(int i = 0, totalCost = 0; i < requirements.size() && totalCost <= fixedCost; i++) {
			Requirement r = requirements.get(i);
			int cost = r.getCost();

			if(cost + totalCost <= fixedCost) {
				chosenReqs.add(r);
				totalCost += r.getCost();
			}
		}

		return chosenReqs;
	}

	/** Access the name of the algorithm ultimately used by Optimizer
	 * 
	 * @return Name of algorithm used by Optimizer
	 */
	public String getChosenAlgorithm() {
		return chosenAlgorithm;
	}
}