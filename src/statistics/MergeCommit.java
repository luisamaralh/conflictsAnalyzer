package statistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class MergeCommit {
	
	private String name;
	
	private String SHA;
	
	private String parent1;
	
	private String parent2;
	
	private String base;
	
	private String NumDevCategory;
	
	private HashMap<String, Integer> authors;
	
	public MergeCommit(String sha, String p1, String p2){
		this.SHA = sha;
		this.parent1 = p1;
		this.parent2 = p2;
		this.name = "rev_" + this.parent1.substring(0, 5) + "-"  + this.parent2.substring(0, 5);
		this.authors = new HashMap<String, Integer>();
	}
	
	public void analyzeNumberOfDevelopers(String clonePath){
		//git merge-base
		this.setMergeBase(clonePath);
		//get commit authors between base and mergecommit
		ArrayList<String> shas = this.getCommitList(clonePath);
		this.getAuthors(shas, clonePath);
		this.setNumDevCategory();
	}
	
	public void setNumDevCategory(){
		int numDevs = this.authors.size();
		if(numDevs == 1){
			this.NumDevCategory = NumDevCategories.oneDev.toString();
		}else if(numDevs == 2){
			this.NumDevCategory = NumDevCategories.twoDevs.toString();
		}else{
			this.NumDevCategory = NumDevCategories.moreThanTwoDevs.toString();
		}
	}
	
	public void getAuthors(ArrayList<String> shas, String clonepath){
		for(String sha : shas){
			String author = this.getAuthor(sha, clonepath);
			int v = 1;
			if(this.authors.containsKey(author)){
				Integer number = this.authors.get(author);
				v = number.intValue() + 1;
			}
			this.authors.put(author, v);
		}
	}
	
	public String getAuthor(String sha, String clonePath){
		String email = "";
		String cmd = "git show -s --format='%ae' " + sha;
		Process p;
		try {
			p = Runtime.getRuntime().exec(cmd, null, new File(clonePath));
			BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			while ((line=buf.readLine())!=null) {
				email = line;
			}
			p.getInputStream().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return email;	
	}
	
	public void setMergeBase(String clonePath){
		String cmd = "git merge-base " + this.parent1 + " " + this.parent2;
		Process p;
		try {
			p = Runtime.getRuntime().exec(cmd, null, new File(clonePath));
			BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			while ((line=buf.readLine())!=null) {
				this.base = line;
			}
			p.getInputStream().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<String> getCommitList(String clonePath){
		String cmd = "git rev-list " + this.base + ".." + this.SHA;
		Process p;
		ArrayList<String> shas = new ArrayList<String>();
		try {
			p = Runtime.getRuntime().exec(cmd, null, new File(clonePath));
			BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			while ((line=buf.readLine())!=null) {
				shas.add(line);
			}
			p.getInputStream().close();
			shas.add(this.base);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return shas;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSHA() {
		return SHA;
	}

	public void setSHA(String sHA) {
		SHA = sHA;
	}

	public String getParent1() {
		return parent1;
	}

	public void setParent1(String parent1) {
		this.parent1 = parent1;
	}

	public String getParent2() {
		return parent2;
	}

	public void setParent2(String parent2) {
		this.parent2 = parent2;
	}

	public String getNumDevCategory() {
		return NumDevCategory;
	}

	public void setNumDevCategory(String numDevCategory) {
		NumDevCategory = numDevCategory;
	}
	
	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

	public HashMap<String, Integer> getAuthors() {
		return authors;
	}

	public void setAuthors(HashMap<String, Integer> authors) {
		this.authors = authors;
	}

	public static void main(String[] args) {
		MergeCommit mc = new MergeCommit("31defd3ef60e09d15faa9ec0e1f8ccbac98e2b07", "4cc6915", "e3439f5");
		String clonePath = "/Users/paolaaccioly/Documents/Doutorado/workspace_empirical/conflictsAnalyzer/downloads2/andlytics";
		mc.analyzeNumberOfDevelopers(clonePath);
		System.out.println(mc.getBase());
	}
	
	
	

}
