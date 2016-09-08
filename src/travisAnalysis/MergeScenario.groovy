package travisAnalysis

import main.Extractor
import main.ExtractorResult;

class MergeScenario {
	
	String projectName
	String sha
	String parent1
	String parent2
	String revName
	int hasFSTMergeConflicts
	int hasRealFSTMergeConflicts
	int hasGitConflictsJava
	int hasGitConflictsNonJava
	Hashtable<String, Integer> predictors
	int discarded
	int buildCompiles
	int testsPass
	
	
	public MergeScenario (String pName, String sha, String parent1, String parent2, String metrics, String clonePath,
		Hashtable<String, ArrayList<String>> commitBuilds, Extractor extractor, Boolean hasRealFSTMergeConflicts){
		this.projectName = pName
		this.sha = sha
		this.parent1 = parent1
		this.parent2 = parent2
		this.hasRealFSTMergeConflicts = hasRealFSTMergeConflicts.value ? 1:0
		this.loadMetrics(metrics)
		if(commitBuilds!=null){
			this.setBuildAndTest(commitBuilds)
			
		}else{
			discarded = 1
		}
		this.runGitMerge(extractor)
	}
		
	/*if the commit contains at least one build that passes we consider that it works.
	 * Otherwise, we consider the last executed build status */
	public void setBuildAndTest(Hashtable<String, ArrayList<String>> commitBuilds){
		int size = commitBuilds.size()
		int i = 0
		boolean foundPassed = false
		for(String buildId : commitBuilds.keySet()){
			String state = commitBuilds.get(buildId)
			/*passed means that both build and tests were executed correctly*/
			if(state.contains('passed')){
				this.buildCompiles = 1
				this.testsPass = 1
				break
			/*errored means that the build does not compile correctly*/	
			}else if(state.contains('errored')){
				this.buildCompiles = 0
				this.testsPass = 0
			/*failed means that the build compiles, but the tests are not executed correctly*/
			}else if(state.contains('failed')){
				this.buildCompiles = 1
				this.testsPass = 0
			}
		}
	}
	
	public void loadMetrics(String metrics){
		String[] m = metrics.split(',')
		//set name
		this.revName = m[0]
		
		//set hasFSTMergeConflicts
		this.hasFSTMergeConflicts = Integer.parseInt(m[1])
		
		//set predictors
		this.loadPredictors(metrics)

	}
	
	public void loadPredictors(String metrics){
		this.predictors = new Hashtable<String, Integer>()
		String[] m = metrics.split(',')
		int editSameMC = Integer.parseInt(m[4]) - Integer.parseInt(m[5])
		this.predictors.put('editSameMC',editSameMC)
		int editSameFD = Integer.parseInt(m[6]) - Integer.parseInt(m[7])
		this.predictors.put('editSameFD',editSameFD)
		int ncEditSameMC = Integer.parseInt(m[8]) - Integer.parseInt(m[9])
		this.predictors.put('ncEditSameMC',ncEditSameMC)
		int ncEditSameFd = Integer.parseInt(m[10]) - Integer.parseInt(m[11])
		this.predictors.put('ncEditSameFd',ncEditSameFd)
		int editDiffMC = Integer.parseInt(m[12])
		this.predictors.put('editDiffMC',editDiffMC)
		int editDiffEditSame = Integer.parseInt(m[13])
		this.predictors.put('editDiffEditSame',editDiffEditSame)
		int editDiffAddsCall = Integer.parseInt(m[14])
		this.predictors.put('editDiffAddsCall',editDiffAddsCall)
		int editDiffEditSameAddsCall = Integer.parseInt(m[15])
		this.predictors.put('editDiffEditSameAddsCall',editDiffEditSameAddsCall)
		
	}
	
	public void runGitMerge(Extractor extractor){
		ExtractorResult er = extractor.getConflictingfiles(this.parent1, this.parent2)
		if(er.javaFilesWithConflict.size()>0){
			this.hasGitConflictsJava = 1
		}
		if(er.nonJavaFilesWithConflict.size()>0){
			this.hasGitConflictsNonJava = 1
		}
	}
	
	/*String header = 'rev_name,sha,hasFSTMergeConflicts,hasRealFSTMergeConflicts,hasGitConflictsJava,' +
				'hasGitConflictsNonJava,discarded,buildCompiles,testsPass,' +
				'editSameMC,editSameFD,ncEditSameMC,ncEditSameFd,editDiffMC,editDiffEditSame,' +
				'editDiffAddsCall,editDiffEditSameAddsCall\n'*/
	public String toString(){
		String result = this.revName + ',' + this.sha + ',' + this.hasFSTMergeConflicts + ',' + this.hasRealFSTMergeConflicts +
		',' + this.hasGitConflictsJava + ',' + this.hasGitConflictsNonJava + ',' +
		this.discarded + ',' + this.buildCompiles + ',' + this.testsPass + ',' +
		this.predictors.get('editSameMC') + ',' + this.predictors.get('editSameFD') +
		',' +this.predictors.get('ncEditSameMC') + ',' + this.predictors.get('ncEditSameFd') +
		',' + this.predictors.get('editDiffMC') + ',' + this.predictors.get('editDiffEditSame') +
		',' + this.predictors.get('editDiffAddsCall') + ',' + this.predictors.get('editDiffEditSameAddsCall')
		
		return result
	}
	
	

}
