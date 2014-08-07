package conflictsAnalyzer;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import de.ovgu.cide.fstgen.ast.FSTTerminal;


public class ConflictsController {
	
	private ArrayList<FSTTerminal> conflictingNodes;
	
	private ArrayList<Conflict> conflictsList;
	
	private Hashtable<String, Integer> conflictsReport;
	
	
	
	public ConflictsController(String revisionFilePath){
		
		this.identifyConflictingNodes(revisionFilePath);
		
		this.identifyConflictsPatterns();
	
		this.computeConflictsReport();
		
		this.printConflictsReport();
		
	}
	
	
	public void printConflictsReport(){
		
		ConflictPrinter cp = new ConflictPrinter();
		cp.writeConflictsReport(this.conflictsReport);
		
		
	}
	
	public void identifyConflictingNodes(String revisionFilePath){
		
		FSTNodeParser parser = new FSTNodeParser();
		this.conflictingNodes = parser.identifyConflictingNodes(revisionFilePath);
		
		
	}
	
	

	public ArrayList<Conflict> getConflictsList() {
		return conflictsList;
	}

	public void setConflictsList(ArrayList<Conflict> conflictsList) {
		this.conflictsList = conflictsList;
	}
	
	public void identifyConflictsPatterns(){
		
		this.conflictsList = new ArrayList<Conflict>();
		
		for (FSTTerminal node : this.conflictingNodes){
			
			
			Conflict conflict = this.matchConflict(node);
			
			this.conflictsList.add(conflict);
			
			
		}
		
	}
	
	public Conflict matchConflict(FSTTerminal node){
		Conflict conflict = new Conflict();
		
		String nodeType = node.getType();
		
		String nodeBody = node.getBody();
		
		String conflictType = "";
		
		if(nodeType.equals("Modifiers")){
			
			conflictType = SSMergeConflicts.ModifierList.toString();
		
		}else if(nodeType.equals("AnnotationMethodDecl")){
			
			conflictType = SSMergeConflicts.DefaultValueAnnotation.toString();
			
		}else if(nodeType.equals("ImplementsList")){
			
			conflictType = SSMergeConflicts.ImplementList.toString();
			
		}else if(nodeType.equals("FieldDecl") ){
			
			conflictType = this.setFieldDeclPattern(nodeBody);
			
		}
		
		else if(nodeType.equals("MethodDecl") || nodeType.equals("ConstructorDecl")){
			
			conflictType = this.setMCPattern(nodeBody);
			
		}
		
		conflict.setType(conflictType);
		conflict.setBody(nodeBody);
		
		return conflict;
	}
	
	
	public String setFieldDeclPattern(String nodeBody){
		
		String type = "";
		String [] fd = nodeBody.split(FSTNodeParser.SSMERGE_SEPARATOR);
		
		if(fd[1].equals(" ")){
			
			type = SSMergeConflicts.SameIdFd.toString();
			
		}else{
			type = SSMergeConflicts.LineBasedMCFd.toString();
		}
		
		return type;
		
	}
	
	public String setMCPattern(String nodeBody){
		
		String type = "";
		
		String [] p1 = nodeBody.split("\\|\\|\\|\\|\\|\\|\\|");
		String [] p2 = p1[1].split("=======");
		String a = p2[0].substring(1, p2[0].length()-1);
		
		if(a.contains(" ")){
			
			type = SSMergeConflicts.LineBasedMCFd.toString();
		}else{
			
			type = SSMergeConflicts.SameSignatureCM.toString();
			
		}
		
		return type;
		
	}
	
	public void computeConflictsReport(){
		
		this.conflictsReport = new Hashtable<String, Integer>();
		
		for(SSMergeConflicts c : SSMergeConflicts.values()){
			
			String type = c.toString();
			int quantity = this.countConflicts(type);
			this.conflictsReport.put(type, quantity);
		}
		
	}
	
	public int countConflicts(String type){
		
		int result = 0;
		
		for(Conflict c : this.conflictsList){
			
			if(c.type.equals(type)){
				
				result++;
			}
			
		}
		
		return result;
	}
	
	

	public Map<String, Integer> getConflictsReport() {
		return conflictsReport;
	}



	public void setConflictsReport(Hashtable<String, Integer> conflictsReport) {
		this.conflictsReport = conflictsReport;
	}



	public ArrayList<FSTTerminal> getConflictingNodes() {
		return conflictingNodes;
	}

	public void setConflictingNodes(ArrayList<FSTTerminal> conflictingNodes) {
		this.conflictingNodes = conflictingNodes;
	}

	public static void main(String[] args) {
		String file = "/Users/paolaaccioly/gitClones/fse_2011_artifacts/examples/SSMergeCatalog/6/rev_6.revisions";
		ConflictsController cc = new ConflictsController(file);
		System.out.println(cc.getConflictsReport().toString());
		
		
	}
	

}