package pDilemma;

public enum Choice {
	SILENT("s"), BETRAY("b");
	
	private final String letter;
	private Choice(String letter) {
		this.letter = letter;
	}
	public String letter() {return letter;}
}