package conflictsAnalyzer

import java.util.Observable;

import merger.FSTGenMerger;
import merger.MergeVisitor;
import composer.rules.ImplementsListMerging;
import de.ovgu.cide.fstgen.ast.FSTTerminal;


class MergeScenario implements Observer {

	private String path

	private ArrayList<Conflict> conflicts

	private Map<String,Integer> mergeScenarioSummary


	MergeScenario(String path){
		this.path = path
		this.conflicts = new ArrayList<Conflict>()
		this.createMergeScenarioSummary()
	}

	public void analyzeConflicts(){
		this.runFstGenMerger()
	}

	public void runFstGenMerger(){
		FSTGenMerger fstGenMerge = new FSTGenMerger()
		fstGenMerge.getMergeVisitor().addObserver(this)
		String[] files = ["--expression", this.path]
		fstGenMerge.run(files);
	}


	@Override
	public void update(Observable o, Object arg) {

		if(o instanceof MergeVisitor && arg instanceof FSTTerminal){

			FSTTerminal node = (FSTTerminal) arg

			if(!node.getType().contains("-Content")){
				this.createConflict(node)
			}
		}
	}

	public void createConflict(FSTTerminal node){
		Conflict conflict = new Conflict(node, this.path);
		this.conflicts.add(conflict)
		this.updateMergeScenarioSummary(conflict.getType())
	}

	public void createMergeScenarioSummary(){
		this.mergeScenarioSummary = new HashMap<String, Integer>()
		for(SSMergeConflicts c : SSMergeConflicts.values()){

			String type = c.toString();
			this.mergeScenarioSummary.put(type, 0)
		}
	}
	
	public void updateMergeScenarioSummary(String type){
		Integer typeQuantity = this.mergeScenarioSummary.get(type).value
		typeQuantity++
		this.mergeScenarioSummary.put(type, typeQuantity)
		
	}

	public String getId(){
		return this.id
	}

	public void setId(String id){
		this.id = id
	}

	public ArrayList<Conflict> getConflicts(){
		return this.conflicts
	}

	public void setConflicts(ArrayList<Conflict> conflicts){
		this.conflicts = conflicts
	}
}
