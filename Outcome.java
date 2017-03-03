package pDilemma;

/* Default payoff matrix (row, column) in years in prison

 \_S_|_B_
S|1,1|3,0  S = stay silent
B|0,3|2,2  B = betray other

ex. if SB below, result for this player choosing silent if other player chooses betray */

public enum Outcome {
	SS(1, "ss", "**** You both stayed silent, you both get 1 year in jail"), 
	SB(3, "sb", "**** You have been betrayed, you get 3 years in jail"), 
	BS(0, "bs", "**** You betrayed, you are free to go"), 
	BB(2, "bb", "**** You both betrayed, you both get 2 years in jail"); 

	final int years;
	final String shortcut;
	final String memo;
	Outcome(int years, String shortcut, String memo) {
		this.years = years;
		this.shortcut = shortcut;
		this.memo = memo;
	}
	public String shortcut() {return shortcut;}
	public String memo() {return memo;}
}