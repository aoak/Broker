import java.util.Comparator;


/* This is class which implements the comparator for different requests.
 * it can be used to sort the priority queue in ascending or descending order.
 */
public class ReqComparator implements Comparator<Request> {
	
	int mult = 1;
	
	public ReqComparator (char mode) {
		if (mode == 'a') {
			mult = 1;
		} else if (mode == 'd') {
			mult = -1;
		}
	}

	public int compare (Request a, Request b) {
		if (a.getOffer() < b.getOffer() ) {
			return -1 * mult;
		} else if (a.getOffer() > b.getOffer() ) {
			return 1 * mult;
		}
		return 0;
	}
}
