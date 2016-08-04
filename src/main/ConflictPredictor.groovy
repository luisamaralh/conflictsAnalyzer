package main

import java.io.File
import java.io.IOException;
import java.util.ArrayList;
import java.util.List
import java.util.Map

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.ASTVisitor
import org.eclipse.jdt.core.dom.CompilationUnit
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration
import org.eclipse.jdt.core.dom.MethodInvocation


import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal
import de.ovgu.cide.fstgen.ast.FSTTerminal
import merger.FSTGenMerger
import util.Util

public abstract class ConflictPredictor {

	public FSTTerminal node

	public Map<ConflictPredictor, Boolean> predictors

	public String leftOrRight

	public boolean diffSpacing

	public String filePath

	public String packageName

	public FSTTerminal constructor

	public List<String> imports

	public String START_SEPARATOR

	public String END_SEPARATOR

	public String signature

	public ArrayList<Integer> leftLines

	public ArrayList<Integer> rightLines

	public String mergeScenarioPath

	public ConflictPredictor(FSTTerminal node, String mergeScenarioPath){
		this.node = node
		this.setLeftOrRight()
		this.setDiffSpacing()
		this.callBlame()
		this.setSeparatorStrings()
		this.mergeScenarioPath = mergeScenarioPath
		this.retrieveFilePath()
		this.annotatePredictor()
		this.setSignature()
	}

	public void setLeftOrRight(){
		this.leftOrRight = 'both'
	}

	public void annotatePredictor(){
		String body = this.node.getBody()
		String [] lines = body.split('\n')
		String firstLine = this.START_SEPARATOR + lines[0]
		String lastLine = this.END_SEPARATOR + lines[lines.length-1]
		String newBody = firstLine + '\n'
		for(int i = 1; i < (lines.length-1); i++){
			newBody = newBody + lines[i] + '\n'
		}
		newBody = newBody + lastLine

		this.node.setBody(newBody)
	}

	public void callBlame(){
		if(!this.diffSpacing){
			File[] files = this.createTempFiles()
			Blame blame = new Blame()
			String result = blame.annotateBlame(files[0], files[1], files[2])
			this.node.setBody(result)
			this.deleteTempFiles(files)
		}
	}

	public void deleteTempFiles(File[] files){
		File tmpDir = new File(files[0].getParent())
		files[0].delete();
		files[1].delete();
		files[2].delete();
		tmpDir.delete();

	}
	public File[] createTempFiles(){
		String [] splitNodeBody = this.splitNodeBody()
		long time = System.currentTimeMillis()
		File tmpDir = new File(System.getProperty("user.dir") + File.separator + "fstmerge_tmp"+time);
		tmpDir.mkdir()
		File fileVar1 = File.createTempFile("fstmerge_var1_", "", tmpDir)
		File fileBase = File.createTempFile("fstmerge_base_", "", tmpDir)
		File fileVar2 = File.createTempFile("fstmerge_var2_", "", tmpDir)
		fileVar1.append(splitNodeBody[0])
		fileBase.append(splitNodeBody[1])
		fileVar2.append(splitNodeBody[2])
		File[] result = [fileVar1, fileBase, fileVar2]
		return result
	}

	public void setDiffSpacing(){
		String [] nodeBodyWithoutSpacing = this.getNodeWithoutSpacing()
		if(nodeBodyWithoutSpacing[0].equals(nodeBodyWithoutSpacing[1]) ||
		nodeBodyWithoutSpacing[2].equals(nodeBodyWithoutSpacing[1])){
			this.diffSpacing = true
		}else{
			this.diffSpacing = false
		}
	}

	public String[] getNodeWithoutSpacing() {
		String [] splitNodeBody = this.splitNodeBody().clone()
		String [] nodeBodyWithoutSpacing = this.removeInvisibleChars(splitNodeBody)
		return nodeBodyWithoutSpacing
	}

	public String[] splitNodeBody(){
		String [] splitBody = ['', '', '']
		String[] tokens = this.node.getBody().split(FSTGenMerger.MERGE_SEPARATOR)
		splitBody[0] = tokens[0].replace(FSTGenMerger.SEMANTIC_MERGE_MARKER, "").trim()
		splitBody[1] = tokens[1].trim()
		splitBody[2] = tokens[2].trim()

		return splitBody
	}

	public String[] removeInvisibleChars(String[] input){
		input[0] = input[0].replaceAll("\\s+","")
		input[1] = input[1].replaceAll("\\s+","")
		input[2] = input[2].replaceAll("\\s+","")
		return input;
	}

	public void retrieveFilePath(){

		int endIndex = this.mergeScenarioPath.length() - 10;
		String systemDir = this.mergeScenarioPath.substring(0, endIndex);

		this.filePath = systemDir + this.retrieveFolderPath(this.node);
	}

	public String retrieveFolderPath(FSTNode n){
		String filePath = "";
		String nodetype = n.getType();

		if(nodetype.equals("CompilationUnit")){
			this.setPackageName(n)
			this.setImportList(n)

		}

		if(nodetype.equals("ClassDeclaration")){
			this.setConstructor(n)

		}


		if(nodetype.equals("Java-File") || nodetype.equals("Folder")){

			filePath = this.retrieveFolderPath(n.getParent()) + File.separator + n.getName();

			return filePath;

		}else if(nodetype.equals("Feature")){

			return "";

		}else{

			return this.retrieveFolderPath(n.getParent());
		}
	}

	public setImportList(FSTNode node){
		this.imports = new ArrayList<String>()
		FSTNonTerminal nonterminal = (FSTNonTerminal) node;
		ArrayList<FSTNode> children = nonterminal.getChildren()
		int i = 0

		while(i < children.size()){
			FSTNode child = children.elementData(i)
			if(child.getType().equals('ImportDeclaration')){
				imports.add(child.getBody().replace("import ", "").replace(";", ""))
			}
			i++
		}
	}

	public setPackageName(FSTNode node){
		this.packageName = ''
		boolean foundPackage = false
		FSTNonTerminal nonterminal = (FSTNonTerminal) node;
		ArrayList<FSTNode> children = nonterminal.getChildren()
		int i = 0

		while(!foundPackage && i < children.size()){
			FSTNode child = children.elementData(i)
			if(child.getType().equals('PackageDeclaration')){
				String [] tokens = child.getBody().split(' ')
				this.packageName = tokens[1].substring(0, tokens[1].length()-1)
				foundPackage = true
			}
			i++
		}

	}

	public setPackageName(String packageN){
		this.packageName = packageN
	}

	public void setConstructor(FSTNode node) {
		boolean foundConstructor = false
		FSTNonTerminal nonterminal = (FSTNonTerminal) node;
		ArrayList<FSTNode> children = nonterminal.getChildren()
		int i = 0
		FSTNode privateConst = null

		while(!foundConstructor && i < children.size()){
			FSTNode child = children.elementData(i)
			if(child.getType().equals('ConstructorDecl')){

				FSTTerminal childTerm = (FSTTerminal) child;
				List<String> modifiersList = Util.getModifiersList(childTerm.getBody())
				if(!Util.isPrivateMethod(modifiersList))
				{
					this.constructor = child
					foundConstructor = true
				}else
				{
					privateConst = child
				}
			}
			i++
		}
		if(!foundConstructor)
		{
			this.constructor = privateConst
		}
	}

	public void setSeparatorStrings(){
		this.START_SEPARATOR = '// START ' + this.node.getName() + '//'
		this.END_SEPARATOR = '// END ' + this.node.getName() + '//'
	}

	public String getFilePath() {
		return filePath;
	}

	public void setSignature(){
		String [] tokens = this.filePath.split(File.separator)
		String className = tokens[tokens.length-1]
		className = className.substring(0, className.length()-5)
		String methodName = Util.simplifyMethodSignature(this.node.getName())
		String returnType;
		if(this.node.getType().equals("ConstructorDecl"))
		{
			returnType = "void"
		} else {
			returnType = Util.getMethodReturnType(this.node.getBody(), imports, packageName, new File(filePath).getParent())
		}
		this.signature = returnType + " " +this.packageName + '.' + className + '.' + Util.includeFullArgsTypes(methodName, imports, packageName, new File(filePath).getParent())
	}

	public String getSignature() {
		return signature;
	}
	public void setSignature(String signature) {
		this.signature = signature;
	}
	public ArrayList<Integer> getLeftLines() {
		return leftLines;
	}
	public void setLeftLines(ArrayList<Integer> leftLines) {
		this.leftLines = leftLines;
	}
	public ArrayList<Integer> getRightLines() {
		return rightLines;
	}
	public void setRightLines(ArrayList<Integer> rightLines) {
		this.rightLines = rightLines;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public List<String> getImportsList()
	{
		imports
	}

	public FSTTerminal getConstructor() {
		return constructor;
	}

	public void assignLeftAndRight(){
		this.leftLines  = new ArrayList<Integer>()
		this.rightLines = new ArrayList<Integer>()
		File file = new File(this.filePath)
		int i = 1;
		String newFile = ''

		boolean isMethodBody = false

		file.eachLine {

			if(it.contains(this.START_SEPARATOR)){
				isMethodBody = true
				String s = it.replace(this.START_SEPARATOR, '')
				newFile = newFile + this.processMethodLine(s, i) + '\n'
			}else{

				if(!isMethodBody){

					newFile = newFile + it + '\n'

				}else{

					if(it.contains(this.END_SEPARATOR)){
						isMethodBody = false
						String s2 = it.replace(this.END_SEPARATOR, '')
						newFile = newFile + this.processMethodLine(s2,i) + '\n'
					}else{
						newFile = newFile + this.processMethodLine(it, i) + '\n'
					}

				}
			}



			i++
		}

		//delete old file and write new content
		file.delete()
		new File(this.filePath).write(newFile)
	}


	public String processMethodLine(String line, int i){
		String result = ''
		if(line.contains(Blame.LEFT_SEPARATOR)){
			this.leftLines.add(new Integer(i))
			result = line.replace(Blame.LEFT_SEPARATOR, '')
		}else if(line.contains(Blame.RIGHT_SEPARATOR)){
			this.rightLines.add(new Integer(i))
			result = line.replace(Blame.RIGHT_SEPARATOR, '')
		}else{
			result = line
		}
		return result
	}

	public String[] linesToString(){
		String left = '['
		String right = '['
		for(Integer i : this.leftLines){
			left = left + i + ','
		}
		for(Integer x : this.rightLines){
			right = right + x + ','
		}
		left = left + ']'
		right = right + ']'
		String[] result = [left,right]

		return result
	}

	/*this method checks for objects' reference equality*/
	public boolean referenceEquals(ConflictPredictor b){
		boolean result = (this == b)
		return result
	}

	private void saveReference(ConflictPredictor predictor, ArrayList<Integer> methodInvocationLines){

		/*instantiates the new hashmap if necessary*/
		if(this.predictors==null){
			this.predictors = new HashMap<ConflictPredictor, Boolean>()
		}
		/*checks whether the editions made added the method call*/
		Boolean editionsAddedMethodCall = this.editionsAddedMethodCall(predictor, methodInvocationLines)

		/*saves the reference in the hashmap*/
		this.predictors.put(predictor,editionsAddedMethodCall)

	}

	private boolean editionsAddedMethodCall(ConflictPredictor predictor, ArrayList<Integer> methodInvocationLines){
		boolean editionsAddedMethodCall = false
		if(this.leftOrRight.equals('left')){
			editionsAddedMethodCall = !Collections.disjoint(predictor.rightLines, methodInvocationLines)
		}else if(this.leftOrRight.equals('right')){
			editionsAddedMethodCall = !Collections.disjoint(predictor.leftLines, methodInvocationLines)
		}else if (this.leftOrRight.equals('both')){
			editionsAddedMethodCall = (!Collections.disjoint(predictor.leftLines, methodInvocationLines)) ||
					(!Collections.disjoint(predictor.rightLines, methodInvocationLines))
		}

		return editionsAddedMethodCall
	}

	public void lookForReferencesOnConflictPredictors(Map<String, Integer> filesWithConflictPredictors){

		/*for each file containing conflict predictors*/
		for(String filePath : filesWithConflictPredictors.keySet()){
			ArrayList<ConflictPredictor> predictors = filesWithConflictPredictors.get(filePath)

			/*for each conflict predictor on that file*/
			for(ConflictPredictor predictor : predictors ){
				/*check if predictor is not the same instance of this,
				 *  if it is an edited method, and if the changes come
				 *  from different merge commit parents*/
				if(this.mightContainReferences(predictor)){

					this.lookForReferenceOnConflictPredictor(predictor)

				}
			}
		}

	}

	private boolean mightContainReferences(ConflictPredictor predictor){
		boolean mightContainReferences = false

		/*if it is not the same predictor as this*/
		if(!this.equals(predictor)){
			/*if the predictor is an edited method
			 * (not considering the different spacing predictors, 
			 * and the changes come from both left and right code versions*/
			if((predictor instanceof EditSameMC || predictor instanceof EditDiffMC) &&
			!(predictor.diffSpacing) && this.changesComeFromDifferentCommits(predictor)){
				mightContainReferences = true
			}

		}
		return mightContainReferences
	}


	/*check of changes on edited methods come from different merge commit parents
	 * as it does not makes sense to analyse conflicts from changes made by same commit*/
	public boolean changesComeFromDifferentCommits(ConflictPredictor predictor){
		boolean changesComeFromDifferentCommits = false

		if((this instanceof EditSameMC) || (predictor instanceof EditSameMC)){
			changesComeFromDifferentCommits = true
		}else {
			EditDiffMC p = (EditDiffMC) predictor
			if((this.leftOrRight.equals('left') && p.leftOrRight.equals('right')) ||
			(this.leftOrRight.equals('right') && p.leftOrRight.equals('left'))){
				changesComeFromDifferentCommits = true
			}
		}

		return changesComeFromDifferentCommits
	}

	private void lookForReferenceOnConflictPredictor(ConflictPredictor predictor){

		/*Step 1: check for potential EditDiffMC when grepping
		 * the method name inside the edited method */
		if(this.containsTextualReference(predictor)){

			/*Step 2: in case the edited method has
			 *  a textual reference, remove false positives using the
			 * method reference finder*/
			ArrayList<Integer> result = this.checkForClassReference(predictor)
			if(!result.empty){
				this.saveReference(predictor, result)
			}
		}


	}

	/*Checks if the string representing the method body declaration
	 * contains a textual reference to this method.
	 * Might report false positives. */
	private boolean containsTextualReference(ConflictPredictor predictor){
		boolean containsTextualReference = false
		String methodBody = this.extractMethodBody(predictor.node.body)
		String thisMethodName = this.getMethodName(this)
		if(methodBody.contains(thisMethodName)){
			containsTextualReference = true
		}
		return containsTextualReference
	}

	private String getMethodName(ConflictPredictor predictor){
		String methodName = ''
		String predictorName = predictor.node.name
		String[] split = predictorName.split('\\(')
		methodName = split[0]
		return methodName
	}

	/*Receives as input the string of the method and returns just
	 *  the lines inside the method body declaration.
	 *  It helps to remove false positives before running
	 *  the compiler analysis*/
	private String extractMethodBody(String method){
		String methodBody = ''

		ArrayList<String> temp = method.split('\n')
		int firstBracket = 0
		int lastBracket = temp.size() -1
		boolean foundFirstBracket, foundLastBracket = false
		String a = ''

		/*get the first bracket index*/
		while(!foundFirstBracket){
			a = temp.elementData(firstBracket)
			if(a.contains('{')){
				foundFirstBracket = true
			}else{
				firstBracket++
			}
		}

		/*gets the last bracket index*/
		while(!foundLastBracket){
			a = temp.elementData(lastBracket)
			if(a.contains('}')){
				foundLastBracket = true
			}else{
				lastBracket--
			}
		}

		/*gets the string representing the method body declaration*/
		String [] temp2 = temp.subList(firstBracket + 1, lastBracket)

		for(String s: temp2){
			methodBody = methodBody + s + '\n'
		}

		return methodBody
	}


	private ArrayList<Integer> checkForClassReference(ConflictPredictor predictor) throws IOException{
		ArrayList<Integer> invocationLines = new ArrayList<Integer>()

		/*sets the predictor signature*/
		String []  temp = predictor.signature.split('\\.')
		String predictorSignature = temp[temp.length-1]

		//get file contents
		String contents = getFileContents(predictor.filePath)

		/*setting compiler environment variables*/
		/*FIXME change classPath if needed*/

		//set classPath
		String[] classPaths = null

		/*set source folders*/
		String[] source = this.fillSources(predictor)

		/*set encodings*/
		String[] encoding = this.fillEncodings(source.length)

		/*set className*/
		File filePredictor = new File(predictor.filePath)
		String classname = filePredictor.getName()

		if(contents!=null){
			println 'Starting to parse and analyse class ' + predictor.filePath
			// Create the ASTParser which will be a CompilationUnit
			ASTParser parser = ASTParser.newParser(AST.JLS8)
			parser.setKind(ASTParser.K_COMPILATION_UNIT)
			parser.setSource(contents.toCharArray())

			//Parsing
			parser.setEnvironment(classPaths, source, encoding, true);
			parser.setBindingsRecovery(true);
			parser.setResolveBindings(true);
			parser.setUnitName(classname);
			Map options = JavaCore.getOptions();
			options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
			parser.setCompilerOptions(options);
			CompilationUnit parse = (CompilationUnit) parser.createAST(null);

			parse.accept(new ASTVisitor() {
						private MethodDeclaration activeMethod;

						/**
				 * @param MethodDeclaration node
				 * This method visits every method declaration in the compiled class
				 * @return true if and only if this node is the method containing this
				 * method invocation. Returns false otherwise
				 */
						@Override
						public boolean visit(MethodDeclaration node) {
							activeMethod = node;
							IMethodBinding activeMethodBinding = activeMethod.resolveBinding()
							/*only visits the active method containing a textual reference 
					 * to this method*/
							if(isTheMethodCallingThisMethod(predictorSignature, activeMethodBinding)){
								return super.visit(node)
							}else{
								return false
							}
						}

						/**
				 * @param MethodInvocation node
				 * This method visits every method invocation inside the method of interest
				 * @return true if and only if it is a method invocation to this method.
				 * Returns false otherwise
				 */
						@Override
						public boolean visit(MethodInvocation node) {
							IMethodBinding thisMethodBinding = node.resolveMethodBinding()
							boolean isThisMethod = methodInvocationMatchesThisMethod(thisMethodBinding)
							if(isThisMethod){
								int lineNumber = parse.getLineNumber(parse.getExtendedStartPosition(node))
								invocationLines.add(lineNumber)
							}
							return super.visit(node)
						}
					})

		}
		println 'Finished to parse and analyse class ' + predictor.filePath
		return invocationLines
	}

	private String[] fillSources(ConflictPredictor predictor){
		String[] sources = []
		ArrayList<String>folders = new ArrayList<String>()
		String rootPath = this.mergeScenarioPath.substring(0, this.mergeScenarioPath.length()-10)
		File rootDir = new File(rootPath)
		File [] fileList = rootDir.listFiles()
		for(File file : fileList){
			if(file.isDirectory()){
				folders.add(file.getAbsolutePath())
			}
		}

		File filePredictor = new File(predictor.filePath)
		File thisFileMethod = new File(this.filePath)

		folders.add(filePredictor.getParent())
		folders.add(thisFileMethod.getParent())
		folders.add(rootPath)
		sources = new String[folders.size()]
		for(int i = 0; i < folders.size(); i++){
			sources[i] = folders.get(i)
		}

		return sources
	}

	private String[] fillEncodings(int amount){
		String[] encodings = new String[amount]
		for(int i = 0; i<amount; i++){
			encodings[i] = 'UFT_8'
		}
		return encodings
	}

	public boolean isTheMethodCallingThisMethod(String predictorSignature, IMethodBinding activeMethod){
		boolean callsThisMethod = false
		String activeMethodSignature= this.simplifyMethodSignature(activeMethod)

		/*if active method is indeed the method calling this method*/
		if(activeMethodSignature.contains(predictorSignature)){
			callsThisMethod = true
		}
		return callsThisMethod
	}

	/**
	 * @param methodInvocation
	 * @return true if and only if the method invocation belongs to the same
	 * class and same method of this method. Returns false otherwise
	 */
	public boolean methodInvocationMatchesThisMethod(IMethodBinding methodInvocation){
		boolean isTheSameMethod = false

		if( methodInvocation!=null && methodInvocation.getKey()!=null){
			String methodInvocationClass = this.simplifyClassName(methodInvocation)
			String methodInvocationSignature = this.simplifyMethodSignature(methodInvocation)
			String [] temp = this.filePath.split('/')
			String thisMethodClass = this.packageName + '.' + temp[temp.length-1].split('\\.')[0]
			temp = this.signature.split('\\.')
			String thisMethodSignature = temp[temp.length-1]

			/*if method invocation is indeed this method*/
			if(methodInvocationClass.contains(thisMethodClass) &&
			methodInvocationSignature.contains(thisMethodSignature)){
				isTheSameMethod = true
			}
		}

		return isTheSameMethod
	}

	private String simplifyClassName(IMethodBinding methodInvocation){
		String className = ''
		String temp = (methodInvocation.getKey().split("\\."))[0]
		className = temp.replaceAll(";", "")
		className = className.replace("/", ".")
		if(className.startsWith("L"))className = className.replaceFirst("L","")
		return className
	}
	private String simplifyMethodSignature(IMethodBinding mb) {
		String simplifiedMethodSignature = ((mb.toString()).replaceAll("(\\w)+\\.", "")).replaceAll("\\s+","");
		return simplifiedMethodSignature;
	}

	public String getFileContents(String filePath){
		String contents = ''
		File file = new File(filePath)
		file.eachLine {
			contents = contents + it + '\n'
		}
		return contents
	}
	
	public String toString(){
		String result = ''
		//TODO
		return result
	}

}
