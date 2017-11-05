import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class VehicleAssignment extends LinkedList<PublicAction> {
	public VehicleAssignment() {
	}

	public VehicleAssignment(Collection<? extends PublicAction> c) {
		super(c);
	}
}
