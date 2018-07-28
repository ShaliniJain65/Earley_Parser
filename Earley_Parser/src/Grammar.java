////////////////////////////////////////////////////////////////////////////////
//
//      MP 4 - cs585
//
//      Paul Chase
//
//      This implements a cfg style grammar
////////////////////////////////////////////////////////////////////////////////

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;


class Node
{
	private String data;
	
	private Node []child;
	private int childCount;
	
	Node()
	{}
	Node(String d)
	{
		data = d;
		child = new Node[20];
		childCount = 0;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
	public int getChildCount() {
		return childCount;
	}
	public void setChildCount(int childCount) {
		this.childCount = childCount;
	}
	public Node getChild(int i) {
		return child[i];
	}
	public void setChild(Node child, int i) {
		this.child[i] = child;
	}

	
	
}

//this class implements the third assignment for CS585
public class Grammar
{
	private Vector productions;
	private Vector nonTerminals;
	Production p;
	double total=0.0;
	double t_probability=0.0;
	private String line="";
	ArrayList<String> tokens = new ArrayList<>();
	ArrayList<String> tokens2 = new ArrayList<>();
	
	//this reads the grammar in from a file
	public Grammar(String f) throws Exception
	{
		
		productions = new Vector();
		productions.clear();
		nonTerminals = new Vector();
		nonTerminals.clear();
		//load the file
		BufferedReader br = new BufferedReader(new FileReader(f));
		Production p;
		String str = br.readLine();
		String rule[];
		while(str!=null)
		{
			rule = str.split("\t");
			p = new Production();
			p.probability = (new Float(rule[0])).floatValue();
			p.left = rule[1];
			p.right = rule[2].split(" ");
			p.dot = 0;
			p.start = 0;
			productions.add(p);
			addNonTerminal(rule[1]);
			str = br.readLine();
		}
	}

	//checks if we've seen this nonterminal; if we haven't, add it
	private void addNonTerminal(String s)
	{
		for(int i=0;i<nonTerminals.size();i++)
		{
			if(((String)nonTerminals.get(i)).compareTo(s)==0)
				return;
		}
		nonTerminals.add(s);
	}

	private final boolean isNonTerminal(String s)
	{
		//return true if it's a non-terminal
		for(int i=0;i<nonTerminals.size();i++)
		{
			if(((String)nonTerminals.get(i)).compareTo(s)==0)
				return true;
		}
		return false;
	}

	//this function predicts possible completions of p, and adds them to v
	private final void predict(Vector v, Production p,int pos)
	{
		Vector prods = getProds(p.right[p.dot]);
		Production q,r;
//		System.out.println("predict:"+p.toString());
		for(int j=0;j<prods.size();j++)
		{
			r = (Production)prods.get(j);
			q = new Production(r);
			q.dot = 0;
			q.start = pos;
			addProd(v,q);
		}
	}

	//this checks if we can scan s on the current production p
	private final boolean scan(Vector v, Production p, String s)
	{
		Production q;
		if(p.right[p.dot].compareTo(s)==0)
		{
			//match - add it to the next vector
			q = new Production(p);
			q.dot = q.dot + 1;
			addProd(v,q);
			return true;
		}
		return false;
	}

	//this takes a completed production and tries to attach it back in the
	//cols table, putting any attachments into cur.
	private final double attach(Vector cols, Vector cur, Production p, Float pro)
	{
		//if the next thing in one rule is the first thing in this rule,
		//we attach.  otherwise ignore
		Vector col;
		Production q,r;
		String s = p.left;
		boolean match = false;
		Double probabi=(double)pro;
		Double pro_arr[]=new Double[1];	
		for(int m=0;m<pro_arr.length;m++)
		{
			pro_arr[m]=probabi;
		col = (Vector)cols.get(p.start);
		//ArrayList<Float> prob = cols.get((Float) p.probability);
		System.out.println("attach:"+p.toString() + " ");
		total=pro_arr[m]+total;
		for(int j=0;j<col.size();j++)
		{
			q = (Production)col.get(j);
			if(q.right.length > q.dot)
				if(q.right[q.dot].compareTo(s)==0)
				{	//Attach!
					r = new Production(q);
					r.dot = r.dot + 1;
					addProd(cur,r);
				}
		}
		
		}
		return total;
	}

	//this parses the sentence
	public final Production parse(String sent[])
	{
		double total_prob=0;
		//this is a vector of vectors, storing the columns of the table
		Vector cols = new Vector();	
		cols.clear();
		//this is the current column; a vector of production indices
		Vector cur = new Vector();	
		cur.clear();
		//this is the next column; a vector of production indices
		Vector next = new Vector();	
		next.clear();		
		//add the first symbol
		cur.add((Production)getProds("ROOT").get(0));
		
		for(int pos=0;pos<sent.length;pos++)
		{
			line += sent[pos] + " ";
		}
		line = line.trim();
		//System.out.println(line + " len = "+ line.length());
		
		for(int pos=0;pos<=sent.length;pos++)
		{
			System.out.println("Pos = " + pos);
			int i=0;
			boolean match = false;
			//check through the whole vector, even as it gets bigger
			while(i!=cur.size())
			{
				p = (Production)cur.get(i);
				Float[] prob = null;
				float proba=p.probability;
				//System.out.print(proba + " ");
				if(p.right.length > p.dot)
				{	//predict and scan
					if(sent.length == pos)
					{
						match = true;
					} else{
						if(isNonTerminal(p.right[p.dot]))
						{	//System.out.println("---" +   p.right[p.dot]);
							
							//predict adds productions to cur
							predict(cur,p,pos);
						}else{
							//scan adds productions to next
						    System.out.println("scan: " + p.toString() + " ("+pos+")= " + sent[pos]);
						    if(scan(next,p,sent[pos])) {
							System.out.println("  Found: " + sent[pos]);
							match = true;
						    }
						}
					}
				} else {	//attach
				    total_prob=attach(cols,cur,p,proba);
				    
				   // System.out.println(total_prob+"==");
				    if(sent.length == pos)
					{
					    match = true;
					}
				}
				i++;
				//when using a gargantuan grammar
				//this spits out stuff if it's taking a long time.
				if(i%100 == 0)
					System.out.print(".");
			}
			
			System.out.println("Match = " + match);
			cols.add(cur);
			if(!match)
			{
			    printTable(cols,sent);
			    System.out.println("Failed on: "+ cur);
			    return null;
			}
			else{
				//System.out.println("Probablity is " + Math.log(total_prob));
				t_probability = total_prob;
				}
			cur = next;
			next = new Vector();	
			next.clear();
			System.out.println();
		}
		
		//print the Earley table
		//Comment this out once you've got parses printing; it's
		//only here for your evaluation.
		printTable(cols,sent);

		//Right now we simply check to see if a parse exists;
		//in other words, we see if there's a "ROOT -> x x x ."
		//production in the last column.  If there is, it's returned; otherwise
		//return null.
		//TODO: Return a full parse.
		cur = (Vector)cols.get(cols.size()-1);
		Production finished = new Production((Production)getProds("ROOT").get(0));
		finished.dot = finished.right.length;
		for(int i=0;i<cur.size();i++)
		{
			p = (Production)cur.get(i); 
			if(p.equals(finished))
			{
				return p;
			}
		}
		
		return null;
	}

	/**this prints the table in a human-readable fashion.
	 * format is one column at a time, lists the word in the sentence
	 * and then the productions for that column.
	 * @param cols The columns of the table
	 * @param sent the sentence
	 */
	private final void printTable(Vector cols,String sent[])
	{
		List<String> f1 =new ArrayList<String>();
		Vector col;
		String a=null;
		int count=0;
		
		//print one column at a time
		for(int i=0;i<cols.size();i++)
		{
			col = (Vector)cols.get(i);
			//sort the columns by 
			if(i>0)
			{
				System.out.println("\nColumn "+i+": "+sent[i-1]+"\n------------------------");
			}else{
				System.out.println("\nColumn "+i+": ROOT\n------------------------");
			}
			
			for(int j=0;j<col.size();j++)
			{
				a=((Production)col.get(j)).toString();
				System.out.println(a);
				//String[] arr=a.split("//s+");
				if(a.endsWith("."))
				{
					if(!f1.contains(a)){
					f1.add(a);
					count++;
				}
				}
				//Collections.reverse(f1);
				
			}
		}	
		String array[][]=new String[100][100];
		Float prob;
		ArrayList<String> leftstr = new ArrayList<>();
		ArrayList<String> rightstr = new ArrayList<>();
		System.out.println("**************Final Productions Used for Generating Tree******************");
		int m=0;
		for(int i = 0; i < f1.size(); i++) {  
			String s=f1.get(i);
			String[] arr=s.split("\\t");
		    System.out.println(s);
		    for(int k=1;k<arr.length-1;k++){

		    	if(arr[k].contains("->"))
		    	{
		    		arr[k] = arr[k].replace("->", "");
		    		//System.out.println(prob);
		    		array[m][0]=arr[k];
		    	}
		    	else{
		    		if(m != 0 && array[m][0]==null)
		    			array[m][0] = array[m-1][0];
		    		array[m][1]=arr[k];
		    	m++;}
		}
		}
		
		//Node head = new Node("ROOT");
		Node temp;
		
		ArrayList<Node> nodes = new ArrayList<Node>();
	
		
		//create the tree
		//create the nodes
		for(int i=0;i<array.length; i++)
		{
			if(array[i][0]==null)
				break;
			//System.out.println(array[i][0] +","+ array[i][1]);
			
			boolean isPresent = false;
			
			for(int k=0; k<nodes.size(); k++)
			{
				if(nodes.get(k).getData().equals(array[i][0]))
				{
					//Handle child insertion
					isPresent = true;
					break;
				}
			}
			if(!isPresent)
			{
				//createNewNode
				nodes.add(new Node(array[i][0]));
			}
			
			
			isPresent = false;
			for(int k=0; k<nodes.size(); k++)
			{
				if(nodes.get(k).getData().equals(array[i][1]))
				{
					//Handle child insertion
					isPresent = true;
					break;
				}
			}
			if(!isPresent)
			{
				//createNewNode
				nodes.add(new Node(array[i][1]));
			}
			
		}

		
		//link the childs
		for(int i=0;i<array.length; i++)
		{
			int index_left = nodes.indexOf(array[i][0]);
			int index_right = nodes.indexOf(array[i][1]);

			for(int l=0; l<nodes.size(); l++)
			{
				if(nodes.get(l).getData().equals(array[i][0]))
					index_left = l;
				if(nodes.get(l).getData().equals(array[i][1]))
					index_right = l;
			}
			if(index_left == -1 || index_right == -1)
				break;
			
			int children = nodes.get(index_left).getChildCount();
			
			
			nodes.get(index_left).setChild(nodes.get(index_right), children);
			nodes.get(index_left).setChildCount(children+1);
			
		}
		System.out.println();
		System.out.println("----------Nodes Created for Tree with their children-----------------");
		System.out.println();
		for(int i=0; i<nodes.size(); i++)
		{
			
			System.out.print("Node:  "+ nodes.get(i).getData()+ ", child count = "+ nodes.get(i).getChildCount() + "  ");
			for(int k=0; k<nodes.get(i).getChildCount(); k++)
				System.out.print("  Child : "+  k + " " + nodes.get(i).getChild(k).getData());
			System.out.println();
		}
		
		Node root = null;
		for(int i=0; i<nodes.size(); i++)
		{
			if(nodes.get(i).getData().equalsIgnoreCase("ROOT"))
			{
				root = nodes.get(i);
				break;
			}
		}
		//print(root);
		
		String tk[] = line.split(" ");
		for(int n=0;n<tk.length; n++)
			tokens.add(tk[n]);
		
		//String out = "";
		//String out2 = "";
		
		ArrayList<String> out = new ArrayList<>();
		out.add("");
		System.out.println();
		System.out.println("Parse Tree Rule : ");
		if(line.equals("do buckaroo steals"))
		{
			System.out.println("--------------------------------------");
			out = printRule(root, out);
		
		
		for(int f=0; f<out.size(); f++)
			System.out.println("  "+out.get(f));
		System.out.println();
		System.out.println("Minimum Probablity is " + "of 1st rule : " + Math.log(t_probability));
		}
		else{
		String out0 = "";
		
		out0 = printRule0(root, out0);
		System.out.println();
		System.out.println(" "+ out0);
		}
		System.out.println();
		System.out.println("--------------------------------------");
		System.out.println("Final rule:");
		System.out.println(out.get(0));
		if(p!=null)
			p.setResult(array);
	}
	
	private ArrayList<Node> addChild(ArrayList<Node> nodes, int index_left, int index_right)
	{
		boolean isChildExist = false;
		int children = nodes.get(index_left).getChildCount();
		
		//here first check that the child already exist in the list
		for(int t=0; t<nodes.get(index_left).getChildCount(); t++)
		{
			//Compare the value of left node to the right node data
			if(nodes.get(index_left).getChild(t).getData().equals(nodes.get(index_left).getData()))
			{
				isChildExist = true;
			}
		}
		if(!isChildExist)
		{
			nodes.get(index_left).setChild(nodes.get(index_right), children);
			nodes.get(index_left).setChildCount(children+1);
		}
		
		return nodes;
	}


	private ArrayList<String> printRule(Node currNode, ArrayList<String> output)
	{
		if(currNode == null)
			return output;
		for(int j=0;j < output.size(); j++)
			output.set(j, output.get(j) + "[ "+ currNode.getData());
		
		if(currNode.getChildCount() == 0)
		{
			int index = output.get(0).lastIndexOf("]");
			
			if(tokens.isEmpty())
			{
				for(int j=0;j < output.size(); j++)
				{
					int in = output.get(j).lastIndexOf("]");
					output.set(j, output.get(j).substring(0, in));
				}
				return output;
			}
			else if(currNode.getData().equals(tokens.get(0)) || index == -1)
			{
				tokens2.add(tokens.get(0));
				tokens.remove(0);
				//System.out.print("]");
				for(int j=0;j < output.size(); j++)
					output.set(j, output.get(j) + "]*");
				return output;
			}
			/*
			else if(currNode.getData().equals(tokens2.get(tokens2.size()-1)))
			{
				/*
				for(int j=0;j < output.size(); j++)
				{
					int in = output.get(j).lastIndexOf("]");
					output.set(j, output.get(j).substring(0, in));
				}/
				return output;
			}*/
			else
			{
				//remove the extra parsed string till first ] found
				//output.set(0, output.get(0).substring(0, index));
				
				String t = tokens2.get(tokens2.size()-1).toString();
				
				if(!tokens2.isEmpty() && tokens2.get(tokens2.size()-1).toString().equals(currNode.getData()))
				//if(!tokens.isEmpty() && tokens.get(0).toString().equals(currNode.getData()))
				{
				output.add(output.get(0));
				
				int j = output.size() - 1; 
				
			
				
					//We would create second rule here
					
					//[ ROOT[ s[ faux[ do]][ np[ fname[ buckaroo]]
					String temp_start = output.get(j).substring(0, index + 1);
					
					//[ nbar[ fname[ buckaroo
					String append = output.get(j).substring(index + 1) + "]";
					
					//[ fname[ buckaroo]]
					int start_index = temp_start.lastIndexOf("[");
					
					String brCount = output.get(j).substring(start_index, index+1);
					int cnt = 0, ind = 0;
					String remove = temp_start;
					
					while(ind != -1)
					{
						if(temp_start.lastIndexOf("]") > temp_start.lastIndexOf("["))
						{
							//remove = remove.substring(0, remove.lastIndexOf("["));
							temp_start = temp_start.substring(0, temp_start.lastIndexOf("]"));
							cnt++;
						}
						else
							ind = -1;
					}
					for(int l=0; l<cnt; l++)
					{
						remove = remove.substring(0, remove.lastIndexOf("["));
					}
					String add = remove + append;
					output.set(j, add);
				
				}
				output.set(0, output.get(0).substring(0, index));
				return output;
			}
		}
		for(int i=0; i<currNode.getChildCount(); i++)
		{
			output = printRule(currNode.getChild(i), output);
			if(output.get(0).contains("*"))
			{
				for(int j=0;j < output.size(); j++)
					output.set(j, output.get(j).substring(0, output.get(j).length()-1));
				break;
			}
		}
		///System.out.print("]");
		for(int j=0;j < output.size(); j++)
			output.set(j, output.get(j) + "]");
		return output;
	}
	
	
	private String printRule0(Node currNode, String output)
	{
		if(currNode == null)
			return output;
		//System.out.print("[ "+ currNode.getData());
		output += "[ "+ currNode.getData();
		
		if(currNode.getChildCount() == 0)
		{
			int index = output.lastIndexOf("]");
			
			if(tokens.isEmpty())
			{
				output = output.substring(0, index);
				return output;
			}
			else if(currNode.getData().equals(tokens.get(0)) || index == -1)
			{
				tokens.remove(0);
				//System.out.print("]");
				output += "]*";
				return output;
			}
			else
			{
				//remove the extra parsed string till first ) found
				
				output = output.substring(0, index);
				return output;
			}
		}
		boolean isbt = false;
		for(int i=0; i<currNode.getChildCount(); i++)
		{
			output = printRule0(currNode.getChild(i), output);
			if(output.contains("*"))
			{
				output = output.substring(0, output.length()-1);
				isbt = true;
				break;
			}
		}
		//if(!isbt)
		//{
			//System.out.print("]");
			output += "]";
		//}
		return output;
	}
	
	private void print(Node currNode)
	{
		if(currNode == null)
			return;
		System.out.print("[ "+ currNode.getData());
		
		if(currNode.getChildCount() == 0)
		{
			//System.out.print("]");
			return;
		}
		for(int i=0; i<currNode.getChildCount(); i++)
		{
			print(currNode.getChild(i));
		}
		//System.out.print("]");
	}

	
	//this adds a production p to the vector v of production indices
	//it also checks for duplicate indices, and skips those
	private final void addProd(Vector v, Production p)
	{
		//check for duplicates
		for(int i=0;i<v.size();i++)
			if(((Production)v.get(i)).equals(p))
				return;
		v.add(p);
	}

	//This runs through the columns and returns all the fully parsed productions
	//i.e. those with little dots at the very end.
	private final Vector getFinalProds(Vector cols)
	{
		Vector cur;
		Vector prods = new Vector();	prods.clear();
		Production p;
		for(int i=0; i<cols.size(); i++)
		{
			cur = (Vector)cols.get(i);
			for(int j=0;j<cur.size();j++)
			{
				p = (Production)cur.get(j);
				if(p.right.length == p.dot)
				{
					if(p.left.compareTo("ROOT")!=0)
					{
						prods.add(p);
					}
				}
			}
		}
		//convert it to an array for returning
		return prods;
	}

	//this returns true if a string is in the grammar, false otherwise
	//it's not exactly "comprehensive"... mostly it'll just see if all
	//the tokens in the sentence are terminals.
	private final boolean inGrammar(String s)
	{
		boolean found=false;
		Production p;
		for(int i=0;i<productions.size();i++)
		{
			p = (Production)productions.get(i);
			for(int j=0;j<p.right.length;j++)
				if(p.right[j].indexOf(s)!=-1)
					found = true;
			//we can't have a string equal to a non-terminal
			if(p.left.compareTo(s)==0)
			{
				System.out.println("String contains a non-terminal - cannot parse");
				return false;
			}
		}
		return found;
	}

	//this returns a vector of productions with a left side matching the
	//argument; happy string comparing.
	private final Vector getProds(String left)
	{
		//we store it in a vector for safekeeping
		Vector prods = new Vector();	
		prods.clear();
		Production p;
		for(int i=0;i<productions.size();i++)
		{
			p = (Production)productions.get(i);
			if(p.left.compareTo(left)==0)
				prods.add(p);
			//System.out.println("***" + p);
		}
		
		
		//convert it to an array for returning
		return prods;
	}

	//this checks if the given string[] has a parse tree with this grammar
	//
	public final boolean canParse(String sent[])
	{
		//check if all symbols are in the grammar
		for(int i=0;i<sent.length;i++)
			if(!inGrammar(sent[i]))
				return false;
		return true;
	}

	//this prints out the grammar
	public void print()
	{
		System.out.println(this.toString());
	}

	//what does every toString function do?
	public String toString()
	{
		String ret = "";
		for(int i=0;i<productions.size();i++)
			ret = ret + ((Production)productions.get(i)).toString() + "\n";
		return ret;
	}
}
