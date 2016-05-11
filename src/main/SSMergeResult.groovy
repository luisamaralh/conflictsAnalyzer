package main

import java.util.ArrayList;
import java.util.Map;

class SSMergeResult {
	
	private String mergeScenarioName
	
	private boolean hasConflicts
	
	private Map<String, ArrayList<MethodEditedByBothRevs>> filesWithMethodsToJoana
	

	public SSMergeResult(String mScenarioName, boolean hc, Map<String, ArrayList<MethodEditedByBothRevs>> fwmtj){

		this.mergeScenarioName = mScenarioName
		this.hasConflicts = hc
		this.filesWithMethodsToJoana = fwmtj
		
	}
	
	public boolean getHasConflicts() {
		return hasConflicts;
	}
	public void setHasConflicts(boolean hasConflicts) {
		this.hasConflicts = hasConflicts;
	}
	public Map<String, ArrayList<MethodEditedByBothRevs>> getFilesWithMethodsToJoana() {
		return filesWithMethodsToJoana;
	}
	public void setFilesWithMethodsToJoana(Map<String, ArrayList<MethodEditedByBothRevs>> filesWithMethodsToJoana) {
		this.filesWithMethodsToJoana = filesWithMethodsToJoana;
	}

}
