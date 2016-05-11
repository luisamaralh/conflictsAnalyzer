package main


import java.util.ArrayList;

public class ConflictPrinter {

	static String conflictReportHeader 
	
	public static void setconflictReportHeader(){
		this.conflictReportHeader = ''
		String noPattern = SSMergeConflicts.NOPATTERN.toString()
		for(SSMergeConflicts c : SSMergeConflicts.values()){
			String type = c.toString()
			this.conflictReportHeader = this.conflictReportHeader +
			type + ', '
			if(!type.equals(noPattern)){
				this.conflictReportHeader = this.conflictReportHeader +
				type + 'DS, ' + type + 'CL, ' +
				type + 'IFP, '
			}
		}
		
		for(PatternSameSignatureCM p : PatternSameSignatureCM.values()){
			String cause = p.toString()
			String diffSpacing = cause + 'DS'
			this.conflictReportHeader = this.conflictReportHeader + cause + ', ' + diffSpacing + ', '
		}
		this.conflictReportHeader = this.conflictReportHeader + 'PossibleRenamings, EditSameMCWithoutConflicts, EditSameMCWithoutConflictsDS'
	}
	
	public static String getConflictReportHeader(){
		return this.conflictReportHeader
	}
	
	public static void printMergeScenariosBuildResult(String mergeScenario, boolean[] buildResults){
		String fileName = 'mergeScenariosBuild.csv'
		def out = new File(fileName)
		String row = ''
		if(!out.exists()){
			row = 'Merge_Scenario,BuildWasSuccessful,TestsFailed\n'
			out.append(row)
		}
		
		row = mergeScenario + ','
		
		if(buildResults[0]){
			row = row + '1,'
		}else{
			row = row + '0,'
		}
		
		if(buildResults[1]){
			row = row + '1\n'
		}else{
			row = row + '0\n'
		}
		out.append(row)
	}
	
	public static void printProjectData(Project p){
		String fileName = 'projectsPatternData.csv'
		def out = new File(fileName)
		
		if(!out.exists()){
		String row = 'Project, Merge_Scenarios, Conflicting_Scenarios_Non_Java_Only, Conflicting_Scenarios_Java, ' +
		this.conflictReportHeader + '\n'

		out.append(row)
		}

			out.append(p.toString() + '\n')

	}
	
	public static void updateProjectData(Project p){
		String fileName = "ResultData" + File.separator + p.getName() + File.separator + 'ProjectReport.csv'
		def out = new File(fileName)
		
		out.delete()
		out = new File(fileName)
		
		
		String row = 'Project, Merge_Scenarios, Conflicting_Scenarios_Non_Java_Only, Conflicting_Scenarios_Java, ' +
		this.conflictReportHeader + '\n'

		out.append(row)
		out.append(p.toString() + '\n')

	}
	public static void printMergeScenarioReport(MergeScenario mergeScenario, String projectName){


		File out = new File("ResultData" + File.separator + projectName + File.separator + 'MergeScenariosReport.csv')

		if(!out.exists()){
			String fileHeader = 'Merge_scenario, Total_Files, Files_Edited_By_One_Dev, ' +
					'Files_That_Remained_The_Same, Files_Added_By_Ove_Dev, Files_Merged, ' + 
					'Has_Non_Java_File_With_Conflicts, ' + 'Files_With_Conflicts, ' + 
					 this.conflictReportHeader + '\n'
			out.append(fileHeader)
		}
		out.append(mergeScenario.toString() + '\n')

		printMergeScenarioMetrics(mergeScenario, projectName)
		printConflictsReport(mergeScenario, projectName)
		if(!mergeScenario.filesWithMethodsToJoana.isEmpty()){
			printEditSameMCWithoutConflicts(mergeScenario, projectName)
		}
	}

	public static void printMergeScenarioMetrics(MergeScenario mergeScenario, String projectName){
		File out = new File('ResultData' + File.separator + projectName + File.separator +
				'Merge_Scenarios' + File.separator + mergeScenario.name + '.csv')
		if(!out.exists()){
			String header = 'File, Total_of_Conflicts, ' +
					'Conflicts_Inside_Methods, Methods_with_Conflicts, ' +
					'Conflicts_Outside_Methods, ' +
					this.conflictReportHeader + '\n'
			out.append(header)
		}

		out.append(mergeScenario.printMetrics() + '\n')


	}
	
	public static void printEditSameMCWithoutConflicts(MergeScenario mergeScenario, String projectName){
		File out = new File("ResultData" + File.separator + projectName + File.separator + 'EditSameMCWithoutConflicts.csv')
		
		def delimiter = '========================================================='
		out.append(delimiter + '\n')
		out.append('Revision: ' + mergeScenario.path + '\n')
		for(Map.Entry <String, ArrayList<MethodEditedByBothRevs>> entry : mergeScenario.filesWithMethodsToJoana.entrySet()){
			out.append('File: ' + entry.key + '\n')
			for(MethodEditedByBothRevs m : entry.value){
				String[] lines = m.linesToString()
				out.append('Method signature: ' + m.node.getName() + '\n')
				out.append('Left editions: ' + lines[0] + '\n')
				out.append('Right editions: ' + lines[1] + '\n')
				out.append('Different Spacing: ' + m.diffSpacing + '\n')
				out.append('Merged body:\n' + m.node.getBody() + '\n')
			}
		}
		out.append(delimiter)
	}
	
	
	
	public static void printConflictsReport(MergeScenario mergeScenario, String projectName){

		def out = new File("ResultData" + File.separator + projectName + File.separator + 'ConflictsReport.csv')

		def delimiter = '========================================================='
		out.append(delimiter)
		out.append '\n'

		out.append('Revision: ' + mergeScenario.path + '\n')

		for(MergedFile mergedFile: mergeScenario.getMergedFiles()){

			for(Conflict c: mergedFile.getConflicts()){

				def row = ['Conflict type: '+ c.getType() + '\n' + 
					'Number of Conflicts: ' + c.getNumberOfConflicts() + '\n' +
					'Different Spacing: ' + c.getDifferentSpacing() + '\n'  +
					'Consecutive Lines: ' + c.getConsecutiveLines() + '\n'+ 
					'Intersection: ' + c.getFalsePositivesIntersection() + '\n' +
					'Cause same signature: ' + c.getCauseSameSignatureCM() + '\n' +
					'Possible renaming: ' + c.getPossibleRenaming() + '\n' +
					'Conflict body: ' + '\n' + c.getBody() ]
				out.append row.join(',')
				out.append '\n'
				row = ['File path: ' + c.getFilePath()]
				out.append row.join(',')
				out.append '\n'

			}

		}
		out.append '\n'
		out.append(delimiter)
	}
	
	public static String auxPrintEditSameMCWithoutConflicts(int leftOrRight){
		
	}
	
	public static void main (String[] args){
		ConflictPrinter.setconflictReportHeader()
		println ConflictPrinter.getConflictReportHeader()
	}
}
